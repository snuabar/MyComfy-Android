package com.snuabar.mycomfy.client;

public class ServerStats {
    private int total_images;
    private int total_requests;
    private float storage_used_mb;
    private String server_uptime;
    private String image_directory;

    // Getters and Setters
    public int getTotal_images() { return total_images; }
    public void setTotal_images(int total_images) { this.total_images = total_images; }

    public int getTotal_requests() { return total_requests; }
    public void setTotal_requests(int total_requests) { this.total_requests = total_requests; }

    public float getStorage_used_mb() { return storage_used_mb; }
    public void setStorage_used_mb(float storage_used_mb) { this.storage_used_mb = storage_used_mb; }

    public String getServer_uptime() { return server_uptime; }
    public void setServer_uptime(String server_uptime) { this.server_uptime = server_uptime; }

    public String getImage_directory() { return image_directory; }
    public void setImage_directory(String image_directory) { this.image_directory = image_directory; }
}
