package com.snuabar.mycomfy.main.model;

import android.text.TextUtils;
import android.util.Log;

import com.snuabar.mycomfy.client.EnqueueResponse;
import com.snuabar.mycomfy.utils.ImageUtils;
import com.snuabar.mycomfy.utils.VideoUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.time.Clock;
import java.util.Arrays;
import java.util.Objects;

public class ReceivedMessageModel extends MessageModel {

    private static final String TAG = ReceivedMessageModel.class.getName();

    private EnqueueResponse response;
    private File imageFile;
    private File thumbnailFile;
    private long utcTimestamp = 0;
    private boolean interruptionFlag = false;
    private int[] imageSize;

    public ReceivedMessageModel(EnqueueResponse response) {
        super();
        this.response = response;
        this.parameters = this.response.getParameters();
    }

    public ReceivedMessageModel(JSONObject object) {
        super();
        this.fromJson(object);
    }

    @Override
    public String getPromptId() {
        return response != null ? response.getPrompt_id() : null;
    }

    @Override
    public boolean isFileExistsOnServer() {
        return response != null && response.isFile_exists();
    }

    @Override
    public long getUTCTimestamp() {
        return response.getUTCTimestamp();
    }

    @Override
    public long getUTCTimestampCompletion() {
        if (isFileExistsOnServer()) {
            return getUTCTimestamp();
        }
        return utcTimestamp;
    }

    @Override
    public String getMessage() {
        if (response != null && response.getCode() != 200) {
            return response.getMessage();
        }
        return super.getMessage();
    }

    @Override
    public int getCode() {
        if (response != null && response.getCode() != 200) {
            return response.getCode();
        }
        return super.getCode();
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
    public void setFinished(File imageFile, int code, String message) {
        utcTimestamp = Clock.systemUTC().millis();
        if (!Objects.equals(this.imageFile, imageFile)) {
            this.imageFile = imageFile;
            refetchImageSize();
        }
        setCode(code);
        setMessage(message);
    }

    @Override
    public void setFinished(File imageFile, int code, String message, String endTime) {
        long timestamp;
        try {
            timestamp = Long.parseLong(endTime);
        } catch (NumberFormatException e) {
            timestamp = Clock.systemUTC().millis();
        }
        utcTimestamp = timestamp;
        this.imageFile = imageFile;
        setCode(code);
        setMessage(message);
    }

    @Override
    public boolean isFinished() {
        return utcTimestamp != 0;
    }

    @Override
    public void setInterruptionFlag(boolean interruption) {
        this.interruptionFlag = interruption;
    }

    @Override
    public boolean getInterruptionFlag() {
        return interruptionFlag;
    }

    @Override
    public int[] getImageSize() {
        return imageSize == null ? refetchImageSize() : imageSize;
    }

    private int[] refetchImageSize() {
        if (this.imageFile != null && this.imageFile.exists()) {
            if (isVideo()) {
                VideoUtils.VideoSize videoSize = VideoUtils.INSTANCE.getVideoSize(this.imageFile);
                imageSize = new int[]{videoSize.getWidth(), videoSize.getHeight()};
            } else {
                imageSize = ImageUtils.getImageSize(this.imageFile);
            }
        }
        return imageSize;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        try {
            if (response != null) {
                jsonObject.putOpt("response", response.toJson());
            }
            if (imageFile != null) {
                jsonObject.putOpt("imageFile", imageFile.getAbsolutePath());
            }
            if (thumbnailFile != null) {
                jsonObject.putOpt("thumbnailFile", thumbnailFile.getAbsolutePath());
            }
            jsonObject.putOpt("utcTimestamp", utcTimestamp);
            jsonObject.putOpt("interruptionFlag", interruptionFlag);
            if (imageSize != null) {
                jsonObject.putOpt("imageSize", imageSize);
            }
        } catch (JSONException e) {
            Log.e(TAG, "toJson. Failed to execute putOpt.", e);
        }
        return jsonObject;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        JSONObject resJson = jsonObject.optJSONObject("response");
        if (resJson != null) {
            response = new EnqueueResponse();
            response.loadJson(resJson);
        }
        String imageFilePath = jsonObject.optString("imageFile", null);
        if (!TextUtils.isEmpty(imageFilePath)) {
            imageFile = new File(imageFilePath);
        }
        String thumbnailFilePath = jsonObject.optString("thumbnailFile", null);
        if (!TextUtils.isEmpty(thumbnailFilePath)) {
            thumbnailFile = new File(thumbnailFilePath);
        }
        utcTimestamp = jsonObject.optLong("utcTimestamp");
        interruptionFlag = jsonObject.optBoolean("interruptionFlag");
        if (jsonObject.has("imageSize")) {
            imageSize = (int[]) jsonObject.opt("imageSize");
        } else {
            imageSize = refetchImageSize();
        }
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ReceivedMessageModel that = (ReceivedMessageModel) o;
        return utcTimestamp == that.utcTimestamp && interruptionFlag == that.interruptionFlag && Objects.equals(response, that.response) && Objects.equals(imageFile, that.imageFile) && Objects.equals(thumbnailFile, that.thumbnailFile) && Objects.deepEquals(imageSize, that.imageSize);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), response, imageFile, thumbnailFile, utcTimestamp, interruptionFlag, Arrays.hashCode(imageSize));
    }
}
