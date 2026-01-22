package com.snuabar.mycomfy.client;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    /**
     * 生成AI图像
     */
    @POST("/api/generate")
    Call<ImageResponse> generateImage(@Body ImageRequest request);

    /**
     * 获取生成的图像
     */
    @GET("/api/images/{request_id}")
    @Streaming
    Call<ResponseBody> downloadImage(@Path("request_id") String requestId);

    /**
     * 流式获取图像（适用于大图像）
     */
    @GET("/api/images/{request_id}/stream")
    @Streaming
    Call<ResponseBody> streamImage(@Path("request_id") String requestId);

    /**
     * 检查生成状态
     */
    @GET("/api/status/{request_id}")
    Call<StatusResponse> checkStatus(@Path("request_id") String requestId);

    /**
     * 获取服务器统计信息
     */
    @GET("/api/stats")
    Call<ServerStats> getServerStats();
}

