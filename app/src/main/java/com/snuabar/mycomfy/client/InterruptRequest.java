package com.snuabar.mycomfy.client;

public class InterruptRequest {
    private String prompt_id;

    public InterruptRequest(String prompt_id) {
        this.prompt_id = prompt_id;
    }

    public String getPrompt_id() {
        return prompt_id;
    }

    public void setPrompt_id(String prompt_id) {
        this.prompt_id = prompt_id;
    }
}
