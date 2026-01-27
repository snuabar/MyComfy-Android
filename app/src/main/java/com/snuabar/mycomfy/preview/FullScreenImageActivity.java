package com.snuabar.mycomfy.preview;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.snuabar.mycomfy.databinding.ActivityFullScreenImageBinding;

/**
 * 全屏图像浏览器
 */
public class FullScreenImageActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_PATH = "extra_image_path";
    public static final String TAG = FullScreenImageActivity.class.getName();

    private ActivityFullScreenImageBinding binding;

    private String imagePath;
    private Bitmap bitmap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFullScreenImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 隐藏状态栏和导航栏
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        imagePath = getIntent().getStringExtra(EXTRA_IMAGE_PATH);

        initViews();
        loadImage();
    }

    @Override
    protected void onDestroy() {
        binding.photoView.setImageBitmap(null);
        if (bitmap != null && !bitmap.isRecycled()) {
            bitmap.recycle();
        }
        super.onDestroy();
    }

    private void initViews() {
        // 设置双击缩放和手势缩放
        binding.photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
        binding.photoView.setMaximumScale(6.0f);
        binding.photoView.setMediumScale(3.0f);

        // 添加退出按钮或手势
        binding.photoView.setOnClickListener(v -> finish());
    }

    private void loadImage() {
        try {
            bitmap = BitmapFactory.decodeFile(imagePath);
            binding.photoView.setImageBitmap(bitmap);
        } catch (Exception e) {
            Log.e(TAG, "Failed to load bitmap.");
        }
    }
}
