package com.snuabar.mycomfy.main.model;

import android.util.Log;

import com.snuabar.mycomfy.client.Parameters;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Objects;

public class SentMessageModel extends MessageModel {
    private static final String TAG = SentMessageModel.class.getName();

    public SentMessageModel(Parameters parameters) {
        super();
        this.parameters = parameters;
    }

    public SentMessageModel(JSONObject object) {
        super();
        this.fromJson(object);
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }
}
