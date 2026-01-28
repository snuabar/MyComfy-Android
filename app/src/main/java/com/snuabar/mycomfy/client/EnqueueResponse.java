package com.snuabar.mycomfy.client;

import java.time.Clock;

public class EnqueueResponse {
    private String prompt_id;
    private int code;
    private String message;
    private String utc_timestamp;
    private Parameters parameters;

    public String getPrompt_id() {
        return prompt_id;
    }

    public void setPrompt_id(String prompt_id) {
        this.prompt_id = prompt_id;
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getUtc_timestamp() {
        return utc_timestamp;
    }

    public void setUtc_timestamp(String utc_timestamp) {
        this.utc_timestamp = utc_timestamp;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public static EnqueueResponse create(int code, String message, Parameters parameters) {
        return new EnqueueResponse(){{
            this.setCode(code);
            this.setMessage(message);
            this.setParameters(parameters);
            this.setUtc_timestamp(String.valueOf(Clock.systemUTC().millis()));
        }};
    }
}
