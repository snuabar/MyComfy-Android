package com.snuabar.mycomfy.setting;

import android.content.Context;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.client.RetrofitClient;
import com.snuabar.mycomfy.client.ServerStats;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SettingsActivity extends AppCompatActivity {

    private EditText etIpAddress;
    private EditText etPort;
    private Button btnTestConnection;
    private TextView tvStatus;
    private final Executor executor = Executors.newSingleThreadExecutor();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // 设置返回按钮
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("设置");
        }

        initViews();
        setupListeners();
    }

    private void initViews() {
        etIpAddress = findViewById(R.id.et_ip_address);
        etPort = findViewById(R.id.et_port);
        btnTestConnection = findViewById(R.id.btn_test_connection);
        tvStatus = findViewById(R.id.tvStatus);

        etIpAddress.setText(getPreferences(Context.MODE_PRIVATE).getString("server_ip", "192.168.1.17"));
        etPort.setText(getPreferences(Context.MODE_PRIVATE).getString("server_port", "8000"));
    }

    private void setupListeners() {
        // 测试连接按钮监听器
        btnTestConnection.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO: 实现测试连接功能
                String ip = etIpAddress.getText().toString().trim();
                String port = etPort.getText().toString().trim();

                RetrofitClient retrofitClient = RetrofitClient.getInstance(SettingsActivity.this);

                if (ip.isEmpty() || port.isEmpty()) {
                    tvStatus.setText("请输入服务器IP和端口");
                    return;
                }

                // 更新Base URL
                String baseUrl = "http://" + ip + ":" + port;
                retrofitClient.setBaseUrl(baseUrl);

                tvStatus.setText("测试连接到: " + baseUrl);
//        tvStatus.setText("正在测试连接...");

                // 发送测试请求
                executor.execute(() -> {
                    try {
                        retrofitClient.getApiService().getServerStats().enqueue(new Callback<>() {
                            @Override
                            public void onResponse(@NonNull Call<ServerStats> call, @NonNull Response<ServerStats> response) {
                                if (response.isSuccessful() && response.body() != null) {
                                    ServerStats stats = response.body();
                                    runOnUiThread(() -> tvStatus.setText("连接成功。服务器状态:\n" + "总图像数: " + stats.getTotal_images() + "\n" + "存储使用: " + stats.getStorage_used_mb() + " MB"));
                                } else {
                                    runOnUiThread(() -> tvStatus.setText("连接失败，状态码: " + response.code()));
                                }
                            }

                            @Override
                            public void onFailure(@NonNull Call<ServerStats> call, @NonNull Throwable t) {
                                runOnUiThread(() -> tvStatus.setText("连接失败: " + t.getMessage()));
                            }
                        });
                    } catch (Exception e) {
                        runOnUiThread(() -> tvStatus.setText("连接异常: " + e.getMessage()));
                    }
                });
            }
        });
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
        getPreferences(Context.MODE_PRIVATE).edit()
                .putString("server_ip", etIpAddress.getText().toString())
                .putString("server_port", etPort.getText().toString())
                .apply();
        super.onPause();
    }
}
