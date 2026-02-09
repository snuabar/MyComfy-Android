package com.snuabar.mycomfy.main.data;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.snuabar.mycomfy.main.data.livedata.DeletionData;
import com.snuabar.mycomfy.utils.FilePicker;

public class MainViewModel extends HttpBaseViewModel {

    private FilePicker filePicker;
    private final MutableLiveData<Boolean> deletionHasPressed;
    private final MutableLiveData<Boolean> associatedDeletionHasPressed;
    private final MutableLiveData<Boolean> deletionModeLiveData;
    private final MutableLiveData<Integer> selectedTabLiveData;
    private final MutableLiveData<DeletionData> deletionDataLiveData;
    private final MutableLiveData<Boolean> modelListChangeLiveData;

    public MainViewModel() {
        deletionHasPressed = new MutableLiveData<>();
        associatedDeletionHasPressed = new MutableLiveData<>();
        deletionModeLiveData = new MutableLiveData<>();
        selectedTabLiveData = new MutableLiveData<>();
        deletionDataLiveData = new MutableLiveData<>();
        modelListChangeLiveData = new MutableLiveData<>();
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

    public LiveData<Boolean> getAssociatedDeletionHasPressedLiveData() {
        return associatedDeletionHasPressed;
    }

    public void changeAssociatedDeletionHasPressed(boolean pressed) {
        associatedDeletionHasPressed.setValue(pressed);
    }

    public LiveData<DeletionData> getDeletionDataLiveData() {
        return deletionDataLiveData;
    }

    public void changeDeletionData(DeletionData deletionData) {
        deletionDataLiveData.postValue(deletionData);
    }


    public LiveData<Boolean> getModelListChangeLiveData() {
        return modelListChangeLiveData;
    }

    public void setModelListChange(boolean changed) {
        modelListChangeLiveData.setValue(changed);
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