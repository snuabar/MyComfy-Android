package com.snuabar.mycomfy.setting;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;

import com.snuabar.mycomfy.client.RetrofitClient;
import com.snuabar.mycomfy.client.ServerStats;
import com.snuabar.mycomfy.databinding.ActivitySettingsBinding;
import com.snuabar.mycomfy.main.data.AbstractMessageModel;
import com.snuabar.mycomfy.main.data.DataHelper;
import com.snuabar.mycomfy.main.data.DataIO;
import com.snuabar.mycomfy.utils.FilePicker;
import com.snuabar.mycomfy.utils.ImageUtils;
import com.snuabar.mycomfy.utils.ThumbnailCacheManager;
import com.snuabar.mycomfy.view.TwoButtonPopup;
import com.snuabar.mycomfy.view.dialog.OptionalDialog;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;
    private final Executor executor = Executors.newSingleThreadExecutor();
    private final FilePicker filePicker;
    private final DataHelper dataHelper;
    private final Handler handler;
    private final OptionalDialog.ProgressDialog pgsDlg;
    private TwoButtonPopup twoButtonPopup;

    public SettingsActivity() {
        filePicker = new FilePicker(this);
        dataHelper = new DataHelper(this);
        handler = new Handler(Looper.getMainLooper());
        pgsDlg = new OptionalDialog.ProgressDialog();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // 设置返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("设置");
        }

        initViews();
        setupListeners();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            getOnBackPressedDispatcher().onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        Settings.getInstance().edit()
                .putString("server_ip", binding.etIpAddress.getText().toString().trim())
                .putString("server_port", binding.etPort.getText().toString().trim())
                .apply();
        super.onPause();
    }

    private void initViews() {
        binding.etIpAddress.setText(Settings.getInstance().getString("server_ip", "192.168.1.17"));
        binding.etPort.setText(Settings.getInstance().getString("server_port", "8000"));
    }

    private void setupListeners() {
        // 测试连接按钮监听器
        binding.btnTestConnection.setOnClickListener(v -> testConnection());
        binding.btnImport.setOnClickListener(v -> importData(null));
        binding.btnExport.setOnClickListener(v -> exportData(null));
        binding.btnRebuildThumbnails.setOnClickListener(v -> rebuiltThumbnails());
    }

    private void importData(Uri fileUri) {
        if (fileUri == null) {
            filePicker.pickFile(false, this::onFilePickerCallback);
            return;
        }
        pgsDlg.setText("正在导入...").show(getSupportFragmentManager());
        dataHelper.executeImport(fileUri, succeeded -> {
            pgsDlg.dismiss();
            if (succeeded) {
                Settings.getInstance().setDataImportedState();
                Toast.makeText(this, "导入成功", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "导入失败！", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onFilePickerCallback(Uri[] uris) {
        if (uris == null || uris.length == 0) {
            return;
        }

        Uri uri = uris[0];
        DocumentFile documentFile = DocumentFile.fromSingleUri(this, uri);
        if (documentFile.exists()) {
            handler.post(() -> importData(uri));
        } else {
            Toast.makeText(this, "导入失败！", Toast.LENGTH_SHORT).show();
        }
    }

    private void exportData(Uri destinationUri) {
        if (destinationUri == null) {
            filePicker.pickDirectory(this::onDirectoryPickerCallback);
            return;
        }
        pgsDlg.setText("正在导出...").show(getSupportFragmentManager());
        dataHelper.executeExport(null, destinationUri, state -> {
            pgsDlg.dismiss();
            if (state == DataHelper.STATE_OK) {
                Toast.makeText(this, "导出成功！", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "导出失败！", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void onDirectoryPickerCallback(Uri[] uris) {
        if (uris == null || uris.length == 0) {
            return;
        }

        Uri uri = uris[0];
        DocumentFile documentFile = DocumentFile.fromTreeUri(this, uri);
        if (documentFile != null && documentFile.exists()) {
            handler.post(() -> exportData(uri));
        } else {
            Toast.makeText(this, "导出失败！", Toast.LENGTH_SHORT).show();
        }
    }

    private void testConnection() {
        // TODO: 实现测试连接功能
        String ip = binding.etIpAddress.getText().toString().trim();
        String port = binding.etPort.getText().toString().trim();

        RetrofitClient retrofitClient = RetrofitClient.getInstance();

        if (ip.isEmpty() || port.isEmpty()) {
            binding.tvStatus.setText("请输入服务器IP和端口");
            return;
        }

        // 更新Base URL
        retrofitClient.setBaseUrl(ip, port);

        binding.tvStatus.setText("测试连接到: " + retrofitClient.getBaseUrl());

        // 发送测试请求
        executor.execute(() -> {
            try {
                retrofitClient.getApiService().getServerStats().enqueue(new Callback<>() {
                    @Override
                    public void onResponse(@NonNull Call<ServerStats> call, @NonNull Response<ServerStats> response) {
                        if (response.isSuccessful() && response.body() != null) {
                            ServerStats stats = response.body();
                            runOnUiThread(() -> binding.tvStatus.setText("连接成功。\n服务器状态:\n" + "总图像数: " + stats.getTotal_images() + "\n" + "存储使用: " + stats.getStorage_used_mb() + " MB"));
                        } else {
                            runOnUiThread(() -> binding.tvStatus.setText("连接失败，状态码: " + response.code()));
                        }
                    }

                    @Override
                    public void onFailure(@NonNull Call<ServerStats> call, @NonNull Throwable t) {
                        runOnUiThread(() -> binding.tvStatus.setText("连接失败: " + t.getMessage()));
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> binding.tvStatus.setText("连接异常: " + e.getMessage()));
            }
        });
    }

    private void rebuiltThumbnails() {
        if (twoButtonPopup == null) {
            twoButtonPopup = new TwoButtonPopup(this).setListener((view, button) -> {
                if (button == 1) {
                    pgsDlg.setText(null).show(getSupportFragmentManager());
                    executor.execute(() -> {
                        List<AbstractMessageModel> models = DataIO.getInstance().copyMessageModels();
                        for (AbstractMessageModel model : models) {
                            if (model.getImageFile() != null) {
                                ImageUtils.deleteThumbnail(model.getImageFile());
                                ThumbnailCacheManager.Companion.getInstance().clearCacheForPath(model.getImageFile().getAbsolutePath());
                            }
                        }
                        runOnUiThread(pgsDlg::dismiss);
                    });
                }
                return true;
            });
        }
        twoButtonPopup.showAsDropDown(binding.btnRebuildThumbnails);
    }
}
