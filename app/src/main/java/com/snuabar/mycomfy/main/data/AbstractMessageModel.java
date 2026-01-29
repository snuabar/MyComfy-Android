package com.snuabar.mycomfy.main.data;

import com.snuabar.mycomfy.main.model.AbstractParameters;

import org.json.JSONObject;

import java.io.File;
import java.io.Serializable;
import java.util.Objects;

public abstract class AbstractMessageModel implements Serializable {
    String id;

    public abstract AbstractParameters getParameters();

    public abstract long getUTCTimestamp();

    public abstract String getPromptId();

    public abstract String getMessage();

    public abstract int getCode();

    public abstract void setImageFile(File imageFile);

    public abstract File getImageFile();

    public abstract void setThumbnailFile(File thumbnailFile);

    public abstract File getThumbnailFile();

    public abstract void setImageResponseCode(int imageResponseCode);

    public abstract void setImageResponseMessage(String imageResponseMessage);

    public abstract void setFinished();

    public abstract boolean isFinished();

    public abstract void setFailureMessage(String failureMessage);

    public abstract String getFailureMessage();

    public abstract JSONObject toJson();

    public abstract void loadJson(JSONObject jsonObject);

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
