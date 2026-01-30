package com.snuabar.mycomfy.main.model;

import android.util.Log;

import com.snuabar.mycomfy.client.Parameters;
import com.snuabar.mycomfy.main.data.AbstractMessageModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Objects;

public class MessageModel extends AbstractMessageModel {
    private final static String TAG = MessageModel.class.getName();

    protected Parameters parameters;

    @Override
    public AbstractParameters getParameters() {
        return parameters;
    }

    @Override
    public long getUTCTimestamp() {
        if (parameters != null) {
            return parameters.getTimestamp();
        }
        return 0;
    }

    @Override
    public long getUTCTimestampCompletion() {
        return 0;
    }

    @Override
    public String getPromptId() {
        return "";
    }

    @Override
    public String getMessage() {
        return "";
    }

    @Override
    public int getCode() {
        return 0;
    }

    @Override
    public void setImageFile(File imageFile) {

    }

    @Override
    public File getImageFile() {
        return null;
    }

    @Override
    public void setThumbnailFile(File thumbnailFile) {

    }

    @Override
    public File getThumbnailFile() {
        return null;
    }

    @Override
    public void setImageResponseCode(int imageResponseCode) {

    }

    @Override
    public void setImageResponseMessage(String imageResponseMessage) {

    }

    @Override
    public void setFinished() {

    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public void setFailureMessage(String failureMessage) {

    }

    @Override
    public String getFailureMessage() {
        return "";
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        try {
            if (parameters != null) {
                jsonObject.putOpt("parameters", parameters.toJson());
            }
            return jsonObject;
        } catch (JSONException e) {
            Log.e(TAG, "toJson. exception thrown.", e);
        }
        return null;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        parameters = new Parameters();
        parameters.loadJson(jsonObject.optJSONObject("parameters"));
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        MessageModel model = (MessageModel) o;
        return Objects.equals(parameters, model.parameters);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(parameters);
    }
}
