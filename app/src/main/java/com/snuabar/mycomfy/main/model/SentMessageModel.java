package com.snuabar.mycomfy.main.model;

import com.snuabar.mycomfy.client.ImageRequest;
import com.snuabar.mycomfy.client.Parameters;

public class SentMessageModel extends MessageModel {
    private final ImageRequest request;
    public SentMessageModel(ImageRequest request) {
        super();
        this.request = request;
        parameters = new Parameters().loadFromRequest(request);
    }
}
