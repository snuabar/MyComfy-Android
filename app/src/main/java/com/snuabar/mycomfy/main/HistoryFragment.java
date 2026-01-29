package com.snuabar.mycomfy.main;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.snuabar.mycomfy.databinding.FragmentHistoryBinding;
import com.snuabar.mycomfy.main.data.AbstractMessageModel;
import com.snuabar.mycomfy.preview.FullScreenImageActivity;
import com.snuabar.mycomfy.main.data.DataIO;
import com.snuabar.mycomfy.view.dialog.OptionalDialog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * A fragment representing a list of Items.
 */
public class HistoryFragment extends Fragment {

    private MainViewModel mViewModel;
    private FragmentHistoryBinding binding;
    private final List<AbstractMessageModel> messageModels = new ArrayList<>();
    private final Executor executor = Executors.newSingleThreadExecutor();

    /**
     * Mandatory empty constructor for the fragment manager to instantiate the
     * fragment (e.g. upon screen orientation changes).
     */
    public HistoryFragment() {
    }

    @SuppressWarnings("unused")
    public static HistoryFragment newInstance() {
        return new HistoryFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        binding.list.setAdapter(new HistoryAdapter(requireContext(), messageModels, this::onItemClick));
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mViewModel.getDeletionHasPressLiveData().observe(this, aBoolean -> {
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                if (aBoolean) {
                    doDelete();
                }
            }
        });
        mViewModel.getDeletionModeLiveData().observe(this, aBoolean -> {
            if (getLifecycle().getCurrentState().isAtLeast(Lifecycle.State.RESUMED)) {
                if (binding.list.getAdapter() instanceof HistoryAdapter) {
                    ((HistoryAdapter) binding.list.getAdapter()).setEditMode(aBoolean);
                }
            }
        });
        mViewModel.getDeletedModelsLiveData().observe(this, stringListMap -> {
            stringListMap.remove(HistoryFragment.class.getName());
            if (!stringListMap.isEmpty()) {

            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (binding.list.getAdapter() instanceof HistoryAdapter) {
            ((HistoryAdapter) binding.list.getAdapter()).setEditMode(false);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        loadImageContents();
    }

    @Override
    public void onConfigurationChanged(@NonNull Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        binding.list.configureLayout();
    }

    private void onItemClick(int position, boolean longClick) {
        if (position == RecyclerView.NO_POSITION) {
            return;
        }

        if (longClick) {
            mViewModel.changeDeletionMode(true);
        } else {
            if (Boolean.TRUE.equals(mViewModel.getDeletionModeLiveData().getValue())) {
                if (binding.list.getAdapter() instanceof HistoryAdapter) {
                    ((HistoryAdapter) binding.list.getAdapter()).toggleSelection(position);
                }
            } else {
                AbstractMessageModel model = messageModels.get(position);
                if (model.getImageFile() != null) {
                    Intent intent = new Intent(requireActivity(), FullScreenImageActivity.class);
                    intent.putExtra(FullScreenImageActivity.EXTRA_IMAGE_PATH, model.getImageFile().getAbsolutePath());
                    startActivity(intent);
                }
            }
        }
    }

    private void loadImageContents() {
        executor.execute(() -> {
            if (getContext() == null) {
                return;
            }
            List<AbstractMessageModel> models = DataIO.copyMessageModels(getContext());
            messageModels.clear();
            models.removeIf(m -> m.getImageFile() == null || !m.getImageFile().exists());
            models.sort((o1, o2) -> Math.toIntExact(o2.getUTCTimestamp() - o1.getUTCTimestamp()));
            messageModels.addAll(models);

            requireActivity().runOnUiThread(() -> {
                if (binding.list.getAdapter() != null) {
                    binding.list.getAdapter().notifyDataSetChanged();
                }
            });
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    private void doDelete() {
        if (binding.list.getAdapter() instanceof HistoryAdapter) {
            List<Integer> indices = ((HistoryAdapter) binding.list.getAdapter()).getSelectedIndices();
            if (indices.isEmpty()) {
                return;
            }

            new OptionalDialog().setTitle("提示")
                    .setMessage(String.format(Locale.getDefault(), "删除 %d 项！", indices.size()))
                    .setPositive(() -> {
                        List<AbstractMessageModel> deletedModels = ((HistoryAdapter) binding.list.getAdapter()).deleteSelection();
                        mViewModel.changeDeletionMode(false);
                        Map<String, List<AbstractMessageModel>> deletedModelsMap = new HashMap<>();
                        deletedModelsMap.put(HistoryFragment.class.getName(), deletedModels);
                        mViewModel.changeDeletedModels(deletedModelsMap);
                    })
                    .setNegative(null)
                    .show(getChildFragmentManager());
        }
    }
}