package com.snuabar.mycomfy.main.model;

import com.snuabar.mycomfy.client.EnqueueResponse;

import org.json.JSONObject;

public class I2VReceivedMessageModel extends ReceivedVideoMessageModel {

    private static final String TAG = I2VReceivedMessageModel.class.getName();

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

    @Override
    public JSONObject toJson() {
        return super.toJson();
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
    }

}
