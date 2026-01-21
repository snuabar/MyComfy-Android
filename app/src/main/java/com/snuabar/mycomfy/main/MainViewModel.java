package com.snuabar.mycomfy.main;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.snuabar.mycomfy.client.ServerStats;

import java.io.File;

public class MainViewModel extends ViewModel {
    // TODO: Implement the ViewModel
    private final MutableLiveData<File> imageFileLiveData;
    private final MutableLiveData<ServerStats> serverStatsLiveData;

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

    @Override
    protected void onCleared() {
        super.onCleared();
    }
}