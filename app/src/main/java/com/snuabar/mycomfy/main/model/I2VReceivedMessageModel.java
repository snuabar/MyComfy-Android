package com.snuabar.mycomfy.main.model;

import android.util.Log;

import com.snuabar.mycomfy.client.EnqueueResponse;

import org.json.JSONException;
import org.json.JSONObject;

public class I2VReceivedMessageModel extends ReceivedVideoMessageModel {

    private static final String TAG = I2VReceivedMessageModel.class.getName();
    private ContinuedI2VSentMessageModel continuedI2VSentMessageModel;

    public I2VReceivedMessageModel(EnqueueResponse response) {
        super(response);
    }

    public I2VReceivedMessageModel(JSONObject object) {
        super(object);
    }

    @Override
    public boolean isI2V() {
        return true;
    }

    public void setContinuedI2VSentMessageModel(ContinuedI2VSentMessageModel continuedI2VSentMessageModel) {
        this.continuedI2VSentMessageModel = continuedI2VSentMessageModel;
    }

    public ContinuedI2VSentMessageModel getContinuedI2VSentMessageModel() {
        return continuedI2VSentMessageModel;
    }

    @Override
    public JSONObject toJson() {
        JSONObject jsonObject = super.toJson();
        if (continuedI2VSentMessageModel != null) {
            try {
                jsonObject.putOpt("continuedI2VSentMessageModel", continuedI2VSentMessageModel.toJson());
            } catch (JSONException e) {
                Log.e(TAG, "toJson. exception thrown.", e);
            }
        }
        return jsonObject;
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
        if (jsonObject.has("continuedI2VSentMessageModel")) {
            continuedI2VSentMessageModel = new ContinuedI2VSentMessageModel(jsonObject.optJSONObject("continuedI2VSentMessageModel"));
        }
    }

}
