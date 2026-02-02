package com.snuabar.mycomfy.main.model;

import com.snuabar.mycomfy.client.EnqueueResponse;

import org.json.JSONObject;

public class UpscaleReceivedMessageModel extends ReceivedMessageModel {

    public UpscaleReceivedMessageModel(EnqueueResponse response) {
        super(response);
    }

    public UpscaleReceivedMessageModel(JSONObject object) {
        super(object);
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
