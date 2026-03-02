package com.snuabar.mycomfy.client;

public class ClientRequest {
    private String client_id;

    public ClientRequest(String client_id) {
        this.client_id = client_id;
    }

    public String getClient_id() {
        return client_id;
    }

    public void setClient_id(String client_id) {
        this.client_id = client_id;
    }
}
