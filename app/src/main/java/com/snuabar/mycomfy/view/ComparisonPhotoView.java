package com.snuabar.mycomfy.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.AttributeSet;

import com.github.chrisbanes.photoview.PhotoView;

public class ComparisonPhotoView extends PhotoView {
    private String[] imagePaths;
    private Bitmap[] bitmaps;
    private boolean freeBitmap = true;

    public ComparisonPhotoView(Context context) {
        super(context);
    }

    public ComparisonPhotoView(Context context, AttributeSet attr) {
        super(context, attr);
    }

    public ComparisonPhotoView(Context context, AttributeSet attr, int defStyle) {
        super(context, attr, defStyle);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        freeBitmaps();
    }

    @Override
    public void setImageBitmap(Bitmap bm) {
        super.setImageBitmap(bm);
        if (freeBitmap) {
            freeBitmaps();
        }
    }

    public void setImagePaths(String... imagePaths) {
        this.imagePaths = imagePaths;
        freeBitmaps();
        bitmaps = new Bitmap[this.imagePaths.length];
    }

    private void freeBitmaps() {
        if (bitmaps != null) {
            for (Bitmap bmp : bitmaps) {
                if (bmp != null && !bmp.isRecycled()) {
                    bmp.recycle();
                }
            }
        }
    }

    private Bitmap getBitmap(int index) {
        if (imagePaths != null && imagePaths.length == 2 && bitmaps != null && bitmaps.length == 2) {
            if (bitmaps[index] == null) {
                bitmaps[index] = BitmapFactory.decodeFile(imagePaths[index]);
            }
            return bitmaps[index];
        }
        return null;
    }

    public void showImageBitmap(int index) {
        freeBitmap = false;
        setImageBitmap(getBitmap(index));
        freeBitmap = true;
    }
}
