package com.snuabar.mycomfy.client;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

public class Parameters extends ImageRequest {
    private long timestamp;
    public Parameters() {
        super(null, null, null, 0, 0, 0, 0, 0.0, 0.0);
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        if (jsonObject != null) {
            try {
                jsonObject.putOpt("timestamp", timestamp);
            } catch (JSONException e) {
                Log.e("Parameters", "toJson. exception thrown.", e);
            }
        }
        return jsonObject;
    }

    @Override
    public void loadJson(JSONObject jsonObject) {
        super.loadJson(jsonObject);
        setTimestamp(jsonObject.optLong("timestamp", 0));
    }
}
