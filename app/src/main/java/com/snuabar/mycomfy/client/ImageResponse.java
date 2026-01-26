package com.snuabar.mycomfy.client;

import java.util.List;

// 响应模型
public class ImageResponse {
    private String request_id;
    private String status;
    private String message;
    private String image_url;
    private List<String> image_paths;
    private Parameters parameters;
    private String created_at;
    private Float processing_time;

    // Getters and Setters
    public String getRequest_id() {
        return request_id;
    }

    public void setRequest_id(String request_id) {
        this.request_id = request_id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getImage_url() {
        return image_url;
    }

    public void setImage_url(String image_url) {
        this.image_url = image_url;
    }

    public List<String> getImage_paths() {
        return image_paths;
    }

    public void setImage_paths(List<String> image_paths) {
        this.image_paths = image_paths;
    }

    public Parameters getParameters() {
        return parameters;
    }

    public void setParameters(Parameters parameters) {
        this.parameters = parameters;
    }

    public String getCreated_at() {
        return created_at;
    }

    public void setCreated_at(String created_at) {
        this.created_at = created_at;
    }

    public Float getProcessing_time() {
        return processing_time;
    }

    public void setProcessing_time(Float processing_time) {
        this.processing_time = processing_time;
    }
}
