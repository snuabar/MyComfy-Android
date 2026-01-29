package com.snuabar.mycomfy.main.model;

import android.util.Log;

import com.snuabar.mycomfy.client.ImageRequest;
import com.snuabar.mycomfy.client.Parameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class SentMessageModel extends MessageModel {
    private static final String TAG = SentMessageModel.class.getName();

    protected String failureMessage = null;

    public SentMessageModel(ImageRequest request) {
        super();
        parameters = new Parameters().loadFromRequest(request);
    }

    public SentMessageModel(JSONObject object) {
        super();
        loadJson(object);
    }


    @Override
    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    @Override
    public String getFailureMessage() {
        return failureMessage;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        if (failureMessage != null) {
            try {
                jsonObject.putOpt("failureMessage", failureMessage);
            } catch (JSONException e) {
                Log.e(TAG, "toJson. Failed to execute putOpt.", e);
            }
        }
        return jsonObject;
    }

    @Override
    public void loadJson(JSONObject jsonObject) {
        super.loadJson(jsonObject);
        failureMessage = jsonObject.optString("failureMessage", null);
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        SentMessageModel that = (SentMessageModel) o;
        return Objects.equals(failureMessage, that.failureMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), failureMessage);
    }
}
