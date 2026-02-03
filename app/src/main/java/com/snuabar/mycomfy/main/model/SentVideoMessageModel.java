package com.snuabar.mycomfy.main.model;

import com.snuabar.mycomfy.client.Parameters;

import org.json.JSONObject;

public class SentVideoMessageModel extends SentMessageModel {

    public SentVideoMessageModel(Parameters parameters) {
        super(parameters);
    }

    public SentVideoMessageModel(JSONObject object) {
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
