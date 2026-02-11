// RetrofitClient.java
package com.snuabar.mycomfy.client;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.snuabar.mycomfy.common.Callbacks;
import com.snuabar.mycomfy.common.FileType;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Cache;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String TAG = "RetrofitClient";
    private static RetrofitClient instance;
    private ApiService apiService = null;
    private final OkHttpClient okHttpClient;
    private String baseUrl = "https://192.168.1.100:8000"; // 修改为您的服务器IP

    private RetrofitClient(Context context) {
        // 创建日志拦截器
        HttpLoggingInterceptor loggingInterceptor = new HttpLoggingInterceptor();
        loggingInterceptor.setLevel(HttpLoggingInterceptor.Level.BODY);

        // 创建缓存目录
        File httpCacheDirectory = new File(context.getCacheDir(), "http_cache");
        int cacheSize = 10 * 1024 * 1024; // 10 MB
        Cache cache = new Cache(httpCacheDirectory, cacheSize);

        // 创建OkHttpClient
        okHttpClient = new OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(1800, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .addInterceptor(loggingInterceptor)
                .addInterceptor(new Interceptor() {
                    @NonNull
                    @Override
                    public Response intercept(@NonNull Chain chain) throws IOException {
                        Request original = chain.request();
                        Request request = original.newBuilder()
                                .header("User-Agent", "Android-AI-Image-Client")
                                .header("Accept", "application/json")
                                .method(original.method(), original.body())
                                .build();
                        return chain.proceed(request);
                    }
                })
                .cache(cache)
                .build();

        createRetrofit();
    }

    public static synchronized void init(Context context) {
        if (instance == null) {
            instance = new RetrofitClient(context);
        }
    }

    public static RetrofitClient getInstance() {
        return instance;
    }

    private void createRetrofit() {

        // 创建Retrofit实例
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        apiService = retrofit.create(ApiService.class);
    }

    public ApiService getApiService() {
        if (apiService == null) {
            createRetrofit();
        }
        return apiService;
    }

    public boolean setBaseUrl(String ip, String port) {
        String baseUrl;
        if (ip != null && ip.indexOf(':') != -1 && ip.indexOf(':') != ip.lastIndexOf(':')) {
            baseUrl = "http://[" + ip + "]:" + port;
        } else {
            baseUrl = "http://" + ip + ":" + port;
        }
        if (!this.baseUrl.equals(baseUrl)) {
            this.baseUrl = baseUrl;
            apiService = null;
            createRetrofit();
            return true;
        }
        return false;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public UploadResponse uploadFileSync(File file, String description) throws IOException {
        // 创建描述部分的RequestBody
        RequestBody descriptionBody = RequestBody.create(
                MediaType.parse("text/plain"),
                description != null ? description : ""
        );

        // 创建文件部分的RequestBody
        RequestBody fileBody = RequestBody.create(
                FileType.gguessMediaType(file),
                file
        );

        // 创建MultipartBody.Part
        MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                "file",
                file.getName(),
                fileBody
        );

        // 执行上传
        retrofit2.Response<UploadResponse> response = apiService.uploadFile(descriptionBody, filePart).execute();
        return response.body();
    }

    /**
     * 下载文件到本地
     */
    public boolean downloadFile(ResponseBody body, File file, Callbacks.Callback2T<Long, Long> callback) {
        try {
            InputStream inputStream = null;
            FileOutputStream outputStream = null;

            try {
                byte[] fileReader = new byte[1024 * 128];
                long fileLength = body.contentLength();
                if (callback != null) {
                    callback.apply(fileLength, 0L);
                }
                inputStream = body.byteStream();
                outputStream = new FileOutputStream(file);

                long readLength = 0;
                while (true) {
                    int read = inputStream.read(fileReader);
                    if (read == -1) {
                        break;
                    }
                    outputStream.write(fileReader, 0, read);
                    readLength += read;
                    if (callback != null) {
                        callback.apply(fileLength, readLength);
                    }
                }

                outputStream.flush();
                return true;
            } catch (IOException e) {
                Log.e(TAG, "下载文件失败", e);
                return false;
            } finally {
                if (inputStream != null) {
                    inputStream.close();
                }
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "下载文件失败", e);
            return false;
        }
    }
}
