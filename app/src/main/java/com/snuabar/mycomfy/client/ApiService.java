package com.snuabar.mycomfy.client;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {

    /**
     * 加载可用工作流
     */
    @GET("/api/workflows")
    Call<WorkflowsResponse> loadWorkflow();

    /**
     * 加载可用模型
     */
    @GET("/api/models/{model_type}")
    Call<ModelResponse> loadModels(@Path("model_type") String modelType);

    /**
     * 生成AI图像
     */
    @POST("/api/generate")
    Call<ImageResponse> generateImage(@Body ImageRequest request);

    /**
     * 添加至生成队列
     */
    @POST("/api/enqueue")
    Call<EnqueueResponse> enqueue(@Body ImageRequest request);

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

    /**
     * 获取历史记录
     */
    @GET("/api/history/{prompt_id}")
    Call<ResponseBody> getHistory(@Path("prompt_id") String promptId);


}

