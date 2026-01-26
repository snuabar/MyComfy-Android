package com.snuabar.mycomfy.utils;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.snuabar.mycomfy.common.Callbacks;

public final class FilePicker {

    private static final String Tag = FilePicker.class.getName();

    private final Context context;
    private final ActivityResultLauncher<Intent> mFilePickerLauncher;
    private final ActivityResultLauncher<Intent> mFileOpenLauncher;
    private Callbacks.CallbackT<Uri[]> directoryPickerCallback;
    private Callbacks.CallbackT<Uri[]> mSelectFileCallback;


    public FilePicker(Context context) {
        this.context = context.getApplicationContext();

        mFilePickerLauncher = ((AppCompatActivity) context).registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (directoryPickerCallback != null && result != null && result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri destinationDirUri = result.getData().getData();
                if (destinationDirUri != null) {
                    // 获取持久化权限
                    this.context.getContentResolver().takePersistableUriPermission(
                            destinationDirUri,
                            Intent.FLAG_GRANT_READ_URI_PERMISSION |
                                    Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                    );

                    directoryPickerCallback.apply(new Uri[]{destinationDirUri});
                }
            }
        });
        mFileOpenLauncher = ((AppCompatActivity) context).registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (mSelectFileCallback != null && result != null && result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                Uri destinationDirUri = result.getData().getData();
                if (destinationDirUri != null) {
                    mSelectFileCallback.apply(new Uri[]{destinationDirUri});
                } else if (result.getData().getClipData() != null) {
                    ClipData clipData = result.getData().getClipData();
                    int itemCount = clipData.getItemCount();
                    Uri[] uris = new Uri[itemCount];
                    for (int i = 0; i < itemCount; i++) {
                        uris[i] = clipData.getItemAt(i).getUri();
                    }
                    mSelectFileCallback.apply(uris);
                }
            }
        });
    }

    // 启动目录选择器
    public void pickDirectory(Callbacks.CallbackT<Uri[]> callback) {
        directoryPickerCallback = callback;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false);
        mFilePickerLauncher.launch(intent);
    }

    public void pickFile(boolean allowMultiple, Callbacks.CallbackT<Uri[]> callback) {
        mSelectFileCallback = callback;
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple);
        mFileOpenLauncher.launch(intent);
    }
}
