package com.snuabar.mycomfy.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.snuabar.mycomfy.main.data.AbstractMessageModel;
import com.snuabar.mycomfy.utils.FilePicker;

import java.util.List;
import java.util.Map;

public class MainViewModel extends ViewModel {

    private FilePicker filePicker;
    private final MutableLiveData<Boolean> deletionHasPressed;
    private final MutableLiveData<Boolean> deletionModeLiveData;
    private final MutableLiveData<Map<String, List<AbstractMessageModel>>> deletedModelsLiveData;

    public MainViewModel() {
        deletionHasPressed = new MutableLiveData<>();
        deletionModeLiveData = new MutableLiveData<>();
        deletedModelsLiveData = new MutableLiveData<>();
    }

    public LiveData<Boolean> getDeletionModeLiveData() {
        return deletionModeLiveData;
    }

    public void changeDeletionMode(boolean deletionMode) {
        deletionModeLiveData.postValue(deletionMode);
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
        deletedModelsLiveData.setValue(deletedModels);
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
}