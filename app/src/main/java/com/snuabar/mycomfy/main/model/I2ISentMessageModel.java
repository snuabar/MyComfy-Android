package com.snuabar.mycomfy.main.model;

import com.snuabar.mycomfy.client.Parameters;

import org.json.JSONObject;

public class I2ISentMessageModel extends SentMessageModel {

    private static final String TAG = I2ISentMessageModel.class.getName();

    public I2ISentMessageModel(Parameters parameters) {
        super(parameters);
    }

    public I2ISentMessageModel(JSONObject object) {
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
