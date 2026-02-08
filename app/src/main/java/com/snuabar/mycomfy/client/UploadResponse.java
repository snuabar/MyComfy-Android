package com.snuabar.mycomfy.client;

// 上传响应数据模型
public class UploadResponse {
    private String status;
    private String message;
    private String filename;
    private String file_url;
    private long file_size;
    private String uploaded_at;

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

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFile_url() {
        return file_url;
    }

    public void setFile_url(String file_url) {
        this.file_url = file_url;
    }

    public long getFile_size() {
        return file_size;
    }

    public void setFile_size(long file_size) {
        this.file_size = file_size;
    }

    public String getUploaded_at() {
        return uploaded_at;
    }

    public void setUploaded_at(String uploaded_at) {
        this.uploaded_at = uploaded_at;
    }
}