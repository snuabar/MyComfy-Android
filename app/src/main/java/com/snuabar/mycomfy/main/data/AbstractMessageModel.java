package com.snuabar.mycomfy.main.data;

import android.content.Context;
import android.util.Log;

import androidx.annotation.CallSuper;

import com.snuabar.mycomfy.client.Parameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractMessageModel {
    private final static String TAG = AbstractMessageModel.class.getName();

    private String id;

    public AbstractMessageModel() {
        id = UUID.randomUUID().toString().replaceAll("-", "");
    }

    public String getId() {
        return id;
    }

    public abstract Parameters getParameters();

    public abstract boolean isFileExistsOnServer();

    /**
     * 响应收到的时间
     *
     * @return UTC时间
     */
    public abstract long getUTCTimestamp();

    /**
     * 完成的时间
     *
     * @return UTC时间
     */
    public abstract long getUTCTimestampCompletion();

    public abstract String getPromptId();

    public abstract String getMessage();

    public abstract int getCode();

    public abstract void setImageFile(File imageFile);

    public abstract File getImageFile();

    public abstract void setThumbnailFile(File thumbnailFile);

    public abstract File getThumbnailFile();

    public abstract boolean setStatus(String status, int code, String message);

    public abstract String getStatus();

    public abstract String getStatusResourceString(Context context);

    public abstract void setFinished(File imageFile, int code, String message);

    public abstract void setFinished(File imageFile, int code, String message, String endTime);

    public abstract boolean isFinished();

    public abstract boolean getInterruptionFlag();

    public abstract void setInterruptionFlag(boolean interruption);

    public abstract boolean isVideo();

    public abstract int[] getImageSize();

    public abstract boolean isI2I();

    public abstract String getAssociatedSentModelId();

    @CallSuper
    public JSONObject toJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("id", id);
            return jsonObject;
        } catch (JSONException e) {
            Log.e(TAG, "toJson. exception thrown.", e);
        }
        return null;
    }

    @CallSuper
    public void fromJson(JSONObject jsonObject) {
        id = jsonObject.optString("id", null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        AbstractMessageModel model = (AbstractMessageModel) o;
        return Objects.equals(id, model.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
