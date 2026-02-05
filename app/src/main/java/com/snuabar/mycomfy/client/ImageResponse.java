package com.snuabar.mycomfy.client;

// 响应模型
public class ImageResponse {
    private String prompt_id;
    private int code;
    private String message;
    private String status;
    private String media_type;
    private String filename;
    private String utc_timestamp;

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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMedia_type() {
        return media_type;
    }

    public void setMedia_type(String media_type) {
        this.media_type = media_type;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getUtc_timestamp() {
        return utc_timestamp;
    }

    public void setUtc_timestamp(String utc_timestamp) {
        this.utc_timestamp = utc_timestamp;
    }
}
