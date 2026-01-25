package com.snuabar.mycomfy.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.snuabar.mycomfy.client.ServerStats;
import com.snuabar.mycomfy.utils.FilePicker;

import java.io.File;

public class MainViewModel extends ViewModel {
    // TODO: Implement the ViewModel
    private final MutableLiveData<File> imageFileLiveData;
    private final MutableLiveData<ServerStats> serverStatsLiveData;
    private FilePicker filePicker;

    public MainViewModel() {
        imageFileLiveData = new MutableLiveData<>();
        serverStatsLiveData = new MutableLiveData<>();
    }

    public LiveData<File> getImageFileData() {
        return imageFileLiveData;
    }

    public LiveData<ServerStats> getServerStatsLiveData() {
        return serverStatsLiveData;
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