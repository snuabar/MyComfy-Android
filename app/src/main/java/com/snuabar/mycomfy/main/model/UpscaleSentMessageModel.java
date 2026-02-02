package com.snuabar.mycomfy.main.model;

import android.text.TextUtils;
import android.util.Log;

import com.snuabar.mycomfy.client.Parameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;

public class UpscaleSentMessageModel extends SentMessageModel {

    private static final String TAG = UpscaleSentMessageModel.class.getName();

    private File imageFile;
    private File thumbnailFile;

    public UpscaleSentMessageModel(Parameters parameters) {
        super(parameters);
    }

    public UpscaleSentMessageModel(JSONObject object) {
        super(object);
    }

    @Override
    public void setImageFile(File imageFile) {
        this.imageFile = imageFile;
    }

    @Override
    public File getImageFile() {
        return imageFile;
    }

    @Override
    public void setThumbnailFile(File thumbnailFile) {
        this.thumbnailFile = thumbnailFile;
    }

    @Override
    public File getThumbnailFile() {
        return thumbnailFile;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        try {
            if (imageFile != null) {
                jsonObject.putOpt("imageFile", imageFile.getAbsolutePath());
            }
            if (thumbnailFile != null) {
                jsonObject.putOpt("thumbnailFile", thumbnailFile.getAbsolutePath());
            }
        } catch (JSONException e) {
            Log.e(TAG, "toJson. Failed to execute putOpt.", e);
        }
        return jsonObject;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        String imageFilePath = jsonObject.optString("imageFile", null);
        if (!TextUtils.isEmpty(imageFilePath)) {
            imageFile = new File(imageFilePath);
        }
        String thumbnailFilePath = jsonObject.optString("thumbnailFile", null);
        if (!TextUtils.isEmpty(thumbnailFilePath)) {
            thumbnailFile = new File(thumbnailFilePath);
        }
    }

}
