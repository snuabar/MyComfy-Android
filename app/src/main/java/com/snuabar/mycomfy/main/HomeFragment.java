package com.snuabar.mycomfy.main;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupWindow;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.client.QueueRequest;
import com.snuabar.mycomfy.client.Parameters;
import com.snuabar.mycomfy.client.RetrofitClient;
import com.snuabar.mycomfy.common.Common;
import com.snuabar.mycomfy.databinding.FragmentHomeBinding;
import com.snuabar.mycomfy.databinding.LayoutMessageItemOptionalDialogBinding;
import com.snuabar.mycomfy.main.data.AbstractMessageModel;
import com.snuabar.mycomfy.main.data.MainViewModel;
import com.snuabar.mycomfy.main.data.MessageModelState;
import com.snuabar.mycomfy.main.model.MessageModel;
import com.snuabar.mycomfy.main.model.ReceivedMessageModel;
import com.snuabar.mycomfy.main.model.SentMessageModel;
import com.snuabar.mycomfy.main.model.SentVideoMessageModel;
import com.snuabar.mycomfy.main.model.UpscaleSentMessageModel;
import com.snuabar.mycomfy.preview.FullScreenImageActivity;
import com.snuabar.mycomfy.setting.Settings;
import com.snuabar.mycomfy.utils.FileOperator;
import com.snuabar.mycomfy.utils.FilePicker;
import com.snuabar.mycomfy.main.data.DataIO;
import com.snuabar.mycomfy.utils.ImageUtils;
import com.snuabar.mycomfy.utils.ViewUtils;
import com.snuabar.mycomfy.view.ParametersPopup;
import com.snuabar.mycomfy.view.dialog.OptionalDialog;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class HomeFragment extends Fragment {

    private static final String TAG = HomeFragment.class.getName();

    private FragmentHomeBinding binding;

    private MainViewModel mViewModel;
    private FilePicker filePicker;
    private MessageAdapter messageAdapter = null;
    private ParametersPopup parametersPopup;
//    private PromptEditPopup promptEditPopup;
    private PopupWindow messageItemOptionalPopup;
    private final OptionalDialog.ProgressDialog pgsDlg;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    public HomeFragment() {
        pgsDlg = new OptionalDialog.ProgressDialog();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        filePicker = mViewModel.getFilePicker();
        parametersPopup = new ParametersPopup(requireContext(), parameterPopupDataRequirer);
//        promptEditPopup = new PromptEditPopup(requireContext(), onPromptChangeListener);

        pgsDlg.show(getChildFragmentManager());
        mViewModel.reloadMessageModels();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        initViews();
        // 设置事件
        setupListeners();
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        messageAdapter = new MessageAdapter(this::onMessageElementClick);
        binding.recyclerView.setAdapter(messageAdapter);

        mViewModel.getDeletionHasPressLiveData().observe(getViewLifecycleOwner(), aBoolean -> {
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                if (aBoolean) {
                    doDelete();
                }
            }
        });
        mViewModel.getDeletionModeLiveData().observe(getViewLifecycleOwner(), aBoolean -> {
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                messageAdapter.setEditMode(aBoolean);
            }
        });
        mViewModel.getDeletedModelsLiveData().observe(getViewLifecycleOwner(), stringListMap -> {
            stringListMap.remove(HomeFragment.class.getName());
            if (!stringListMap.isEmpty()) {
                for (String key : stringListMap.keySet()) {
                    for (AbstractMessageModel model : Objects.requireNonNull(stringListMap.get(key))) {
                        int index = mViewModel.getIndexWithId(model.getId());
                        messageAdapter.notifyItemDeleted(index);
                    }
                }
            }
        });
        mViewModel.observeMessageModelsLiveData(getViewLifecycleOwner(), abstractMessageModels -> {
            messageAdapter.setData(abstractMessageModels);
            pgsDlg.dismiss();
        });
        mViewModel.getMessageModelStateLiveData().observe(getViewLifecycleOwner(), state -> {
            if (state == null || state.state == MessageModelState.STATE_NONE) {
                return;
            }

            if (state.state == MessageModelState.STATE_ADDED) {
                messageAdapter.notifyItemAdded(state.index);
            } else if (state.state == MessageModelState.STATE_DELETED) {
                messageAdapter.notifyItemDeleted(state.index);
            } else if (state.state == MessageModelState.STATE_CHANGED) {
                messageAdapter.notifyItemChanged(state.index);
            }
        });
        mViewModel.getWorkflowsLiveData().observe(getViewLifecycleOwner(), parametersPopup::setWorkflows);
        mViewModel.getModelsLiveData().observe(getViewLifecycleOwner(), parametersPopup::setModels);
    }

    @Override
    public void onPause() {
        super.onPause();
        mViewModel.changeDeletionMode(false);
//        // 保存上一次的提示词
//        Settings.getInstance().edit().putString("prompt", binding.tvPrompt.getText().toString()).apply();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBaseUrl();
        mViewModel.startStatusCheck();
        if (Settings.getInstance().hasDataImportedState()) {
            Settings.getInstance().clearDataImportedState();
            mViewModel.reloadMessageModels();
        }
    }

    private final ParametersPopup.DataRequirer parameterPopupDataRequirer = new ParametersPopup.DataRequirer() {
        @Override
        public void loadWorkflows() {
            mViewModel.loadWorkflows();
        }

        @Override
        public void loadModels(@NonNull List<String> modelTypes) {
            mViewModel.loadModels(modelTypes);
        }
    };
