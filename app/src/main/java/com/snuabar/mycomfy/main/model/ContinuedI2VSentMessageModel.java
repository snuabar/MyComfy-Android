package com.snuabar.mycomfy.main.model;

import com.snuabar.mycomfy.client.Parameters;

import org.json.JSONObject;

public class ContinuedI2VSentMessageModel extends I2VSentMessageModel {

    private static final String TAG = ContinuedI2VSentMessageModel.class.getName();

    public ContinuedI2VSentMessageModel(Parameters parameters) {
        super(parameters);
    }

    public ContinuedI2VSentMessageModel(JSONObject jsonObject) {
        super(jsonObject);
    }

    @Override
    public JSONObject toJson() {
        return super.toJson();
    }

    @Override
    public void fromJson(JSONObject jsonObject) {
        super.fromJson(jsonObject);
    }

    public boolean downloadingImagesIsNeeded() {
        return getParameters().downloadingImagesIsNeeded();
    }
}
