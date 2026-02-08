package com.snuabar.mycomfy.preview;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Insets;
import android.graphics.Rect;
import android.graphics.RectF;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.documentfile.provider.DocumentFile;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.github.chrisbanes.photoview.PhotoView;
import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.common.Common;
import com.snuabar.mycomfy.databinding.ActivityFullScreenImageBinding;
import com.snuabar.mycomfy.databinding.LayoutFullScreenImageItemBinding;
import com.snuabar.mycomfy.databinding.LayoutFullScreenVideoItemBinding;
import com.snuabar.mycomfy.main.data.AbstractMessageModel;
import com.snuabar.mycomfy.main.data.DataIO;
import com.snuabar.mycomfy.main.model.I2IReceivedMessageModel;
import com.snuabar.mycomfy.main.model.I2ISentMessageModel;
import com.snuabar.mycomfy.main.model.ReceivedMessageModel;
import com.snuabar.mycomfy.utils.FileOperator;
import com.snuabar.mycomfy.utils.FilePicker;
import com.snuabar.mycomfy.utils.ImageUtils;
import com.snuabar.mycomfy.utils.VideoUtils;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * 全屏图像浏览器
 */
public class FullScreenImageActivity extends AppCompatActivity {

    public static final String EXTRA_ID_LIST = "extra_id_list";
    public static final String EXTRA_CURRENT_ID = "extra_current_id";
    public static final String EXTRA_I2I_IMAGE_INDEX = "extra_i2i_image_index";
    public static final String TAG = FullScreenImageActivity.class.getName();

    private ActivityFullScreenImageBinding binding;

