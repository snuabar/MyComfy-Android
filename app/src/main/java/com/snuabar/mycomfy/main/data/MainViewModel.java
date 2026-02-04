package com.snuabar.mycomfy.main.data;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.snuabar.mycomfy.utils.FilePicker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainViewModel extends ViewModel {

    private FilePicker filePicker;
    private final MutableLiveData<Boolean> deletionHasPressed;
    private final MutableLiveData<Boolean> deletionModeLiveData;
    private final MutableLiveData<Integer> selectedTabLiveData;
    private final MutableLiveData<Map<String, List<AbstractMessageModel>>> deletedModelsLiveData;
    private final MutableLiveData<List<AbstractMessageModel>> messageModelsLiveData;
    private List<AbstractMessageModel> messageModels;
    private final Executor messageModelsLoadingExecutor;
    private final DataIO dataIO;

    public MainViewModel() {
        deletionHasPressed = new MutableLiveData<>();
        deletionModeLiveData = new MutableLiveData<>();
        selectedTabLiveData = new MutableLiveData<>();
        deletedModelsLiveData = new MutableLiveData<>();
        messageModels = new ArrayList<>();
        messageModelsLiveData = new MutableLiveData<>();

        dataIO = DataIO.getInstance();
        messageModelsLoadingExecutor = Executors.newSingleThreadExecutor();
    }

    public void reloadMessageModels(Context context) {
        messageModelsLoadingExecutor.execute(() -> {
            // 从储存读取列表
            List<AbstractMessageModel> list = dataIO.loadMessageModels(context);
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

    public LiveData<Boolean> getDeletionModeLiveData() {
        return deletionModeLiveData;
    }

    public void changeDeletionMode(boolean deletionMode) {
        deletionModeLiveData.postValue(deletionMode);
    }

    public LiveData<Integer> getSelectedTabLiveData() {
        return selectedTabLiveData;
    }

    public void changeSelectedTab(int tab) {
        selectedTabLiveData.setValue(tab);
    }

    public LiveData<Boolean> getDeletionHasPressLiveData() {
        return deletionHasPressed;
    }

    public void changeDeletionHasPressed(boolean pressed) {
        deletionHasPressed.setValue(pressed);
    }

    public LiveData<Map<String, List<AbstractMessageModel>>> getDeletedModelsLiveData() {
        return deletedModelsLiveData;
    }

    public void changeDeletedModels(Map<String, List<AbstractMessageModel>> deletedModels) {
        deletedModelsLiveData.postValue(deletedModels);
    }

    public void setFilePicker(FilePicker filePicker) {
        this.filePicker = filePicker;
    }

    public FilePicker getFilePicker() {
        return filePicker;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
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

    public int saveMessageModel(Context context, AbstractMessageModel model) {
        AbstractMessageModel newModel = dataIO.writeModelFile(context, model);
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

    public int deleteModelFile(Context context, AbstractMessageModel model) {
        if (dataIO.deleteModelFile(context, model)) {
            // 更新缓存列表
            int index = getIndexWithId(model.getId());
            if (index >= 0) {
                messageModels.remove(index);
                refreshIdToIndexMap();
            }
            return index;
        }
        return -1;
    }
}