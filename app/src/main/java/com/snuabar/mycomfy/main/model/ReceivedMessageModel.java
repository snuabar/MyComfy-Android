package com.snuabar.mycomfy.main.model;

import com.snuabar.mycomfy.client.EnqueueResponse;

public class ReceivedMessageModel extends MessageModel {

    private final EnqueueResponse response;

    public ReceivedMessageModel(EnqueueResponse response) {
        super();
        this.response = response;
        this.parameters = this.response.getParameters();
    }

    public EnqueueResponse getResponse() {
        return response;
    }
}
