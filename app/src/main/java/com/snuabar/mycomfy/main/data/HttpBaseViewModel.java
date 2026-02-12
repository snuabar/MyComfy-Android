package com.snuabar.mycomfy.main.data;

import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.snuabar.mycomfy.client.EnqueueResponse;
import com.snuabar.mycomfy.client.FileSearchResponse;
import com.snuabar.mycomfy.client.JobResponse;
import com.snuabar.mycomfy.client.InterruptRequest;
import com.snuabar.mycomfy.client.ModelResponse;
import com.snuabar.mycomfy.client.QueueRequest;
import com.snuabar.mycomfy.client.RetrofitClient;
import com.snuabar.mycomfy.client.UploadResponse;
import com.snuabar.mycomfy.client.WorkflowsResponse;
import com.snuabar.mycomfy.common.Callbacks;
import com.snuabar.mycomfy.main.data.livedata.MessageState;
import com.snuabar.mycomfy.main.model.I2IReceivedMessageModel;
import com.snuabar.mycomfy.main.model.MessageModel;
import com.snuabar.mycomfy.main.model.ReceivedMessageModel;
import com.snuabar.mycomfy.main.model.ReceivedVideoMessageModel;
import com.snuabar.mycomfy.main.model.SentMessageModel;
import com.snuabar.mycomfy.main.model.UpscaleReceivedMessageModel;
import com.snuabar.mycomfy.main.model.UpscaleSentMessageModel;
import com.snuabar.mycomfy.utils.ImageHashCalculator;
import com.snuabar.mycomfy.utils.ThumbnailCacheManager;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class HttpBaseViewModel extends ViewModel {
    private static final String TAG = HttpBaseViewModel.class.getName();

    private final MutableLiveData<List<AbstractMessageModel>> messageModelsLiveData;
    protected List<AbstractMessageModel> messageModels;
    private final Executor messageModelsLoadingExecutor;
    private Executor promptCheckExecutor, requestExecutor;
    private final DataIO dataIO;
    private boolean isPromptCheckExecutorStop = false;
    private final MutableLiveData<MessageState> messageModelStateLiveData;
    private final MutableLiveData<Map<String, WorkflowsResponse.Workflow>> workflowsLiveData;
    private final MutableLiveData<List<String>> modelsLiveData;

    private final RetrofitClient retrofitClient;
    private boolean isWorkflowLoading = false;

    public HttpBaseViewModel() {
        messageModels = new ArrayList<>();
        messageModelsLiveData = new MutableLiveData<>();
        messageModelStateLiveData = new MutableLiveData<>();
        workflowsLiveData = new MutableLiveData<>();
        modelsLiveData = new MutableLiveData<>();

        // 初始化Retrofit客户端
        retrofitClient = RetrofitClient.getInstance();
        dataIO = DataIO.getInstance();
        messageModelsLoadingExecutor = Executors.newSingleThreadExecutor();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        ThumbnailCacheManager.Companion.getInstance().release();
        isPromptCheckExecutorStop = true;
        dataIO.close();
    }

    public LiveData<MessageState> getMessageModelStateLiveData() {
        return messageModelStateLiveData;
    }

    private void setMessageModelState(MessageState state) {
        messageModelStateLiveData.postValue(state);
    }

    public void reloadMessageModels() {
        messageModelsLoadingExecutor.execute(() -> {
            // 从储存读取列表
            List<AbstractMessageModel> list = dataIO.loadMessageModels();
            // 保护列表，让它线程安全
            messageModels = Collections.synchronizedList(list);
            // 更新id-index健值对
            refreshIdToIndexMap();
            // 设置共享列表
            dataIO.setSharedMessageModels(Collections.unmodifiableList(messageModels));
            // 发起列表更新
            messageModelsLiveData.postValue(messageModels);
        });
    }

    public void observeMessageModelsLiveData(@NonNull LifecycleOwner owner, @NonNull Observer<? super List<AbstractMessageModel>> observer) {
        messageModelsLiveData.observe(owner, observer);
    }

    public LiveData<Map<String, WorkflowsResponse.Workflow>> getWorkflowsLiveData() {
        return workflowsLiveData;
    }

    public LiveData<List<String>> getModelsLiveData() {
        return modelsLiveData;
    }

    private final Map<String, Integer> modelIdToIndexMap = new HashMap<>();

    @NonNull
    public List<AbstractMessageModel> getMessageModels() {
        return Collections.unmodifiableList(messageModels);
    }

    private void refreshIdToIndexMap() {
        modelIdToIndexMap.clear();
        for (int i = 0; i < messageModels.size(); i++) {
            modelIdToIndexMap.put(messageModels.get(i).getId(), i);
        }
    }

    public int getIndexWithId(String messageModelId) {
        Integer index = modelIdToIndexMap.get(messageModelId);
        return index == null ? -1 : index;
    }

    public int saveMessageModel(AbstractMessageModel model) {
        AbstractMessageModel newModel = dataIO.writeModel(model);
        if (newModel == null) {
            newModel = dataIO.writeModelFile(model);
        }
        // 更新缓存列表
        int index = getIndexWithId(model.getId());
        if (index == -1) {
            index = messageModels.size();
            messageModels.add(newModel);
            refreshIdToIndexMap();
        } else {
            messageModels.set(index, newModel);
        }
        return index;
    }

    public int deleteModel(String modelId) {
        int index = getIndexWithId(modelId);
        if (index >= 0) {
            return deleteModel(messageModels.get(index));
        }
        return -1;
    }


    public int deleteModel(AbstractMessageModel model) {
        if (dataIO.deleteModel(model) || dataIO.deleteModelFile(model)) {
            int index = getIndexWithId(model.getId());
            if (index >= 0) {
                messageModels.remove(index);
                // 更新缓存列表
                refreshIdToIndexMap();
            }
            return index;
        }
        return -1;
    }

    private void fetchImagesBeforeEnqueueing(QueueRequest request, SentMessageModel sentMessageModel) {
        Executor executor = getRequestExecutor();
        executor.execute(() -> {
            String[] images = null;
            for (int i = 0; i < request.getImageFiles().length; i++) {
                File imageFile = request.getImageFiles()[i];
                if (imageFile == null) {
                    continue;
                }

                String md5;
                try {
                    md5 = ImageHashCalculator.calculateMD5(imageFile);
                } catch (Exception e) {
                    sentMessageModel.setStatus(MessageModel.STATUS_FAILED, 999, "无法处理图像: " + e.getMessage());
                    int index = saveMessageModel(sentMessageModel);
                    setMessageModelState(MessageState.changed(index));
                    return;
                }

                Response<FileSearchResponse> response;
                try {
                    response = retrofitClient.getApiService().searchFile(md5).execute();
                } catch (IOException e) {
                    sentMessageModel.setStatus(MessageModel.STATUS_FAILED, 999, "图像校验失败: " + e.getMessage());
                    int index = saveMessageModel(sentMessageModel);
                    setMessageModelState(MessageState.changed(index));
                    return;
                }

                int responseCode = response.code();
                if (!response.isSuccessful() && responseCode != 404) {
                    sentMessageModel.setStatus(MessageModel.STATUS_FAILED, response.code(), response.message());
                    int index = saveMessageModel(sentMessageModel);
                    setMessageModelState(MessageState.changed(index));
                    return;
                }

                if (responseCode == 404) {
                    UploadResponse uploadResponse;
                    try {
                        uploadResponse = retrofitClient.uploadFileSync(imageFile, "");
                    } catch (IOException e) {
                        sentMessageModel.setStatus(MessageModel.STATUS_FAILED, 999, "图像上传失败: " + e.getMessage());
                        int index = saveMessageModel(sentMessageModel);
                        setMessageModelState(MessageState.changed(index));
                        return;
                    }

                    if (images == null) {
                        images = new String[request.getImageFiles().length];
                    }
                    images[i] = uploadResponse.getFilename();
                } else {
                    FileSearchResponse body = response.body();
                    if (body == null) {
                        sentMessageModel.setStatus(MessageModel.STATUS_FAILED, 1000, "未知错误");
                        int index = saveMessageModel(sentMessageModel);
                        setMessageModelState(MessageState.changed(index));
                        return;
                    }

                    if (images == null) {
                        images = new String[request.getImageFiles().length];
                    }
                    images[i] = body.getFile_name();
                }
            }

            if (images != null) {
                request.setImages(images);
            }

            enqueue(request, sentMessageModel);
        });
    }

    private boolean ignore409(EnqueueResponse enqueueResponse) {
        if (enqueueResponse.getCode() == 409) { // conflict 服务器端已存在相同任务
            // 查找本地列表中是否有相同任务
            return messageModels.stream().noneMatch(
                    model -> enqueueResponse.getPrompt_id().equals(model.getPromptId()));
        }
        return false;
    }

    public void enqueue(QueueRequest request, SentMessageModel sentMessageModel) {

        // 图像列表需要更新
        if (request.fetchingImagesIsNeeded()) {
            fetchImagesBeforeEnqueueing(request, sentMessageModel);
            return;
        }

        // 发送请求
        retrofitClient.getApiService().enqueue(request).enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<EnqueueResponse> call, @NonNull Response<EnqueueResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    EnqueueResponse enqueueResponse = response.body();
                    boolean ignore409 = false;
                    if (enqueueResponse.getCode() == 200 || ignore409(enqueueResponse)) { // OK
                        enqueueResponse.setCode(200);
                        enqueueResponse.setMessage("OK");
                        // 时间纠正（服务器时间不正确的情况下）
                        if (enqueueResponse.getUTCTimestamp() <= sentMessageModel.getUTCTimestamp()) {
                            enqueueResponse.setUtc_timestamp(String.valueOf(Clock.systemUTC().millis()));
                        }
                        // 保存响应对应，更新列表
                        ReceivedMessageModel receivedMessageModel;
                        if (sentMessageModel.isI2I()) {
                            receivedMessageModel = new I2IReceivedMessageModel(enqueueResponse);
                            receivedMessageModel.getParameters().setImageFiles(sentMessageModel.getParameters().getImageFiles());
                        } else if (sentMessageModel.isVideo()) {
                            receivedMessageModel = new ReceivedVideoMessageModel(enqueueResponse);
                        } else if (sentMessageModel instanceof UpscaleSentMessageModel) {
                            receivedMessageModel = new UpscaleReceivedMessageModel(enqueueResponse);
                        } else {
                            receivedMessageModel = new ReceivedMessageModel(enqueueResponse);
                        }
                        receivedMessageModel.setAssociatedSentModelId(sentMessageModel.getId());
                        int index = saveMessageModel(receivedMessageModel);
                        setMessageModelState(MessageState.added(index));
                        startStatusCheck();
                    } else if (enqueueResponse.getCode() == 409) { // conflict 已存在相同任务
                        int index = deleteModel(sentMessageModel);
                        setMessageModelState(MessageState.deleted(index));
                    }
                } else {
                    sentMessageModel.setStatus(MessageModel.STATUS_FAILED, response.code(), response.message());
                    int index = saveMessageModel(sentMessageModel);
                    setMessageModelState(MessageState.changed(index));
                }
            }

            @Override
            public void onFailure(@NonNull Call<EnqueueResponse> call, @NonNull Throwable t) {
                sentMessageModel.setStatus(MessageModel.STATUS_FAILED, 999, t.getMessage());
                int index = saveMessageModel(sentMessageModel);
                setMessageModelState(MessageState.changed(index));
            }
        });
    }

    public void startStatusCheck() {
        if (promptCheckExecutor != null) {
            return;
        }

        promptCheckExecutor = Executors.newSingleThreadExecutor();
        promptCheckExecutor.execute(() -> {
            while (!isPromptCheckExecutorStop) {
                SystemClock.sleep(3000);
                for (int i = 0; i < messageModels.size(); i++) {
                    AbstractMessageModel model = messageModels.get(i);
                    if (!(model instanceof ReceivedMessageModel) ||
                            TextUtils.isEmpty(model.getPromptId()) ||
                            model.isFinished()) {
                        continue;
                    }

                    if (model.getInterruptionFlag() && model.getCode() != MessageModel.CODE_CANCELED) {
                        try {
                            Response<ResponseBody> response = retrofitClient.getApiService().interrupt(new InterruptRequest(model.getPromptId())).execute();
                            if (response.isSuccessful()) {
                                model.setFinished(null, MessageModel.CODE_CANCELED, "已取消");
                                int index = saveMessageModel(model);
                                setMessageModelState(MessageState.changed(index));
                            }
                        } catch (Throwable t) {
                            Log.e(TAG, "“中止”请求失败.", t);
                        }
                        SystemClock.sleep(300);
                        continue;
                    }

                    try {
                        if (model.isFileExistsOnServer()) {
                            streamContentSync(model, null);
                        } else {
                            retrofit2.Response<JobResponse> response = retrofitClient.getApiService().getJobStatus(model.getPromptId()).execute();
                            if (response.isSuccessful()) {
                                JobResponse body = response.body();
                                if (body != null) {
                                    if (body.getCode() == 200) {
                                        streamContentSync(model, body.getUtc_timestamp());
                                    } else if (body.getCode() == 204
                                            || body.getCode() == 202
                                    ) {
                                        Log.i(TAG, model.getPromptId() + " is being processing.");
                                        if (model.setStatus(body.getStatus(), body.getCode(), body.getMessage())) {
                                            int index = saveMessageModel(model);
                                            setMessageModelState(MessageState.changed(index));
                                        }
                                    } else {
                                        Log.e(TAG, "Failed." + response.code() + ", " + response.message());
                                        model.setFinished(null, body.getCode(), body.getMessage());
                                        int index = saveMessageModel(model);
                                        setMessageModelState(MessageState.changed(index));
                                    }
                                } else {
                                    Log.e(TAG, "Failed." + response.code() + ", " + response.message());
                                    model.setFinished(null, 999, "unknown.");
                                    int index = saveMessageModel(model);
                                    setMessageModelState(MessageState.changed(index));
                                }
                            } else {
                                Log.e(TAG, "Failed." + response.code() + ", " + response.message());
                                model.setFinished(null, response.code(), response.message());
                                int index = saveMessageModel(model);
                                setMessageModelState(MessageState.changed(index));
                            }
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "Failed. ", t);
//                        model.setFinished(null, 1000, t.getMessage());
//                        int index = saveMessageModel(model);
//                        setMessageModelState(MessageModelState.changed(index));
                    }

                    SystemClock.sleep(100);
                }
            }
        });
    }

    private void downloadContentSync(AbstractMessageModel model, String endTime) throws IOException {
        Response<ResponseBody> response = retrofitClient.getApiService().download(model.getPromptId()).execute();
        if (response.isSuccessful()) {
            try (ResponseBody body = response.body()) {
                int idx = getIndexWithId(model.getId());
                long[] pgs = new long[2];
                File file = saveFile(body, model.isVideo(), (total, progress) -> {
                    Log.d(TAG, "下载文件: " + progress + "/" + total);
                    pgs[0] = total;
                    pgs[1] = progress;
                    setMessageModelState(MessageState.progress(idx, total, Math.max(total - 1, progress)));// total - 1 是让进度不走完
                });
                if (file != null) {
                    model.setFinished(file, response.code(), response.message(), endTime);
                    int index = saveMessageModel(model);
                    setMessageModelState(MessageState.progress(index, pgs[0], pgs[0]));// 走完进度
                }
            }
        }
    }

    private File saveFile(ResponseBody body, boolean isVideo, Callbacks.Callback2T<Long, Long> callback) {
        try {
            File file;
            if (isVideo) {
                file = DataIO.getInstance().newVideoFile();
            } else {
                file = DataIO.getInstance().newImageFile();
            }

            // 保存文件
            if (retrofitClient.downloadFile(body, file, callback)) {
                return file;
            }
        } catch (Throwable t) {
            Log.d(TAG, "保存文件失败: ", t);
        }
        return null;
    }

    public void streamContent(AbstractMessageModel model) {
        Executor executor = getRequestExecutor();
        executor.execute(() -> {
            try {
                streamContentSync(model, null);
            } catch (IOException e) {
                model.setStatus(MessageModel.STATUS_FAILED, MessageModel.CODE_DOWNLOADING_FAILED, "下载失败");
                int index = saveMessageModel(model);
                setMessageModelState(MessageState.changed(index));
            }
        });
    }

    // Android端调用流式接口
    public void streamContentSync(AbstractMessageModel model, String endTime) throws IOException {

        model.setStatus(MessageModel.STATUS_DOWNLOADING, 0, "");
        int index = saveMessageModel(model);
        setMessageModelState(MessageState.changed(index));

        Response<ResponseBody> response = retrofitClient.getApiService().stream(model.getPromptId()).execute();
        if (response.isSuccessful()) {
            try (ResponseBody body = response.body()) {
                int idx = getIndexWithId(model.getId());
                long[] pgs = new long[2];
                File file = saveStreamingFile(body, model.isVideo(), (total, progress) -> {
                    Log.d(TAG, "下载文件: " + progress + "/" + total);
                    pgs[0] = total;
                    pgs[1] = progress;
                    setMessageModelState(MessageState.progress(idx, total, Math.min(total - 1, progress)));// total - 1 是让进度不走完
                });
                if (file != null) {
                    if (model.getImageFile() != null && model.getImageFile().exists() && !model.getImageFile().delete()) {
                        Log.e(TAG, "streamContentSync > 文件删除失败。");
                    }
                    if (model.getThumbnailFile() != null && model.getThumbnailFile().exists() && !model.getThumbnailFile().delete()) {
                        Log.e(TAG, "streamContentSync > 文件（缩略图）删除失败。");
                    }
                    model.setFinished(file, response.code(), response.message(), endTime);
                    index = saveMessageModel(model);
                    setMessageModelState(MessageState.progress(index, pgs[0], pgs[0]));// 走完进度
                }
            }
        } else {
            model.setStatus(MessageModel.STATUS_FAILED, response.code(), response.message());
            index = saveMessageModel(model);
            setMessageModelState(MessageState.changed(index));
        }
    }

    private File saveStreamingFile(ResponseBody body, boolean isVideo, Callbacks.Callback2T<Long, Long> callback) {
        File file;
        if (isVideo) {
            file = DataIO.getInstance().newVideoFile();
        } else {
            file = DataIO.getInstance().newImageFile();
        }

        long contentLength = body.contentLength();

        if (callback != null) {
            callback.apply(contentLength, 0L);
        }

        try (InputStream inputStream = body.byteStream();
             FileOutputStream outputStream = new FileOutputStream(file)) {

            byte[] buffer = new byte[8192];
            int bytesRead;
            long totalBytes = 0;
            long lastTotalBytes = 0;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
                totalBytes += bytesRead;

                if (callback != null && (((float) contentLength / totalBytes) * 100) % 10 == 0) {
                    callback.apply(contentLength, totalBytes);
                    lastTotalBytes = totalBytes;
                }
            }

            if (callback != null && lastTotalBytes != contentLength) {
                callback.apply(contentLength, contentLength);
            }
            Log.i("Stream", "流式下载完成: " + totalBytes + " bytes");

        } catch (IOException e) {
            Log.e("Stream", "保存失败", e);
        }

        return file;
    }

    public void loadWorkflows() {
        if (isWorkflowLoading) {
            return;
        }
        isWorkflowLoading = true;
        // 发送请求
        retrofitClient.getApiService().loadWorkflow().enqueue(new Callback<>() {
            @Override
            public void onResponse(@NonNull Call<WorkflowsResponse> call, @NonNull Response<WorkflowsResponse> response) {
                isWorkflowLoading = false;
                if (response.isSuccessful() && response.body() != null) {
                    WorkflowsResponse workflowsResponse = response.body();
                    workflowsLiveData.postValue(workflowsResponse.getWorkflows());
                } else {
                    Log.e(TAG, "请求失败，状态码: " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<WorkflowsResponse> call, @NonNull Throwable t) {
                isWorkflowLoading = false;
                Log.e(TAG, "请求失败。" + t.getMessage(), t);
            }
        });
    }

    public void loadModels(@NonNull List<String> modelTypes) {
        Log.i(TAG, "发送请求: 加载模型列表。");
        for (String modelType : modelTypes) {
            // 发送请求
            retrofitClient.getApiService().loadModels(modelType).enqueue(new Callback<>() {
                @Override
                public void onResponse(@NonNull Call<ModelResponse> call, @NonNull Response<ModelResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        ModelResponse modelResponse = response.body();
                        modelsLiveData.postValue(modelResponse.getModels());
                    } else {
                        Log.e(TAG, "请求失败，状态码: " + response.code());
                    }
                }

                @Override
                public void onFailure(@NonNull Call<ModelResponse> call, @NonNull Throwable t) {
                    Log.e(TAG, "请求失败: " + t.getMessage());
                }
            });
        }
    }

    private Executor getRequestExecutor() {
        if (requestExecutor == null) {
            requestExecutor = Executors.newSingleThreadExecutor();
        }
        return requestExecutor;
    }
}
