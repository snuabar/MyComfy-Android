package com.snuabar.mycomfy.main.model;

import com.snuabar.mycomfy.client.EnqueueResponse;

import org.json.JSONObject;

public class I2IReceivedMessageModel extends ReceivedMessageModel {

    private static final String TAG = I2IReceivedMessageModel.class.getName();

    public I2IReceivedMessageModel(EnqueueResponse response) {
        super(response);
    }

    public I2IReceivedMessageModel(JSONObject object) {
        super(object);
    }

    @Override
    public boolean isI2I() {
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
