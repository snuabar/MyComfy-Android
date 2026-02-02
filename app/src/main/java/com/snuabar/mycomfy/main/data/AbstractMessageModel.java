package com.snuabar.mycomfy.main.data;

import android.util.Log;

import androidx.annotation.CallSuper;
import androidx.annotation.Nullable;

import com.snuabar.mycomfy.client.Parameters;
import com.snuabar.mycomfy.main.model.ReceivedMessageModel;
import com.snuabar.mycomfy.main.model.SentMessageModel;
import com.snuabar.mycomfy.main.model.UpscaleReceivedMessageModel;
import com.snuabar.mycomfy.main.model.UpscaleSentMessageModel;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

public abstract class AbstractMessageModel implements Serializable {
    private final static String TAG = AbstractMessageModel.class.getName();
    private final static String CLASS_IDENTITY_KEY = "class.identify.key.to.create.class.from.json";

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

    public abstract void setImageResponseCode(int imageResponseCode);

    public abstract void setImageResponseMessage(String imageResponseMessage);

    public abstract void setFinished(File imageFile, int code, String message);

    public abstract boolean isFinished();

    public abstract void setFailureMessage(String failureMessage);

    public abstract String getFailureMessage();

    public abstract boolean getInterruptionFlag();

    public abstract void setInterruptionFlag(boolean interruption);

    @CallSuper
    public JSONObject toJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt(CLASS_IDENTITY_KEY, this.getClass().getName());
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

    @Nullable
    public static AbstractMessageModel Create(JSONObject jsonObject) {
        if (jsonObject != null && jsonObject.has(CLASS_IDENTITY_KEY)) {
            String className = jsonObject.optString(CLASS_IDENTITY_KEY, null);
            if (ReceivedMessageModel.class.getName().equals(className)) {
                return new ReceivedMessageModel(jsonObject);
            }
            if (UpscaleReceivedMessageModel.class.getName().equals(className)) {
                return new UpscaleReceivedMessageModel(jsonObject);
            }
            if (SentMessageModel.class.getName().equals(className)) {
                return new SentMessageModel(jsonObject);
            }
            if (UpscaleSentMessageModel.class.getName().equals(className)) {
                return new UpscaleSentMessageModel(jsonObject);
            }
        }
        return null;
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