//    private final PromptEditPopup.OnPromptChangeListener onPromptChangeListener = prompt ->
//            binding.tvPrompt.setText(prompt);

    private void onMessageElementClick(View view, int position, int ope, float[] downLocation) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        AbstractMessageModel model = mViewModel.getMessageModels().get(position);
        if (model == null) {
            return;
        }

        File imageFile = model.getImageFile();
        if (ope == MessageAdapter.OnElementClickListener.OPE_NONE) {
            if (Boolean.TRUE.equals(mViewModel.getDeletionModeLiveData().getValue())) {
                messageAdapter.toggleSelection(position);
            } else {
                if (imageFile != null && imageFile.exists()) {
                    Intent intent = new Intent(requireActivity(), FullScreenImageActivity.class);
                    intent.putExtra(FullScreenImageActivity.EXTRA_ID_LIST, new ArrayList<>(Collections.singletonList(model.getId())));
                    startActivity(intent);
                }
            }
        } else if (ope == MessageAdapter.OnElementClickListener.OPE_LONG_CLICK) {
            showMessageItemOptionalPopup(view, downLocation, model);
        } else if (ope == MessageAdapter.OnElementClickListener.OPE_INTERRUPT) {
            model.setInterruptionFlag(true);
            int index = mViewModel.saveMessageModel(model);
            messageAdapter.notifyItemChanged(index);
        } else if (ope == MessageAdapter.OnElementClickListener.OPE_SAVE) {
            if (imageFile != null && imageFile.exists()) {
                // 使用文档创建API
                filePicker.pickDirectory(imageFile, this::onDirectoryPickerWithFileCallback);
            }
        } else if (ope == MessageAdapter.OnElementClickListener.OPE_SHARE) {
            if (imageFile != null && imageFile.exists()) {
                FileOperator.shareImageFromLocal(requireContext(), imageFile);
            }
        } else if (ope == MessageAdapter.OnElementClickListener.OPE_RESENT) {
            if (model instanceof SentMessageModel) {
                enqueue(model);
            }
        } else if (ope == MessageAdapter.OnElementClickListener.OPE_X2) {
            enqueue(model, 2.f);
        } else if (ope == MessageAdapter.OnElementClickListener.OPE_X4) {
            enqueue(model, 4.f);
        } else if (ope == MessageAdapter.OnElementClickListener.OPE_XN) {
            showUpscaleAdjustmentDialog(model);
        }
    }

    private void showUpscaleAdjustmentDialog(AbstractMessageModel model) {
        final double[] factorGetter = new double[]{model.getParameters().getUpscale_factor()};
        new OptionalDialog()
                .setType(OptionalDialog.Type.Info)
                .setMessage("设定放大倍数：")
                .setPositive("提交", () -> enqueue(model, factorGetter[0]))
                .setCustomView(R.layout.layout_scale_factor)
                .setDialogCreatedCallback(dlg -> {
                    TextView tvSrc = dlg.findCustomViewById(R.id.tvSrc);
                    TextView tvFactor = dlg.findCustomViewById(R.id.tvFactor);
                    TextView tvDest = dlg.findCustomViewById(R.id.tvDest);
                    SeekBar seekBar = dlg.findCustomViewById(R.id.seekBar);
                    if (tvSrc != null && tvFactor != null && tvDest != null && seekBar != null) {
                        tvSrc.setText(String.format(Locale.getDefault(),
                                "%d x %d",
                                model.getParameters().getImg_width(), model.getParameters().getImg_height())
                        );
                        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                            @Override
                            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                                double factor = progress / 10.0;
                                factorGetter[0] = factor;
                                int destWidth = Common.calcScale(model.getParameters().getImg_width(), factor);
                                int destHeight = Common.calcScale(model.getParameters().getImg_height(), factor);
                                tvFactor.setText(String.format(Locale.getDefault(), "x%.01f", factor));
                                tvDest.setText(String.format(Locale.getDefault(), "%d x %d", destWidth, destHeight));
                            }
                            @Override
                            public void onStartTrackingTouch(SeekBar seekBar) {

                            }
                            @Override
                            public void onStopTrackingTouch(SeekBar seekBar) {

                            }
                        });
                        seekBar.setProgress(20);// default: x2
                    }
                })
                .show(getChildFragmentManager());

    }

    private void showMessageItemOptionalPopup(View anchor, float[] downLocation, AbstractMessageModel model) {
        LayoutMessageItemOptionalDialogBinding binding;
        if (messageItemOptionalPopup == null) {
            binding = LayoutMessageItemOptionalDialogBinding.inflate(LayoutInflater.from(requireContext()));
            PopupWindow popupWindow = new PopupWindow(requireContext());
            popupWindow.setContentView(binding.getRoot());
            popupWindow.setOutsideTouchable(true);
            popupWindow.setFocusable(true);
            popupWindow.setElevation(8);
            popupWindow.setBackgroundDrawable(AppCompatResources.getDrawable(requireContext(), R.drawable.message_item_optional_popup_bg));
            popupWindow.getContentView().setTag(binding);
            messageItemOptionalPopup = popupWindow;
        } else {
            binding = (LayoutMessageItemOptionalDialogBinding) messageItemOptionalPopup.getContentView().getTag();
        }

        binding.btnCopyPrompt.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) requireContext().getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("prompt", model.getParameters().getPrompt());
            clipboard.setPrimaryClip(clip);
            Toast.makeText(requireContext(), "已复制到剪贴板", Toast.LENGTH_SHORT).show();

            messageItemOptionalPopup.dismiss();
        });

        binding.btnCopyImage.setOnClickListener(v -> {
            ImageUtils.copyImageUsingContentUri(requireContext(), model.getImageFile());

            messageItemOptionalPopup.dismiss();
        });

        binding.btnDelete.setOnClickListener(v -> {
            new OptionalDialog()
                    .setType(OptionalDialog.Type.Alert)
                    .setTitle("提示")
                    .setMessage("删除？")
                    .setPositive(() -> {
                        int index = mViewModel.deleteModelFile(model);
                        messageAdapter.notifyItemDeleted(index);
                    })
                    .setNegative(null)
                    .show(getChildFragmentManager());

            messageItemOptionalPopup.dismiss();
        });

        if (model.getImageFile() != null && model.getImageFile().exists()) {
            binding.btnCopyImage.setVisibility(View.VISIBLE);
        } else {
            binding.btnCopyImage.setVisibility(View.GONE);
        }

        // 手动测量和布局
        ViewUtils.measure(messageItemOptionalPopup.getContentView(), 300);

        int x = (int) downLocation[0] - messageItemOptionalPopup.getContentView().getWidth() / 2;
        int y = -(anchor.getHeight() - (int) downLocation[1]) - messageItemOptionalPopup.getContentView().getHeight() - 100;

        // 显示 PopupWindow
        messageItemOptionalPopup.showAsDropDown(anchor, x, y, Gravity.NO_GRAVITY);
    }

    private void onDirectoryPickerWithFileCallback(Uri[] uris, File file) {
        if (uris == null || uris.length == 0 || file == null) {
            return;
        }

        Uri uri = uris[0];
        DocumentFile destDir = DocumentFile.fromTreeUri(requireContext(), uri);
        if (destDir != null && destDir.exists()) {
            if (!destDir.isDirectory() || !destDir.exists()) {
                Toast.makeText(getContext(), "保存失败！无效目录！", Toast.LENGTH_SHORT).show();
                return;
            }
            DocumentFile zipDocumentFile = DocumentFile.fromFile(file);
            if (!FileOperator.copyDocumentFile(requireContext(), zipDocumentFile, destDir, false)) {
                Toast.makeText(getContext(), "保存失败！", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "保存成功！", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "保存失败！", Toast.LENGTH_SHORT).show();
        }
    }

    private void initViews() {
//        binding.tvPrompt.setText(Settings.getInstance().getPrompt(""));
    }

    private void setupListeners() {
//        // 提示词
//        binding.tvPrompt.setOnClickListener(v -> parametersPopup.showAsDropDown(v));
//        // 生成图像按钮
//        binding.btnSubmit.setOnClickListener(v -> parametersPopup.showAsDropDown(v));
//        // 更改参数按扭
//        binding.btnParam.setOnClickListener(v -> parametersPopup.showAsDropDown(v));
        binding.fab.setOnClickListener(v -> parametersPopup.showAsDropDown(v));
        parametersPopup.setOnSubmitCallback(() -> enqueue(null));
//        parametersPopup.setOnDismissListener(() -> binding.tvPrompt.setText(Settings.getInstance().getPrompt("")));
        binding.fab.setOnPositionChangedListener((x, y) -> {

            return null;
        });
        binding.fab.setLongClickable(true);
        binding.fab.setOnLongClickListener(v -> {
            parametersPopup.randomSeed(true);
            return true;
        });
    }

    private void enqueue(AbstractMessageModel model, double... upscale) {
        final SentMessageModel sentMessageModel;
        final QueueRequest request;
        if (model == null) {
//            Parameters parameters = newParameters();
            Parameters parameters = parametersPopup.getParameters();
            if (parameters == null) {
                return;
            }

            // 创建请求对象
            request = new QueueRequest(parameters);

            if (parametersPopup.isVideoWorkflow()) {
                sentMessageModel = new SentVideoMessageModel(parameters);
            } else {
                sentMessageModel = new SentMessageModel(parameters);
            }
            // 保存数据
            int index = mViewModel.saveMessageModel(sentMessageModel);
            // 界面显示
            messageAdapter.notifyItemAdded(index);
        } else {
            if (upscale.length > 0 && model instanceof ReceivedMessageModel) {
                Parameters parameters = new Parameters(model.getParameters());
                parameters.setUpscale_factor(upscale[0]);
                sentMessageModel = new UpscaleSentMessageModel(parameters);
                sentMessageModel.setImageFile(DataIO.getInstance().copyImageFile(model.getImageFile()));
                request = new QueueRequest(sentMessageModel.getParameters());
                int index = mViewModel.saveMessageModel(sentMessageModel);
                messageAdapter.notifyItemAdded(index);
            } else {
                model.setStatus(MessageModel.STATUS_PENDING, 0, null);
                int index = mViewModel.saveMessageModel(model);
                messageAdapter.notifyItemChanged(index);
                request = new QueueRequest(model.getParameters().setResent());
                sentMessageModel = (SentMessageModel) model;
            }
        }

        // 发送请求
        mViewModel.enqueue(request, sentMessageModel);
    }

    private void updateBaseUrl() {
        String ip = Settings.getInstance().getString("server_ip", "192.168.1.17");
        String port = Settings.getInstance().getString("server_port", "8000");
        // 更新Base URL
        RetrofitClient.getInstance().setBaseUrl(ip, port);

        mViewModel.loadWorkflows();
    }

    private void doDelete() {
        List<Integer> indices = messageAdapter.getSelectedIndices();
        if (indices.isEmpty()) {
            return;
        }

        // 从大到小排序
        indices.sort((o1, o2) -> o2 - o1);

        new OptionalDialog()
                .setType(OptionalDialog.Type.Alert)
                .setTitle("提示")
                .setMessage(String.format(Locale.getDefault(), "删除 %d 项！", indices.size()))
                .setPositive(() -> {
                    for (int i : indices) {
                        AbstractMessageModel model = mViewModel.getMessageModels().get(i);
                        int index = mViewModel.deleteModelFile(model);
                        assert i == index;
                        messageAdapter.notifyItemDeleted(index);
                    }
                    mViewModel.changeDeletionMode(false);
                })
                .setNegative(null)
                .show(getChildFragmentManager());
    }
}