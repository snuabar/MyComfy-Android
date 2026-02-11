package com.snuabar.mycomfy.main.model;

import android.content.Context;
import android.util.Log;

import com.snuabar.mycomfy.R;
import com.snuabar.mycomfy.client.Parameters;
import com.snuabar.mycomfy.main.data.AbstractMessageModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Map;
import java.util.Objects;

public class MessageModel extends AbstractMessageModel {
    private final static String TAG = MessageModel.class.getName();

    //region 来至服务器的状态
    public final static String STATUS_PENDING = "pending";
    public final static String STATUS_IN_PROGRESS = "in_progress";
    public final static String STATUS_COMPLETED = "completed";
    public final static String STATUS_FAILED = "failed";
    //endregion

    public static final Map<String, Integer> STATUS_RES_MAP = Map.of(
            STATUS_PENDING, R.string.status_pending,
            STATUS_IN_PROGRESS, R.string.status_in_progress,
            STATUS_COMPLETED, R.string.status_completed,
            STATUS_FAILED, R.string.status_failed
    );

    protected Parameters parameters;
    protected String status;
    private int code;
    private String message;

    @Override
    public Parameters getParameters() {
        return parameters;
    }

    @Override
    public boolean isFileExistsOnServer() {
        return false;
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
        return message;
    }

    @Override
    public int getCode() {
        return code;
    }

    protected void setCode(int code) {
        this.code = code;
    }

    protected void setMessage(String message) {
        this.message = message;
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
    public boolean setStatus(String status, int code, String message) {
        if (!Objects.equals(this.status, status) || this.code != code || !Objects.equals(this.message, message)) {
            this.status = status;
            this.code = code;
            this.message = message;
            return true;
        }
        return false;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public String getStatusResourceString(Context context) {
        int res = Objects.requireNonNullElse(STATUS_RES_MAP.getOrDefault(status, 0), 0);
        if (res == 0) {
            return "";
        }
        return context.getString(res);
    }

    @Override
    public void setFinished(File imageFile, int code, String message) {

    }

    @Override
    public void setFinished(File imageFile, int code, String message, String endTime) {

    }

    @Override
    public boolean isFinished() {
        return false;
    }

    @Override
    public boolean getInterruptionFlag() {
        return false;
    }

    @Override
    public void setInterruptionFlag(boolean interruption) {
    }

    @Override
    public boolean isVideo() {
        return false;
    }

    @Override
    public int[] getImageSize() {
        return new int[]{0, 0};
    }

    @Override
    public boolean isI2I() {
        return false;
    }

    @Override
    public String getAssociatedSentModelId() {
        return "";
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        try {
            if (parameters != null) {
                jsonObject.putOpt("parameters", parameters.toJson());
            }
            jsonObject.putOpt("status", status);
            jsonObject.putOpt("code", code);
            jsonObject.putOpt("message", message);
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
        status = jsonObject.optString("status");
        code = jsonObject.optInt("code");
        message = jsonObject.optString("message", null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        MessageModel that = (MessageModel) o;
        return code == that.code && Objects.equals(parameters, that.parameters) && Objects.equals(status, that.status) && Objects.equals(message, that.message);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), parameters, status, code, message);
    }
}
