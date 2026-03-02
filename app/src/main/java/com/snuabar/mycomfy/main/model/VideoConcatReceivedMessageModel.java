package com.snuabar.mycomfy.main.model;

import com.snuabar.mycomfy.client.EnqueueResponse;

import org.json.JSONObject;

public class VideoConcatReceivedMessageModel extends ReceivedVideoMessageModel {

    private static final String TAG = VideoConcatReceivedMessageModel.class.getName();

    public VideoConcatReceivedMessageModel(EnqueueResponse response) {
        super(response);
    }

    public VideoConcatReceivedMessageModel(JSONObject object) {
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
