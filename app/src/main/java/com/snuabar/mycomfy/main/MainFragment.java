package com.snuabar.mycomfy.main;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.client.ImageRequest;
import com.snuabar.mycomfy.client.ImageResponse;
import com.snuabar.mycomfy.client.RetrofitClient;
import com.snuabar.mycomfy.client.StatusResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainFragment extends Fragment {

    private static final String TAG = MainFragment.class.getName();

    private EditText etPrompt, etWidth, etHeight, etSeed;
    private Button btnGenerate, btnDownload;
    private ProgressBar progressBar;
    private TextView tvStatus, tvRequestId, tvLog;
    private ImageView ivGeneratedImage;

    private RetrofitClient retrofitClient;
    private String currentRequestId;
    private ScheduledExecutorService statusChecker;
    private final Executor executor = Executors.newSingleThreadExecutor();

    private MainViewModel mViewModel;

    private Handler mainHandler;
    private boolean connectionOk = false;

    // 使用新的 Activity Result API 处理权限请求
    private ActivityResultLauncher<String[]> requestPermissionLauncher;
    private ActivityResultLauncher<String> createDocumentLauncher;

    public static MainFragment newInstance() {
        return new MainFragment();
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(requireActivity()).get(MainViewModel.class);
        // 初始化Retrofit客户端
        retrofitClient = RetrofitClient.getInstance(requireContext());
        mainHandler = new Handler(Looper.getMainLooper());
        // 初始化权限请求Launcher
        initPermissionLaunchers();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);
        initViews(view);
        // 请求存储权限
        requestStoragePermission();
        // 设置按钮点击事件
        setupClickListeners();
        return view;
    }

    private void initViews(View root) {
        etPrompt = root.findViewById(R.id.etPrompt);
        etWidth = root.findViewById(R.id.etWidth);
        etHeight = root.findViewById(R.id.etHeight);
        etSeed = root.findViewById(R.id.etSeed);
        btnGenerate = root.findViewById(R.id.btnGenerate);
        btnDownload = root.findViewById(R.id.btnDownload);
        progressBar = root.findViewById(R.id.progressBar);
        tvStatus = root.findViewById(R.id.tvStatus);
        tvRequestId = root.findViewById(R.id.tvRequestId);
        tvLog = root.findViewById(R.id.tvLog);
        ivGeneratedImage = root.findViewById(R.id.ivGeneratedImage);

        etPrompt.setText(requireActivity().getPreferences(Context.MODE_PRIVATE).getString("prompt", getString(R.string.default_prompt)));
        etWidth.setText(requireActivity().getPreferences(Context.MODE_PRIVATE).getString("width", "512"));
        etHeight.setText(requireActivity().getPreferences(Context.MODE_PRIVATE).getString("height", "512"));
        etSeed.setText(requireActivity().getPreferences(Context.MODE_PRIVATE).getString("seed", "0"));
    }

    private void setupClickListeners() {
        // 生成图像按钮
        btnGenerate.setOnClickListener(v -> generateImage());

        // 下载图像按钮 - 使用新的文档创建API
        btnDownload.setOnClickListener(v -> saveImageWithPicker());
    }

    private void initPermissionLaunchers() {
        // 权限请求Launcher
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), permissions -> {
            boolean allGranted = true;
            for (Map.Entry<String, Boolean> entry : permissions.entrySet()) {
                if (!entry.getValue()) {
                    allGranted = false;
                    log("权限被拒绝: " + entry.getKey());
                }
            }

            if (allGranted) {
                log("所有权限已授予");
            } else {
                log("部分权限被拒绝，可能无法保存图像");
                Toast.makeText(requireContext(), "存储权限被拒绝，可能无法保存图像", Toast.LENGTH_LONG).show();
            }
        });

        // 创建文档Launcher（用于保存文件）
        createDocumentLauncher = registerForActivityResult(new ActivityResultContracts.CreateDocument("image/png"), uri -> {
            if (uri != null) {
                saveImageWithUri(uri);
            }
        });
    }


    private void requestStoragePermission() {
        // Android 13 (API 33) 及以上使用新的权限
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要READ_MEDIA_IMAGES权限
            String[] permissions = {
                    Manifest.permission.READ_MEDIA_IMAGES,
                    Manifest.permission.POST_NOTIFICATIONS
            };

            // 检查哪些权限还没被授予
            List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(requireContext(), permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (!permissionsToRequest.isEmpty()) {
                // 只请求未授予的权限
                String[] permissionsArray = permissionsToRequest.toArray(new String[0]);
                requestPermissionLauncher.launch(permissionsArray);
            } else {
                log("所有权限已授予");
            }
        } else {
            // Android 12 及以下使用旧的存储权限
            String[] permissions = {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            };

            // 检查哪些权限还没被授予
            List<String> permissionsToRequest = new ArrayList<>();
            for (String permission : permissions) {
                if (ContextCompat.checkSelfPermission(requireContext(), permission)
                        != PackageManager.PERMISSION_GRANTED) {
                    permissionsToRequest.add(permission);
                }
            }

            if (!permissionsToRequest.isEmpty()) {
                // 只请求未授予的权限
                String[] permissionsArray = permissionsToRequest.toArray(new String[0]);
                requestPermissionLauncher.launch(permissionsArray);
            } else {
                log("所有权限已授予");
            }
        }
    }

    private void generateImage() {
        String prompt = etPrompt.getText().toString().trim();
        String widthStr = etWidth.getText().toString().trim();
        String heightStr = etHeight.getText().toString().trim();
        String seedStr = etSeed.getText().toString().trim();

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

        SharedPreferences.Editor editor =requireActivity().getPreferences(Context.MODE_PRIVATE).edit();
        Set<String> prompts = requireActivity().getPreferences(Context.MODE_PRIVATE).getStringSet("prompts", new HashSet<>());
        if (!requireActivity().getPreferences(Context.MODE_PRIVATE).getString("prompt", getString(R.string.default_prompt)).equals(prompt)) {
            HashSet<String> newSet = new HashSet<>(prompts);
            newSet.add(getPromptForSave(prompt));
            editor.putStringSet("prompts", newSet);
            editor.putString("prompt", prompt);
        }
        editor.putString("width", width + "");
        editor.putString("height", height + "");
        editor.putString("seed", seed + "");
        editor.apply();

        // 创建请求对象
        ImageRequest request = new ImageRequest(prompt, seed, width, height);

        // 显示进度条
        progressBar.setVisibility(View.VISIBLE);
        tvStatus.setText("正在生成图像...");
        btnGenerate.setEnabled(false);

        log("发送生成请求:\n" + "提示词: " + prompt + "\n" + "尺寸: " + width + "x" + height + "\n" + "种子: " + (seed != null ? seed : "随机"));

        currentRequestId = null;
        btnDownload.setEnabled(false);

        // 发送请求
        retrofitClient.getApiService().generateImage(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<ImageResponse> call, @NonNull Response<ImageResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    ImageResponse imageResponse = response.body();
                    currentRequestId = imageResponse.getRequest_id();

                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.INVISIBLE);
                        btnGenerate.setEnabled(true);

                        if ("success".equals(imageResponse.getStatus())) {
                            tvStatus.setText("图像生成成功");
                            tvRequestId.setText("请求ID: " + currentRequestId);
                            btnDownload.setEnabled(true);

                            log("生成成功！\n" + "请求ID: " + currentRequestId + "\n" + "处理时间: " + imageResponse.getProcessing_time() + "秒");

                            // 自动下载并显示图像
                            downloadAndDisplayImage();
                        } else {
                            tvStatus.setText("生成失败: " + imageResponse.getMessage());
                            log("生成失败: " + imageResponse.getMessage());
                        }
                    });
                } else {
                    mainHandler.post(() -> {
                        progressBar.setVisibility(View.INVISIBLE);
                        btnGenerate.setEnabled(true);
                        tvStatus.setText("请求失败: " + response.code());
                        log("请求失败，状态码: " + response.code());
                    });
                }
            }

            @Override
            public void onFailure(@NonNull Call<ImageResponse> call, @NonNull Throwable t) {
                mainHandler.post(() -> {
                    progressBar.setVisibility(View.INVISIBLE);
                    btnGenerate.setEnabled(true);
                    tvStatus.setText("请求失败: " + t.getMessage());
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

        // 生成文件名
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String fileName = "AI_Image_" + timeStamp + ".png";

        // 使用文档创建API
        createDocumentLauncher.launch(fileName);
    }

    private void saveImageWithUri(Uri uri) {
        if (currentRequestId == null || currentRequestId.isEmpty()) {
            showToast("没有可保存的图像");
            return;
        }

        tvStatus.setText("正在保存图像...");

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
                                        tvStatus.setText("保存失败");
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
                            tvStatus.setText("下载失败");
                        });
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showToast("保存异常: " + e.getMessage());
                    tvStatus.setText("保存异常");
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
                tvStatus.setText("图像保存完成");
                log("图像已保存到: " + uri.toString());
            });
        }
    }

    private void downloadAndDisplayImage() {
        if (currentRequestId == null || currentRequestId.isEmpty()) {
            showToast("没有可下载的图像");
            return;
        }

        tvStatus.setText("正在下载图像...");

        executor.execute(() -> {
            try {
                retrofitClient.getApiService().downloadImage(currentRequestId).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            try (ResponseBody body = response.body()) {
                                // 保存图像到文件
                                File imageFile = saveImageToFile(body);
                                if (imageFile != null) {
                                    mainHandler.post(() -> {
                                        tvStatus.setText("图像下载完成");
                                        displayImage(imageFile);
                                        log("图像已保存到: " + imageFile.getAbsolutePath());
                                    });
                                }
                            }
                        } else {
                            mainHandler.post(() -> {
                                tvStatus.setText("下载失败: " + response.code());
                                log("下载失败，状态码: " + response.code());
                            });
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ResponseBody> call, @NonNull Throwable t) {
                        mainHandler.post(() -> {
                            tvStatus.setText("下载失败: " + t.getMessage());
                            log("下载失败: " + t.getMessage());
                        });
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    tvStatus.setText("下载异常: " + e.getMessage());
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

        tvStatus.setText("正在下载图像...");

        executor.execute(() -> {
            try {
                retrofitClient.getApiService().downloadImage(currentRequestId).enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ResponseBody> call, @NonNull Response<ResponseBody> response) {
                        if (response.isSuccessful()) {
                            try (ResponseBody body = response.body()) {
                                File imageFile = saveImageToFile(body);
                                if (imageFile != null) {
                                    mainHandler.post(() -> {
                                        showToast("图像已保存到: " + imageFile.getAbsolutePath());
                                        tvStatus.setText("图像保存完成");
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
                            tvStatus.setText("下载失败");
                        });
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showToast("下载异常: " + e.getMessage());
                    tvStatus.setText("下载异常");
                });
            }
        });
    }

    private File saveImageToFile(ResponseBody body) {
        try {
            // 创建保存目录
            File downloadDir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "AI_Images");
            if (!downloadDir.exists() && !downloadDir.mkdirs()) {
                Log.e(TAG, "saveImageToFile: failed to execute mkdirs()");
            }

            // 生成文件名
            String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "AI_Image_" + timeStamp + ".png";
            File imageFile = new File(downloadDir, fileName);

            // 保存文件
            if (RetrofitClient.downloadFile(body, imageFile)) {
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
                ivGeneratedImage.setImageBitmap(bitmap);
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
                        tvStatus.setText("状态: " + statusResponse.getStatus() + " - " + statusResponse.getMessage());
                        log("状态更新: " + statusResponse.getStatus());

                        if ("success".equals(statusResponse.getStatus())) {
                            stopStatusChecker();
                            downloadAndDisplayImage();
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
        String currentLog = tvLog.getText().toString();
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String newLog = "[" + timestamp + "] " + message + "\n" + currentLog;
        tvLog.setText(newLog);
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
    }

    private void updateBaseUrl() {
        String ip = requireActivity().getPreferences(Context.MODE_PRIVATE).getString("server_ip", "192.168.1.17");
        String port = requireActivity().getPreferences(Context.MODE_PRIVATE).getString("server_port", "8000");
        // 更新Base URL
        String baseUrl = "http://" + ip + ":" + port;
        retrofitClient.setBaseUrl(baseUrl);
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

    private ArrayList<Object[]> getPromptFromSaved() {
        Set<String> prompts = requireActivity().getPreferences(Context.MODE_PRIVATE).getStringSet("prompts", new HashSet<>());
        ArrayList<Object[]> promptArray = new ArrayList<>();
        for (String jsonString : prompts) {
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                Object[] promptInfo = new Object[]{
                        jsonObject.optLong("timestamp", 0),
                        jsonObject.optString("prompt", "")
                };
                promptArray.add(promptInfo);
            } catch (JSONException e) {
                Log.e(TAG, "getPromptFromSaved. error thrown.", e);
            }
        }

        promptArray.sort((o1, o2) -> Math.toIntExact((long) o2[0] - (long) o1[0]));

        return promptArray;
    }
}