package com.snuabar.mycomfy.main.model;

import com.snuabar.mycomfy.client.Parameters;

import org.json.JSONObject;

public class I2VSentMessageModel extends SentVideoMessageModel {

    private static final String TAG = I2VSentMessageModel.class.getName();

    public I2VSentMessageModel(Parameters parameters) {
        super(parameters);
    }

    public I2VSentMessageModel(JSONObject object) {
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
