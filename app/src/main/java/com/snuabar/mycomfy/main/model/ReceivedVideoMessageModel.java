package com.snuabar.mycomfy.main.model;

import com.snuabar.mycomfy.client.EnqueueResponse;

import org.json.JSONObject;

public class ReceivedVideoMessageModel extends ReceivedMessageModel {

    private static final String TAG = ReceivedVideoMessageModel.class.getName();

    public ReceivedVideoMessageModel(EnqueueResponse response) {
        super(response);
    }

    public ReceivedVideoMessageModel(JSONObject object) {
        super(object);
    }

    @Override
    public boolean isVideo() {
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
