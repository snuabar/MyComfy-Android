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
import com.snuabar.mycomfy.main.data.livedata.MessageModelState;
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
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
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
    private List<AbstractMessageModel> messageModels;
    private final Executor messageModelsLoadingExecutor;
    private Executor promptCheckExecutor, requestExecutor;
    private final DataIO dataIO;
    private boolean isPromptCheckExecutorStop = false;
    private final MutableLiveData<MessageModelState> messageModelStateLiveData;
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

    public LiveData<MessageModelState> getMessageModelStateLiveData() {
        return messageModelStateLiveData;
    }

    private void setMessageModelState(MessageModelState state) {
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

    public int deleteModelFile(String modelId) {
        int index = getIndexWithId(modelId);
        if (index >= 0) {
            return deleteModelFile(messageModels.get(index));
        }
        return -1;
    }


    public int deleteModelFile(AbstractMessageModel model) {
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
                    setMessageModelState(MessageModelState.changed(index));
                    break;
                }

                Response<FileSearchResponse> response;
                try {
                    response = retrofitClient.getApiService().searchFile(md5).execute();
                } catch (IOException e) {
                    sentMessageModel.setStatus(MessageModel.STATUS_FAILED, 999, "图像校验失败: " + e.getMessage());
                    int index = saveMessageModel(sentMessageModel);
                    setMessageModelState(MessageModelState.changed(index));
                    break;
                }

                int responseCode = response.code();
                if (!response.isSuccessful() && responseCode != 404) {
                    sentMessageModel.setStatus(MessageModel.STATUS_FAILED, response.code(), response.message());
                    int index = saveMessageModel(sentMessageModel);
                    setMessageModelState(MessageModelState.changed(index));
                    break;
                }

                if (responseCode == 404) {
                    UploadResponse uploadResponse;
                    try {
                        uploadResponse = retrofitClient.uploadFileSync(imageFile, "");
                    } catch (IOException e) {
                        sentMessageModel.setStatus(MessageModel.STATUS_FAILED, 999, "图像上传失败: " + e.getMessage());
                        int index = saveMessageModel(sentMessageModel);
                        setMessageModelState(MessageModelState.changed(index));
                        break;
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
                        setMessageModelState(MessageModelState.changed(index));
                        break;
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
                    if (enqueueResponse.getCode() == 200) { // OK
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
                        }else if (sentMessageModel instanceof UpscaleSentMessageModel) {
                            receivedMessageModel = new UpscaleReceivedMessageModel(enqueueResponse);
                        } else {
                            receivedMessageModel = new ReceivedMessageModel(enqueueResponse);
                        }
                        receivedMessageModel.setAssociatedSentModelId(sentMessageModel.getId());
                        int index = saveMessageModel(receivedMessageModel);
//                        messageAdapter.notifyItemAdded(index);
                        setMessageModelState(MessageModelState.added(index));
                        startStatusCheck();
                    } else if (enqueueResponse.getCode() == 409) { // conflict 已存在相同任务
                        int index = deleteModelFile(sentMessageModel);
//                        messageAdapter.notifyItemDeleted(index);
                        setMessageModelState(MessageModelState.deleted(index));
                    }
                } else {
                    sentMessageModel.setStatus(MessageModel.STATUS_FAILED, response.code(), response.message());
                    int index = saveMessageModel(sentMessageModel);
//                    messageAdapter.notifyItemChanged(index);
                    setMessageModelState(MessageModelState.changed(index));
                }
            }

            @Override
            public void onFailure(@NonNull Call<EnqueueResponse> call, @NonNull Throwable t) {
                sentMessageModel.setStatus(MessageModel.STATUS_FAILED, 999, t.getMessage());
                int index = saveMessageModel(sentMessageModel);
//                messageAdapter.notifyItemChanged(index);
                setMessageModelState(MessageModelState.changed(index));
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

                for (int i = 0; i < messageModels.size(); i++) {
                    AbstractMessageModel model = messageModels.get(i);
                    if (!(model instanceof ReceivedMessageModel) ||
                            TextUtils.isEmpty(model.getPromptId()) ||
                            model.isFinished()) {
                        continue;
                    }

                    if (model.getInterruptionFlag()) {
                        try {
                            Response<ResponseBody> response = retrofitClient.getApiService().interrupt(new InterruptRequest(model.getPromptId())).execute();
                            if (response.isSuccessful()) {
                                model.setFinished(null, 998, "已取消");
                                int index = saveMessageModel(model);
//                                requireActivity().runOnUiThread(() -> messageAdapter.notifyItemChanged(index));
                                setMessageModelState(MessageModelState.changed(index));
                            }
                        } catch (IOException e) {
                            log("请求失败: " + e.getMessage(), true);
                        }
                        SystemClock.sleep(300);
                        continue;
                    }

                    try {
                        if (model.isFileExistsOnServer()) {
                            downloadContentSync(model, null);
                        } else {
                            retrofit2.Response<JobResponse> response = retrofitClient.getApiService().getJobStatus(model.getPromptId()).execute();
                            if (response.isSuccessful()) {
                                JobResponse body = response.body();
                                if (body != null) {
                                    if (body.getCode() == 200) {
                                        downloadContentSync(model, body.getUtc_timestamp());
                                    } else if (body.getCode() == 204
                                            || body.getCode() == 202
                                    ) {
                                        Log.i(TAG, model.getPromptId() + " is being processing.");
                                        if (model.setStatus(body.getStatus(), body.getCode(), body.getMessage())) {
                                            int index = saveMessageModel(model);
//                                            requireActivity().runOnUiThread(() -> messageAdapter.notifyItemChanged(index));
                                            setMessageModelState(MessageModelState.changed(index));
                                        }
                                    } else {
                                        Log.e(TAG, "Failed." + response.code() + ", " + response.message());
                                        model.setFinished(null, body.getCode(), body.getMessage());
                                        int index = saveMessageModel(model);
//                                        requireActivity().runOnUiThread(() -> messageAdapter.notifyItemChanged(index));
                                        setMessageModelState(MessageModelState.changed(index));
                                    }
                                } else {
                                    Log.e(TAG, "Failed." + response.code() + ", " + response.message());
                                    //                                messageAdapter.setFinished(promptIdInner, null, 999, "unknown.");
                                    model.setFinished(null, 999, "unknown.");
                                    int index = saveMessageModel(model);
//                                    requireActivity().runOnUiThread(() -> messageAdapter.notifyItemChanged(index));
                                    setMessageModelState(MessageModelState.changed(index));
                                }
                            } else {
                                Log.e(TAG, "Failed." + response.code() + ", " + response.message());
                                //                            messageAdapter.setFinished(promptIdInner, null, response.code(), response.message());
                                model.setFinished(null, response.code(), response.message());
                                int index = saveMessageModel(model);
//                                requireActivity().runOnUiThread(() -> messageAdapter.notifyItemChanged(index));
                                setMessageModelState(MessageModelState.changed(index));
                            }
                        }
                    } catch (Throwable t) {
                        Log.e(TAG, "failed to execute downloadImage(" + model.getPromptId() + ")", t);
                        //                        messageAdapter.setFinished(promptIdInner, null, 1000, t.getMessage());
                        model.setFinished(null, 1000, t.getMessage());
                        int index = saveMessageModel(model);
//                        requireActivity().runOnUiThread(() -> messageAdapter.notifyItemChanged(index));
                        setMessageModelState(MessageModelState.changed(index));
                    }

                    SystemClock.sleep(100);
                }
                SystemClock.sleep(5000);
            }
        });
    }

    private void downloadContentSync(AbstractMessageModel model, String endTime) throws IOException {
        Response<ResponseBody> response = retrofitClient.getApiService().download(model.getPromptId()).execute();
        if (response.isSuccessful()) {
            try (ResponseBody body = response.body()) {
                File file = saveFile(body, model.isVideo(), (total, progress) -> {
                    log("下载文件: " + progress + "/" + total, false);
                });
                if (file != null) {
                    model.setFinished(file, response.code(), response.message(), endTime);
                    int index = saveMessageModel(model);
//                    requireActivity().runOnUiThread(() -> messageAdapter.notifyItemChanged(index));
                    setMessageModelState(MessageModelState.changed(index));
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
        } catch (Exception e) {
            log("保存文件失败: " + e.getMessage(), true);
        }
        return null;
    }

    private void log(String message, boolean isErr) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String newLog = "[" + timestamp + "] " + message;
        if (isErr) {
            Log.e(TAG, newLog);
        } else {
            Log.i(TAG, newLog);
        }
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
