package com.snuabar.mycomfy.main;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.documentfile.provider.DocumentFile;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.client.ImageRequest;
import com.snuabar.mycomfy.client.ImageResponse;
import com.snuabar.mycomfy.client.RetrofitClient;
import com.snuabar.mycomfy.client.StatusResponse;
import com.snuabar.mycomfy.client.WorkflowsResponse;
import com.snuabar.mycomfy.common.Callbacks;
import com.snuabar.mycomfy.databinding.FragmentHomeBinding;
import com.snuabar.mycomfy.utils.FileOperator;
import com.snuabar.mycomfy.utils.FilePicker;
import com.snuabar.mycomfy.utils.Output;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HomeFragment extends Fragment {

    private static final String TAG = HomeFragment.class.getName();

    private FragmentHomeBinding binding;

    private RetrofitClient retrofitClient;
    private String currentRequestId;
    private ScheduledExecutorService statusChecker;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private MainViewModel mViewModel;
    private FilePicker filePicker;
    private File latestImageFile;
    private List<String> workflows;
    private ArrayAdapter<String> workflowAdapter;

    private Handler mainHandler;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        filePicker = mViewModel.getFilePicker();
        // 初始化Retrofit客户端
        retrofitClient = RetrofitClient.getInstance(requireContext());
        mainHandler = new Handler(Looper.getMainLooper());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        initViews();
        // 设置按钮点击事件
        setupClickListeners();
        // 设置工作流控件
        setupWorkflowAdapter();
        return binding.getRoot();
    }

    private void setupWorkflowAdapter() {
        workflowAdapter = new ArrayAdapter<>(requireContext(), R.layout.layout_workflow_item, R.id.text1);
        binding.spinner.setAdapter(workflowAdapter);
        binding.spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selectedWorkflow = binding.spinner.getItemAtPosition(position).toString();
                requireActivity().getPreferences(Context.MODE_PRIVATE).edit().putString("workflow", selectedWorkflow).apply();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    private void initViews() {
        binding.etPrompt.setText(requireActivity().getPreferences(Context.MODE_PRIVATE).getString("prompt", getString(R.string.default_prompt)));
        binding.etWidth.setText(requireActivity().getPreferences(Context.MODE_PRIVATE).getString("width", "512"));
        binding.etHeight.setText(requireActivity().getPreferences(Context.MODE_PRIVATE).getString("height", "512"));
        binding.etSeed.setText(requireActivity().getPreferences(Context.MODE_PRIVATE).getString("seed", "0"));
    }

    private void setupClickListeners() {
        // 更新工作流按扭
        binding.btnLoadWorkflows.setOnClickListener(v -> loadWorkflows());

        // 生成图像按钮
        binding.btnGenerate.setOnClickListener(v -> generateImage());

        // 保存图像按钮
        binding.btnDownload.setOnClickListener(v -> saveImageWithPicker());

        // 分享图像按钮
        binding.btnShare.setOnClickListener(v -> shareImage());
    }

    private void shareImage() {
        if (latestImageFile != null && latestImageFile.exists()) {
            FileOperator.shareImageFromLocal(requireContext(), latestImageFile);
        }
    }

    private void loadWorkflows() {
        log("发送请求: 加载工作流。");
        // 发送请求
        retrofitClient.getApiService().loadWorkflow().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<WorkflowsResponse> call, @NonNull Response<WorkflowsResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    WorkflowsResponse workflowsResponse = response.body();
                    workflows = workflowsResponse.getWorkflows();
                    mainHandler.post(() -> {
                        workflowAdapter.clear();
                        workflowAdapter.addAll(workflows);
                        String selectedWorkflow = requireActivity().getPreferences(Context.MODE_PRIVATE).getString("workflow", null);
                        int index = workflows.indexOf(selectedWorkflow);
                        if (index < 0 || index >= workflows.size()) {
                            index = 0;
                            requireActivity().getPreferences(Context.MODE_PRIVATE).edit().putString("workflow", workflows.get(index)).apply();
                        }
                        binding.spinner.setSelection(index);
                    });
                } else {
                    mainHandler.post(() -> {
                        binding.tvStatus.setText("请求失败: " + response.code());
                        log("请求失败，状态码: " + response.code());
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<WorkflowsResponse> call, @NonNull Throwable t) {
                mainHandler.post(() -> {
                    binding.tvStatus.setText("请求失败: " + t.getMessage());
                    log("请求失败: " + t.getMessage());
                });
            }
        });
    }

    private void generateImage() {
        String workflow = binding.spinner.getSelectedItem().toString();
        String prompt = binding.etPrompt.getText().toString().trim();
        String widthStr = binding.etWidth.getText().toString().trim();
        String heightStr = binding.etHeight.getText().toString().trim();
        String seedStr = binding.etSeed.getText().toString().trim();

        // 验证输入
        if (workflow.isEmpty()) {
            showToast("请选择工作流");
            return;
        }

        // 验证输入
        if (prompt.isEmpty()) {
            showToast("请输入图像描述");
            return;
        }

        int width, height;
        try {
            width = Integer.parseInt(widthStr);
            height = Integer.parseInt(heightStr);

            if (width < 64 || width > 4096 || height < 64 || height > 4096) {
                showToast("图像尺寸应在64-4096之间");
                return;
            }
        } catch (NumberFormatException e) {
            showToast("请输入有效的尺寸");
            return;
        }

        Integer seed = null;
        if (!seedStr.isEmpty()) {
            try {
                seed = Integer.parseInt(seedStr);
            } catch (NumberFormatException e) {
                showToast("种子必须是数字");
                return;
            }
        }

        SharedPreferences.Editor editor = requireActivity().getPreferences(Context.MODE_PRIVATE).edit();
        Set<String> prompts = requireActivity().getPreferences(Context.MODE_PRIVATE).getStringSet("prompts", new HashSet<>());
        if (!requireActivity().getPreferences(Context.MODE_PRIVATE).getString("prompt", getString(R.string.default_prompt)).equals(prompt)) {
            HashSet<String> newSet = new HashSet<>(prompts);
            newSet.add(getPromptForSave(prompt));
            editor.putStringSet("prompts", newSet);
            editor.putString("prompt", prompt);
        }
        editor.putString("workflow", workflow);
        editor.putString("width", width + "");
        editor.putString("height", height + "");
        editor.putString("seed", seed + "");
        editor.apply();

        // 创建请求对象
        ImageRequest request = new ImageRequest(workflow, prompt, seed, width, height);

        // 显示进度条
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.tvStatus.setText("正在生成图像...");
        binding.btnGenerate.setEnabled(false);

        log("发送生成请求：\n" + "工作流: " + workflow + "\n" + "提示词: " + prompt + "\n" + "尺寸: " + width + "x" + height + "\n" + "种子: " + (seed != null ? seed : "随机"));

        currentRequestId = null;
        binding.btnDownload.setEnabled(false);
        binding.btnShare.setEnabled(false);

        // 发送请求
        retrofitClient.getApiService().generateImage(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ImageResponse> call, @NonNull Response<ImageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ImageResponse imageResponse = response.body();
                    currentRequestId = imageResponse.getRequest_id();

                    mainHandler.post(() -> {
                        binding.progressBar.setVisibility(View.INVISIBLE);
                        binding.btnGenerate.setEnabled(true);

                        if ("success".equals(imageResponse.getStatus())) {
                            binding.tvStatus.setText("图像生成成功");
                            binding.tvRequestId.setText("请求ID: " + currentRequestId);

                            log("生成成功！\n" + "请求ID: " + currentRequestId + "\n" + "处理时间: " + imageResponse.getProcessing_time() + "秒");

                            // 自动下载并显示图像
                            downloadAndDisplayImage(request);
                        } else {
                            binding.tvStatus.setText("生成失败: " + imageResponse.getMessage());
                            log("生成失败: " + imageResponse.getMessage());
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        binding.progressBar.setVisibility(View.INVISIBLE);
                        binding.btnGenerate.setEnabled(true);
                        binding.tvStatus.setText("请求失败: " + response.code());
                        log("请求失败，状态码: " + response.code());
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<ImageResponse> call, @NonNull Throwable t) {
                mainHandler.post(() -> {
                    binding.progressBar.setVisibility(View.INVISIBLE);
                    binding.btnGenerate.setEnabled(true);
                    binding.tvStatus.setText("请求失败: " + t.getMessage());
                    log("请求失败: " + t.getMessage());
                });
            }
        });
    }


    private void saveImageWithPicker() {
        if (currentRequestId == null || currentRequestId.isEmpty()) {
            showToast("没有可保存的图像");
            return;
        }

        // 使用文档创建API
        filePicker.pickDirectory(this::onDirectoryPickerCallback);
    }

    private void onDirectoryPickerCallback(Uri[] uris) {
        if (uris == null || uris.length == 0) {
            return;
        }

        Uri uri = uris[0];
        DocumentFile destDir = DocumentFile.fromTreeUri(requireContext(), uri);
        if (destDir != null && destDir.exists()) {
            if (!destDir.isDirectory() || !destDir.exists()) {
                Toast.makeText(getContext(), "保存失败！无效目录！", Toast.LENGTH_SHORT).show();
                return;
            }
            DocumentFile zipDocumentFile = DocumentFile.fromFile(latestImageFile);
            if (!FileOperator.moveDocumentFile(requireContext(), zipDocumentFile, destDir)) {
                Toast.makeText(getContext(), "保存失败！", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(), "保存成功！", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getContext(), "保存失败！", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveImageWithUri(Uri uri) {
        if (currentRequestId == null || currentRequestId.isEmpty()) {
            showToast("没有可保存的图像");
            return;
        }

        binding.tvStatus.setText("正在保存图像...");

        executor.execute(() -> {
            try {
                // 下载图像
                retrofitClient.getApiService().downloadImage(currentRequestId).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            try (ResponseBody body = response.body()) {
                                try {
                                    // 将图像保存到Uri指定的位置
                                    saveImageToUri(body, uri);
                                } catch (IOException e) {
                                    mainHandler.post(() -> {
                                        showToast("保存失败: " + e.getMessage());
                                        binding.tvStatus.setText("保存失败");
                                        log("保存失败: " + e.getMessage());
                                    });
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        mainHandler.post(() -> {
                            showToast("下载失败: " + t.getMessage());
                            binding.tvStatus.setText("下载失败");
                        });
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showToast("保存异常: " + e.getMessage());
                    binding.tvStatus.setText("保存异常");
                });
            }
        });
    }

    private void saveImageToUri(ResponseBody body, Uri uri) throws IOException {
        try (InputStream inputStream = body.byteStream(); OutputStream outputStream = requireContext().getContentResolver().openOutputStream(uri)) {

            if (outputStream == null) {
                throw new IOException("无法打开输出流");
            }

            byte[] buffer = new byte[4096];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }

            outputStream.flush();

            mainHandler.post(() -> {
                showToast("图像保存成功");
                binding.tvStatus.setText("图像保存完成");
                log("图像已保存到: " + uri.toString());
            });
        }
    }

    private void downloadAndDisplayImage(ImageRequest request) {
        if (currentRequestId == null || currentRequestId.isEmpty()) {
            showToast("没有可下载的图像");
            return;
        }

        binding.tvStatus.setText("正在下载图像...");

        executor.execute(() -> {
            try {
                retrofitClient.getApiService().downloadImage(currentRequestId).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            try (ResponseBody body = response.body()) {
                                // 保存图像到文件
                                File imageFile = saveImageToFile(body, request, (total, progress) -> {
                                    mainHandler.post(() -> {
                                        binding.progressBar.setVisibility(View.VISIBLE);
                                        binding.progressBar.setMax(Math.toIntExact(total));
                                        binding.progressBar.setProgress(Math.toIntExact(progress));
                                    });
                                });
                                if (imageFile != null) {
                                    mainHandler.post(() -> {
                                        binding.tvStatus.setText("图像下载完成");
                                        displayImage(imageFile);
                                        binding.btnDownload.setEnabled(true);
                                        binding.btnShare.setEnabled(true);
                                        log("图像已保存到: " + imageFile.getAbsolutePath());
                                    });
                                }
                            }
                        } else {
                            mainHandler.post(() -> {
                                binding.tvStatus.setText("下载失败: " + response.code());
                                log("下载失败，状态码: " + response.code());
                            });
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        mainHandler.post(() -> {
                            binding.tvStatus.setText("下载失败: " + t.getMessage());
                            log("下载失败: " + t.getMessage());
                        });
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    binding.tvStatus.setText("下载异常: " + e.getMessage());
                    log("下载异常: " + e.getMessage());
                });
            }
        });
    }

    private void downloadImage() {
        if (currentRequestId == null || currentRequestId.isEmpty()) {
            showToast("没有可下载的图像");
            return;
        }

        binding.tvStatus.setText("正在下载图像...");

        executor.execute(() -> {
            try {
                retrofitClient.getApiService().downloadImage(currentRequestId).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            try (ResponseBody body = response.body()) {
                                File imageFile = saveImageToFile(body, null, (total, progress) -> {
                                    mainHandler.post(() -> {
                                        binding.progressBar.setVisibility(View.VISIBLE);
                                        binding.progressBar.setMax(Math.toIntExact(total));
                                        binding.progressBar.setProgress(Math.toIntExact(progress));
                                    });
                                });
                                if (imageFile != null) {
                                    mainHandler.post(() -> {
                                        showToast("图像已保存到: " + imageFile.getAbsolutePath());
                                        binding.tvStatus.setText("图像保存完成");
                                        log("图像已保存: " + imageFile.getName());
                                    });
                                }
                            }
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        mainHandler.post(() -> {
                            showToast("下载失败: " + t.getMessage());
                            binding.tvStatus.setText("下载失败");
                        });
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showToast("下载异常: " + e.getMessage());
                    binding.tvStatus.setText("下载异常");
                });
            }
        });
    }

    private File saveImageToFile(ResponseBody body, ImageRequest request, Callbacks.Callback2T<Long, Long> callback) {
        try {
            File[] outputFiles = Output.newOutputFile(requireContext());
            File imageFile = outputFiles[0];

            // 保存文件
            if (RetrofitClient.downloadFile(body, imageFile, callback)) {
                latestImageFile = imageFile;
                if (request != null) {
                    JSONObject jsonObject = request.toJson();
                    if (jsonObject != null) {
                        jsonObject.putOpt("timestamp", System.currentTimeMillis());
                        File requestJsonFile = outputFiles[1];
                        try (FileWriter writer = new FileWriter(requestJsonFile, StandardCharsets.UTF_8, false)) {
                            writer.append(jsonObject.toString());
                        }
                    }
                }

                SharedPreferences preferences = requireActivity().getPreferences(Context.MODE_PRIVATE);
                Set<String> imageSet = preferences.getStringSet("images", new HashSet<>());

                SharedPreferences.Editor editor = preferences.edit();
                editor.apply();

                return imageFile;
            }
        } catch (Exception e) {
            log("保存文件失败: " + e.getMessage());
        }
        return null;
    }

    private void displayImage(File imageFile) {
        try {
            Bitmap bitmap = BitmapFactory.decodeFile(imageFile.getAbsolutePath());
            if (bitmap != null) {
                binding.ivGeneratedImage.setImageBitmap(bitmap);
                log("图像显示成功，尺寸: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            } else {
                log("无法解码图像文件");
            }
        } catch (Exception e) {
            log("显示图像失败: " + e.getMessage());
        }
    }

    private void startStatusChecker() {
        if (statusChecker != null && !statusChecker.isShutdown()) {
            statusChecker.shutdown();
        }

        statusChecker = Executors.newSingleThreadScheduledExecutor();
        statusChecker.scheduleAtFixedRate(() -> {
            if (currentRequestId != null && !currentRequestId.isEmpty()) {
                checkImageStatus();
            }
        }, 0, 2, TimeUnit.SECONDS);
    }

    private void checkImageStatus() {
        if (currentRequestId == null || currentRequestId.isEmpty()) {
            return;
        }

        retrofitClient.getApiService().checkStatus(currentRequestId).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<StatusResponse> call, @NonNull Response<StatusResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    StatusResponse statusResponse = response.body();
                    mainHandler.post(() -> {
                        binding.tvStatus.setText("状态: " + statusResponse.getStatus() + " - " + statusResponse.getMessage());
                        log("状态更新: " + statusResponse.getStatus());

                        if ("success".equals(statusResponse.getStatus())) {
                            stopStatusChecker();
                            downloadAndDisplayImage(null);
                        }
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<StatusResponse> call, @NonNull Throwable t) {
                log("状态检查失败: " + t.getMessage());
            }
        });
    }

    private void stopStatusChecker() {
        if (statusChecker != null && !statusChecker.isShutdown()) {
            statusChecker.shutdown();
            statusChecker = null;
        }
    }

    private void log(String message) {
        String currentLog = binding.tvLog.getText().toString();
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String newLog = "[" + timestamp + "] " + message + "\n" + currentLog;
        binding.tvLog.setText(newLog);
    }

    private void showToast(String message) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopStatusChecker();
        if (statusChecker != null) {
            statusChecker.shutdown();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        updateBaseUrl();
        if (binding.spinner.getCount() == 0) {
            // 更新工作流
            loadWorkflows();
        }
    }

    private void updateBaseUrl() {
        String ip = requireActivity().getPreferences(Context.MODE_PRIVATE).getString("server_ip", "192.168.1.17");
        String port = requireActivity().getPreferences(Context.MODE_PRIVATE).getString("server_port", "8000");
        // 更新Base URL
        String baseUrl = "http://" + ip + ":" + port;
        retrofitClient.setBaseUrl(baseUrl);
    }

    private String getFileInfoForSave(File imageFile) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.putOpt("timestamp", System.currentTimeMillis());
            jsonObject.putOpt("image", imageFile.getAbsolutePath());
            return jsonObject.toString();
        } catch (JSONException e) {
            Log.e(TAG, "getFileInfoForSave. error thrown.", e);
        }

        return null;
    }

    private String getPromptForSave(String prompt) {
        JSONObject jsonObject = new JSONObject();
        try {
            jsonObject.putOpt("timestamp", System.currentTimeMillis());
            jsonObject.putOpt("prompt", prompt);
            return jsonObject.toString();
        } catch (JSONException e) {
            Log.e(TAG, "getPromptForSave. error thrown.", e);
        }

        return null;
    }

    private ArrayList<Object[]> getPromptsFromSaved() {
        Set<String> prompts = requireActivity().getPreferences(Context.MODE_PRIVATE).getStringSet("prompts", new HashSet<>());
        ArrayList<Object[]> promptArray = new ArrayList<>();
        for (String jsonString : prompts) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                Object[] promptInfo = new Object[]{jsonObject.optLong("timestamp", 0), jsonObject.optString("prompt", "")};
                promptArray.add(promptInfo);
            } catch (JSONException e) {
                Log.e(TAG, "getPromptFromSaved. error thrown.", e);
            }
        }

        promptArray.sort((o1, o2) -> Math.toIntExact((long) o2[0] - (long) o1[0]));

        return promptArray;
    }
}