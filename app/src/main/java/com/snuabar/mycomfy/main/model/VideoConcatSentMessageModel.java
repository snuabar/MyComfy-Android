package com.snuabar.mycomfy.main.model;

import com.snuabar.mycomfy.client.Parameters;

import org.json.JSONObject;

public class VideoConcatSentMessageModel extends SentVideoMessageModel {

    private final static String TAG = VideoConcatSentMessageModel.class.getName();

    public VideoConcatSentMessageModel(Parameters parameters) {
        super(parameters);
    }

    public VideoConcatSentMessageModel(JSONObject object) {
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
