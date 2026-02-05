package com.snuabar.mycomfy.main.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.snuabar.mycomfy.utils.FilePicker;

import java.util.List;
import java.util.Map;

public class MainViewModel extends HttpBaseViewModel {

    private FilePicker filePicker;
    private final MutableLiveData<Boolean> deletionHasPressed;
    private final MutableLiveData<Boolean> deletionModeLiveData;
    private final MutableLiveData<Integer> selectedTabLiveData;
    private final MutableLiveData<Map<String, List<AbstractMessageModel>>> deletedModelsLiveData;

    public MainViewModel() {
        deletionHasPressed = new MutableLiveData<>();
        deletionModeLiveData = new MutableLiveData<>();
        selectedTabLiveData = new MutableLiveData<>();
        deletedModelsLiveData = new MutableLiveData<>();

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

}