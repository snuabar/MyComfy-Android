package com.snuabar.mycomfy.client;

import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.time.Clock;
import java.util.Objects;

public class Parameters extends ImageRequest {
    private long timestamp;
    public Parameters() {
        super(null, null, null, 0, 0, 0, 0, 0.0, 0.0);
        timestamp = Clock.systemUTC().millis();
    }

    @Override
    public long getTimestamp() {
        return timestamp;
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
        if (jsonObject.has("timestamp")) {
            timestamp = jsonObject.optLong("timestamp", 0L);
        }
    }

    public Parameters loadFromRequest(ImageRequest request) {
        JSONObject object = request.toJson();
        loadJson(object);
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        Parameters that = (Parameters) o;
        return timestamp == that.timestamp;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), timestamp);
    }
}
