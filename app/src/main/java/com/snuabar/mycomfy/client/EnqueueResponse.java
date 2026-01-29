package com.snuabar.mycomfy.client;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.Serializable;
import java.time.Clock;

public class EnqueueResponse implements Serializable {
    private String prompt_id;
    private int code;
    private String message;
    private String utc_timestamp;
    private Parameters parameters;
    private boolean file_exists;

    public String getPrompt_id() {
        return prompt_id;
    }

    public void setPrompt_id(String prompt_id) {
        this.prompt_id = prompt_id;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUtc_timestamp() {
        return utc_timestamp;
    }

    public void setUtc_timestamp(String utc_timestamp) {
        this.utc_timestamp = utc_timestamp;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public boolean isFile_exists() {
        return file_exists;
    }

    public void setFile_exists(boolean file_exists) {
        this.file_exists = file_exists;
    }

    public static EnqueueResponse create(int code, String message, Parameters parameters) {
        return new EnqueueResponse() {{
            this.setCode(code);
            this.setMessage(message);
            this.setParameters(parameters);
            this.setUtc_timestamp(String.valueOf(Clock.systemUTC().millis()));
        }};
    }

    public JSONObject toJson() {
        try {
            JSONObject jsonObject = new JSONObject();
            jsonObject.putOpt("prompt_id", prompt_id);
            jsonObject.putOpt("code", code);
            jsonObject.putOpt("message", message);
            jsonObject.putOpt("utc_timestamp", utc_timestamp);
            jsonObject.putOpt("parameters", parameters.toJson());
            jsonObject.putOpt("file_exists", file_exists);
            return jsonObject;
        } catch (JSONException e) {
            Log.e("ImageRequest", "toJson. exception thrown.", e);
        }
        return null;
    }

    public void loadJson(JSONObject jsonObject) {
        prompt_id = jsonObject.optString("prompt_id", null);
        code = jsonObject.optInt("code", 0);
        message = jsonObject.optString("message", null);
        utc_timestamp = jsonObject.optString("utc_timestamp", null);
        JSONObject pJson = jsonObject.optJSONObject("parameters");
        if (pJson != null) {
            parameters = new Parameters();
            parameters.loadJson(pJson);
        }
        file_exists = jsonObject.optBoolean("file_exists", false);
    }
}
