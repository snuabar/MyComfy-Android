package com.snuabar.mycomfy.client;

import okhttp3.MultipartBody;
import okhttp3.RequestBody;
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
     * 添加至生成队列
     */
    @POST("/api/enqueue")
    Call<EnqueueResponse> enqueue(@Body QueueRequest request);

    /**
     * 获取生成状态
     */
    @GET("/api/jobs/{prompt_id}")
    Call<JobResponse> getJobStatus(@Path("prompt_id") String promptId);

    /**
     * 获取生成状态
     */
    @GET("/api/search/{file_hash}")
    Call<FileSearchResponse> searchFile(@Path("file_hash") String fileHash);

    /**
     * 上传文件
     */
    @POST("/api/upload")
    @Multipart
    Call<UploadResponse> uploadFile(
            @Part("description") RequestBody description,
            @Part MultipartBody.Part file
    );

    /**
     * 下载
     */
    @GET("/api/download/{prompt_id}")
    @Streaming
    Call<ResponseBody> download(@Path("prompt_id") String promptId);

    /**
     * 流式获取图像（适用于大图像）
     */
    @GET("/api/images/{request_id}/stream")
    @Streaming
    Call<ResponseBody> streamImage(@Path("request_id") String requestId);

    /**
     * 获取服务器统计信息
     */
    @GET("/api/stats")
    Call<ServerStats> getServerStats();

    @POST("/api/interrupt")
    Call<ResponseBody> interrupt(@Body InterruptRequest request);
}

