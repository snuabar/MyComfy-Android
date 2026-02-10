package com.snuabar.mycomfy.main.data;

import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.os.SystemClock;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.snuabar.mycomfy.client.Parameters;
import com.snuabar.mycomfy.main.data.livedata.DeletionData;
import com.snuabar.mycomfy.utils.FilePicker;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class MainViewModel extends HttpBaseViewModel {

    private final static int MSG_MATCHING_DATA = 0;

    private FilePicker filePicker;
    private Executor dataMatchingExecutor;
    private String matchingKeywords;
    private final MutableLiveData<Boolean> deletionHasPressed;
    private final MutableLiveData<Boolean> associatedDeletionHasPressed;
    private final MutableLiveData<Boolean> deletionModeLiveData;
    private final MutableLiveData<Integer> selectedTabLiveData;
    private final MutableLiveData<Integer> clickedTabLiveData;
    private final MutableLiveData<DeletionData> deletionDataLiveData;
    private final MutableLiveData<Boolean> modelListChangeLiveData;
    private final MutableLiveData<Boolean> searchingModeLiveData;
    private final MutableLiveData<Set<String>> matchedIDsLiveData;

    public MainViewModel() {
        deletionHasPressed = new MutableLiveData<>();
        associatedDeletionHasPressed = new MutableLiveData<>();
        deletionModeLiveData = new MutableLiveData<>();
        selectedTabLiveData = new MutableLiveData<>();
        clickedTabLiveData = new MutableLiveData<>();
        deletionDataLiveData = new MutableLiveData<>();
        modelListChangeLiveData = new MutableLiveData<>();
        searchingModeLiveData = new MutableLiveData<>();
        matchedIDsLiveData = new MutableLiveData<>();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
    }

    public void setFilePicker(FilePicker filePicker) {
        this.filePicker = filePicker;
    }

    public FilePicker getFilePicker() {
        return filePicker;
    }

    private final Handler handler = new Handler(Looper.getMainLooper()) {
        @Override
        public void handleMessage(@NonNull Message msg) {
            if (msg.what == MSG_MATCHING_DATA) {
                matchingKeywords = msg.obj.toString();
            }
            super.handleMessage(msg);
        }
    };

    public void searchData(String keywords) {
        handler.removeMessages(0);
        Message message = Message.obtain(handler, 0);
        message.obj = keywords;
        handler.sendMessageDelayed(message, 300);

        if (dataMatchingExecutor != null) {
            return;
        }

        final String[] exchanging = new String[1];
        dataMatchingExecutor = Executors.newSingleThreadExecutor();
        dataMatchingExecutor.execute(() -> {
            while (isSearchingMode()) {
                SystemClock.sleep(200);
                if (Objects.equals(exchanging[0], matchingKeywords)) {
                    continue;
                }
                exchanging[0] = matchingKeywords;

                Set<String> ids = new HashSet<>();
                String[] keywordArray = matchingKeywords.split(" ");
                // 查找
                for (int i = 0; i < messageModels.size(); i++) {
                    AbstractMessageModel model = messageModels.get(i);
                    if (model == null) {
                        continue;
                    }

                    for (String k : keywordArray) {
                        if (TextUtils.isEmpty(k.trim())) {
                            break;
                        }
                        k = k.toLowerCase();
                        if (!TextUtils.isEmpty(model.getId()) && model.getId().contains(k)) {
                            ids.add(model.getId());
                            break;
                        }
                        Parameters p = model.getParameters();
                        if (p == null) {
                            break;
                        }
                        if (!TextUtils.isEmpty(p.getWorkflow()) && p.getWorkflow().contains(k)) {
                            ids.add(model.getId());
                            break;
                        }
                        if (!TextUtils.isEmpty(p.getModel()) && p.getModel().contains(k)) {
                            ids.add(model.getId());
                            break;
                        }
                        if (!TextUtils.isEmpty(p.getPrompt()) && p.getPrompt().contains(k)) {
                            ids.add(model.getId());
                            break;
                        }
                        if (!TextUtils.isEmpty(p.getSeed()) && p.getSeed().contains(k)) {
                            ids.add(model.getId());
                            break;
                        }
                        if (!model.isI2I()) {
                            String upscale = "x" + p.getUpscale_factor();
                            if (upscale.contains(k)) {
                                ids.add(model.getId());
                                break;
                            }
                            String resolution = p.getImg_width() + "*" + p.getImg_height();
                            if (resolution.contains(k)) {
                                ids.add(model.getId());
                                break;
                            }
                            resolution = p.getImg_width() + "x" + p.getImg_height();
                            if (resolution.contains(k)) {
                                ids.add(model.getId());
                                break;
                            }
                            int[] size = model.getImageSize();
                            resolution = size[0] + "*" + size[1];
                            if (resolution.contains(k)) {
                                ids.add(model.getId());
                                break;
                            }
                            resolution = size[0] + "x" + size[1];
                            if (resolution.contains(k)) {
                                ids.add(model.getId());
                                break;
                            }
                        }
                    }
                }

                matchedIDsLiveData.postValue(ids);
            }
            dataMatchingExecutor = null;
        });
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

    public LiveData<Integer> getClickedTabLiveData() {
        return clickedTabLiveData;
    }

    public void notifyTabClicked(int tab) {
        clickedTabLiveData.setValue(tab);
        clickedTabLiveData.setValue(-1);
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

    public LiveData<Boolean> getSearchingModeLiveData() {
        return searchingModeLiveData;
    }

    public void setSearchingMode(boolean searchingMode) {
        searchingModeLiveData.setValue(searchingMode);
        if (!searchingMode) {
            matchedIDsLiveData.postValue(null);
        }
    }

    public boolean isSearchingMode() {
        return Boolean.TRUE.equals(searchingModeLiveData.getValue());
    }

    public LiveData<Set<String>> getMatchedIDsLiveData() {
        return matchedIDsLiveData;
    }
}