    private List<AbstractMessageModel> messageList;
    private int currentIndex;
    private int i2iImageIndex;
    private Adapter adapter = null;
    private boolean isFullscreen = false;
    private int mDefaultSystemUIVisibility = 0;
    private FilePicker filePicker;
    private final DisplayInsets displayInsets = new DisplayInsets();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        binding = ActivityFullScreenImageBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        displayInsets.from(this, R.id.view_measure);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(null);
            getSupportActionBar().setTitle(null);
            getSupportActionBar().hide();
        }

        filePicker = new FilePicker(this);

        List<String> messageIds = Collections.unmodifiableList(Objects.requireNonNull(getIntent().getStringArrayListExtra(EXTRA_ID_LIST)));
        String currentId = getIntent().getStringExtra(EXTRA_CURRENT_ID);
        currentIndex = messageIds.indexOf(currentId);
        i2iImageIndex = getIntent().getIntExtra(EXTRA_I2I_IMAGE_INDEX, 0);

        List<AbstractMessageModel> models = DataIO.getInstance().copyMessageModels();
        models.removeIf(m -> !messageIds.contains(m.getId()));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            messageList = new ArrayList<>(models.reversed());
        } else {
            models.sort((o1, o2) -> Long.compare(o2.getUTCTimestamp(), o1.getUTCTimestamp()));
            messageList = new ArrayList<>(models);
        }

        if (currentIndex < 0 || currentIndex >= messageList.size()) {
            currentIndex = 0;
        }

        initViews();
        setupForFullScreen();
        displayInfo();
    }

    @Override
    protected void onDestroy() {
        adapter.recycle();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initViews() {
        adapter = new Adapter(messageList);
        binding.viewPager2.setAdapter(adapter);
        binding.viewPager2.setCurrentItem(currentIndex, false);
        binding.viewPager2.registerOnPageChangeCallback(onPageChangeCallback);
        binding.btnSave.setOnClickListener(v -> saveImage());
        binding.btnShare.setOnClickListener(v -> shareImage());
        binding.btnClose.setOnClickListener(v -> finish());
    }

    private final ViewPager2.OnPageChangeCallback onPageChangeCallback = new ViewPager2.OnPageChangeCallback() {
        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
            super.onPageScrolled(position, positionOffset, positionOffsetPixels);
        }

        @Override
        public void onPageSelected(int position) {
            super.onPageSelected(position);
            displayInfo();
        }

        @Override
        public void onPageScrollStateChanged(int state) {
            super.onPageScrollStateChanged(state);
        }
    };

    private boolean canDisplayInfo() {
        int index = binding.viewPager2.getCurrentItem();
        AbstractMessageModel model = messageList.get(index);
        return model instanceof ReceivedMessageModel;
    }

    private void displayInfo() {
        if (canDisplayInfo()) {
            if (binding.layoutInfoContainer.getVisibility() == View.VISIBLE) {
                adjustInfoLayout();

                int index = binding.viewPager2.getCurrentItem();
                AbstractMessageModel model = messageList.get(index);

                binding.tvName.setText(model.getImageFile().getName());
                binding.tvDate.setText(Common.formatTimestamp(model.getUTCTimestampCompletion()));
                binding.tvFileSize.setText(Common.formatFileSize(model.getImageFile().length()));
                binding.tvPrompt.setText(model.getParameters().getPrompt());
                binding.tvParams.setText(String.format(Locale.getDefault(),
                        "%s\n%s\n%dx%d %d %d %.01f",
                        model.getParameters().getWorkflow(),
                        model.getParameters().getModel(),
                        model.getParameters().getImg_width(), model.getParameters().getImg_height(),
                        model.getParameters().getSeed(),
                        model.getParameters().getStep(),
                        model.getParameters().getCfg()
                ));
                if (model.getParameters().getUpscale_factor() > 1.0) {
                    binding.tvScaleFactor.setVisibility(View.VISIBLE);
                    binding.tvScaleFactor.setText(String.format(Locale.getDefault(),
                            "%.01fx",
                            model.getParameters().getUpscale_factor())
                    );
                } else {
                    binding.tvScaleFactor.setVisibility(View.GONE);
                }
                int[] size;
                if (model.isVideo()) {
                    VideoUtils.VideoSize videoSize = VideoUtils.INSTANCE.getVideoSize(model.getImageFile());
                    size = new int[]{videoSize.getWidth(), videoSize.getHeight()};
                } else {
                    size = ImageUtils.getImageSize(model.getImageFile());
                }
                binding.tvResolution.setText(String.format(Locale.getDefault(), "%d x %d", size[0], size[1]));
            }
        } else {
            binding.layoutInfoContainer.setVisibility(View.INVISIBLE);
        }
    }

    /**
     * Setup for full screen mode.
     */
    private void setupForFullScreen() {
        mDefaultSystemUIVisibility = getWindow().getDecorView().getSystemUiVisibility();

        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            mDefaultSystemUIVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        } else {
            mDefaultSystemUIVisibility |= View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    | View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION;
        }

        getWindow().getDecorView().setSystemUiVisibility(mDefaultSystemUIVisibility);

        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        getWindow().setAttributes(lp);
    }

    /**
     * Hide system status bar.
     */
    private void hideSystemStatusBar() {
        getWindow().getDecorView().setSystemUiVisibility(mDefaultSystemUIVisibility | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
    }

    /**
     * Show system status bar.
     */
    private void showSystemStatusBar() {
        getWindow().getDecorView().setSystemUiVisibility(mDefaultSystemUIVisibility);
    }

    /**
     * Toggle full screen mode.
     */
    private void toggleFullScreen() {
        setFullScreenMode(!isFullscreen);
    }

    private void setFullScreenMode(final boolean fullScreenMode) {
        isFullscreen = fullScreenMode;
        if (isFullscreen) {
            if (canDisplayInfo()) {
                binding.layoutInfoContainer.animate().alpha(0).withEndAction(() -> {
                    binding.layoutInfoContainer.setVisibility(View.INVISIBLE);
                });
            } else {
                binding.layoutInfoContainer.setVisibility(View.INVISIBLE);
            }
            hideSystemStatusBar();
        } else {
            if (canDisplayInfo()) {
                binding.layoutInfoContainer.animate().alpha(1).withStartAction(() -> {
                    binding.layoutInfoContainer.setVisibility(View.VISIBLE);
                    displayInfo();
                });
            } else {
                binding.layoutInfoContainer.setVisibility(View.INVISIBLE);
            }
            showSystemStatusBar();
        }
    }

    private void adjustInfoLayout() {
        ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) binding.layoutInfo.getLayoutParams();
        Rect insect = displayInsets.getWindowInsets();
        lp.rightMargin = insect.right;
        lp.leftMargin = insect.left;
        lp.topMargin = displayInsets.getHeightOfStatusBar(this);
//        lp.bottomMargin = insect.bottom + adapter.getVideoControlLayoutHeight();
        binding.layoutInfo.setLayoutParams(lp);
        binding.layoutInfo.requestLayout();
    }

    private void saveImage() {
        int index = binding.viewPager2.getCurrentItem();
        AbstractMessageModel model = messageList.get(index);
        // 使用文档创建API
        filePicker.pickDirectory(model.getImageFile(), this::onDirectoryPickerWithFileCallback);
    }

    private void shareImage() {
        int index = binding.viewPager2.getCurrentItem();
        AbstractMessageModel model = messageList.get(index);
        FileOperator.shareImageFromLocal(this, model.getImageFile());
    }

    private void onDirectoryPickerWithFileCallback(Uri[] uris, File file) {
        if (uris == null || uris.length == 0 || file == null) {
            return;
        }

        Uri uri = uris[0];
        DocumentFile destDir = DocumentFile.fromTreeUri(this, uri);
        if (destDir != null && destDir.exists()) {
            if (!destDir.isDirectory() || !destDir.exists()) {
                Toast.makeText(this, "保存失败！无效目录！", Toast.LENGTH_SHORT).show();
                return;
            }
            DocumentFile zipDocumentFile = DocumentFile.fromFile(file);
            if (!FileOperator.copyDocumentFile(this, zipDocumentFile, destDir, false)) {
                Toast.makeText(this, "保存失败！", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "保存成功！", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "保存失败！", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * For obtaining display insets
     */
    private class DisplayInsets {
        private View mViewMeasure;

        private void from(@NonNull Activity a, @IdRes int measureViewId) {
            mViewMeasure = a.findViewById(measureViewId);
        }

        /**
         * Gets window insets
         *
         * @return Insets.
         */
        private Rect getWindowInsets() {
            Rect insets = new Rect();
            if (mViewMeasure == null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    Insets insets1 = getWindow().getDecorView().getRootWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.systemBars());
                    insets.left = insets1.left;
                    insets.top = insets1.top;
                    insets.right = insets1.right;
                    insets.bottom = insets1.bottom;
                }
            } else {
                insets.left = mViewMeasure.getPaddingStart();
                insets.top = mViewMeasure.getPaddingTop();
                insets.right = mViewMeasure.getPaddingEnd();
                insets.bottom = mViewMeasure.getPaddingBottom();
            }
            return insets;
        }

        /**
         * Gets the inset only for system navigation bar.
         *
         * @return Insets.
         */
        private Rect getWindowInsetsOfNavigationBar(Context context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && getWindow().getDecorView().getRootWindowInsets() != null) {
                Insets insets = getWindow().getDecorView().getRootWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.navigationBars());
                return new Rect(insets.left, insets.top, insets.right, insets.bottom);
            }

            int barHeight = Common.getNavigationBarHeight(context);
            Rect insets = getWindowInsets();
            //The navigation bar may not certainly be at bottom of the screen.
            adjustInsetsBySize(insets, barHeight);
            return insets;
        }

        /**
         * Gets the height system status bar.
         *
         * @return Height.
         */
        private int getHeightOfStatusBar(Context context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && getWindow().getDecorView().getRootWindowInsets() != null) {
                Insets insets = getWindow().getDecorView().getRootWindowInsets().getInsetsIgnoringVisibility(WindowInsets.Type.statusBars());
                return insets.top;
            }

            return Common.getStatusBarHeight(context);
        }

        /**
         * Adjust insets by specified size, the other 3 size will be set to 0.
         *
         * @param insets Insets to adjust.
         * @param size   Size to match.
         */
        private void adjustInsetsBySize(@NonNull Rect insets, int size) {
            //convert Rect to array.
            int[] arr = new int[]{insets.left, insets.top, insets.right, insets.bottom};
            //find the matched size.
            int i = 0;
            for (; i < arr.length; i++) {
                if (arr[i] == size) {
                    break;
                }
            }
            //empty other size.
            for (int n = 0; n < arr.length; n++) {
                if (n == i) {
                    continue;
                }
                arr[n] = 0;
            }
            //adjust insets.
            insets.left = arr[0];
            insets.top = arr[1];
            insets.right = arr[2];
            insets.bottom = arr[3];
        }
    }

    private void onPhotoViewMatrixChange(int position, PhotoView photoView) {
        if (binding.viewPager2.getCurrentItem() == position) {
            RectF rect = photoView.getDisplayRect();
            DisplayMetrics metrics = getResources().getDisplayMetrics();
//            Log.e("!@#", metrics.widthPixels + "-" + rect.width() + ", " + metrics.heightPixels + "-" + rect.height());
//            Log.e("!@#", String.valueOf(rect));
            boolean zoomed = photoView.getScale() > 1.0f;
            boolean atStart = rect.left == 0;
            boolean atEnd = rect.right == metrics.widthPixels;
            binding.viewPager2.setUserInputEnabled(!zoomed || atStart || atEnd);
        } else {
            binding.viewPager2.setUserInputEnabled(true);
        }
    }

    private class Adapter extends RecyclerView.Adapter<Adapter.BaseHolder> {

        private final List<AbstractMessageModel> messageModels;
        private final Map<Integer, WeakReference<Bitmap>> bitmapRefs;
        private final HashSet<VideoHolder> videoHolders = new HashSet<>();
        private Bitmap bitmapTemp;
        private String imageFilePathTemp;

        private Adapter(List<AbstractMessageModel> messageModels) {
            this.messageModels = messageModels;
            this.bitmapRefs = new HashMap<>();
        }

        @NonNull
        @Override
        public BaseHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            if (viewType == 1) {
                return new VideoHolder(LayoutFullScreenVideoItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
            }
            return new ImageHolder(LayoutFullScreenImageItemBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull BaseHolder holder, int position) {
            AbstractMessageModel model = messageModels.get(position);
            if (model.isVideo()) {
                VideoHolder videoHolder = (VideoHolder) holder;
                videoHolder.binding.videoView.setVideoSource(model.getImageFile().getAbsolutePath());
                videoHolders.add(videoHolder);
            } else {
                ImageHolder imageHolder = (ImageHolder) holder;
                Bitmap bitmap = getBitmap(position);
                imageHolder.binding.photoView.setImageBitmap(bitmap);
                float[] scales = Common.getPhotoViewScales(holder.itemView.getContext(), bitmap.getWidth(), bitmap.getHeight());
                imageHolder.binding.photoView.setMaximumScale(scales[0]);
                imageHolder.binding.photoView.setMediumScale(scales[1]);
                imageHolder.binding.photoView.setZoomTransitionDuration(holder.itemView.getResources().getInteger(android.R.integer.config_longAnimTime));
                if (model.isI2I() && model instanceof I2IReceivedMessageModel) {
                    imageHolder.binding.photoView.setImagePaths(
                            model.getImageFile().getAbsolutePath(),
                            model.getParameters().getImageFiles()[0].getAbsolutePath()
                    );
                    imageHolder.binding.btnCompare.setVisibility(View.VISIBLE);
                } else {
                    imageHolder.binding.photoView.setImagePaths();
                    imageHolder.binding.btnCompare.setVisibility(View.INVISIBLE);
                }
            }
        }

        @Override
        public int getItemCount() {
            return messageModels.size();
        }

        @Override
        public int getItemViewType(int position) {
            if (messageModels.get(position).isVideo()) {
                return 1;
            }
            return super.getItemViewType(position);
        }

        private Bitmap getBitmap(int position) {
            AbstractMessageModel model = messageModels.get(position);
            WeakReference<Bitmap> bitmapRef = bitmapRefs.get(position);
            Bitmap bitmap;
            if (bitmapRef != null && bitmapRef.get() != null) {
                bitmap = bitmapRef.get();
            } else {
                if (model.isI2I() && model instanceof I2ISentMessageModel) {
                    bitmap = BitmapFactory.decodeFile(model.getParameters().getImageFiles()[i2iImageIndex].getAbsolutePath());
                } else {
                    bitmap = BitmapFactory.decodeFile(model.getImageFile().getAbsolutePath());
                }
                bitmapRefs.put(position, new WeakReference<>(bitmap));
            }
            return bitmap;
        }

        public int getVideoControlLayoutHeight() {
            for (VideoHolder videoHolder : videoHolders) {
                return videoHolder.binding.videoView.getControlLayoutHeight();
            }
            return 0;
        }

        public void recycle() {
            for (WeakReference<Bitmap> bitmapRef : bitmapRefs.values()) {
                if (bitmapRef != null && bitmapRef.get() != null && !bitmapRef.get().isRecycled()) {
                    bitmapRef.get().recycle();
                }
            }
            bitmapRefs.clear();

            if (bitmapTemp != null && !bitmapTemp.isRecycled()) {
                bitmapTemp.recycle();
            }

            for (VideoHolder videoHolder : videoHolders) {
                videoHolder.binding.videoView.release();
            }
            videoHolders.clear();
        }

        private class BaseHolder extends RecyclerView.ViewHolder {

            public BaseHolder(@NonNull View itemView) {
                super(itemView);
            }
        }

        private class ImageHolder extends BaseHolder {
            private final LayoutFullScreenImageItemBinding binding;

            public ImageHolder(@NonNull LayoutFullScreenImageItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                // 设置双击缩放和手势缩放
                binding.photoView.setScaleType(ImageView.ScaleType.FIT_CENTER);
                binding.photoView.setOnMatrixChangeListener(rect -> onPhotoViewMatrixChange(getAbsoluteAdapterPosition(), binding.photoView));
                // 点击切换全屏
                binding.photoView.setOnClickListener(v -> toggleFullScreen());
                binding.btnCompare.setOnStateChangeListener(isOn -> binding.photoView.showImageBitmap(isOn ? 1 : 0));
            }
        }

        private class VideoHolder extends BaseHolder {
            private final LayoutFullScreenVideoItemBinding binding;

            public VideoHolder(@NonNull LayoutFullScreenVideoItemBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
                // 点击切换全屏
                itemView.setOnClickListener(v -> {
                    toggleFullScreen();
                    binding.videoView.toggleControls();
                });

            }
        }
    }
}
