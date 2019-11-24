/*
 * Copyright (C) 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.home.androidworkmanagerdemo.viewmodel;

import android.app.Application;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.work.Constraints;
import androidx.work.Data;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkContinuation;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;

import com.home.androidworkmanagerdemo.model.MainModel;
import com.home.androidworkmanagerdemo.viewmodel.workers.BlurWorker;
import com.home.androidworkmanagerdemo.viewmodel.workers.CleanupWorker;
import com.home.androidworkmanagerdemo.viewmodel.workers.SaveImageToFileWorker;

import java.util.List;

public class MainViewModel extends AndroidViewModel {

    private WorkManager mWorkManager;
    private Uri mImageUri;
    private Uri mOutputUri;
    private LiveData<List<WorkInfo>> mSavedWorkInfo;

    public MainViewModel(@NonNull Application application) {
        super(application);
        mWorkManager = WorkManager.getInstance(application);
        // 通過Tag標籤跟踪任務的狀態
        mSavedWorkInfo = mWorkManager.getWorkInfosByTagLiveData(MainModel.tagWorkInfos);
    }

    /**
     * Create the WorkRequest to apply the blur and save the resulting image
     */
    public void applyBlur() {
        // cleanupWorkerRequest
        OneTimeWorkRequest.Builder cleanupWorkerRequest = new OneTimeWorkRequest.Builder(CleanupWorker.class);
        Data.Builder cleanupWorkerRequestData = new Data.Builder();
        cleanupWorkerRequestData.putString(MainModel.keyNotificationTitle, MainModel.notificationTitle);
        cleanupWorkerRequestData.putString(MainModel.keyNotificationSummary, MainModel.notificationSummary);
        // setInputData(): 傳遞一個Data對象, 它是key-value形式的對象
        cleanupWorkerRequest.setInputData(cleanupWorkerRequestData.build());
        // 創建WorkContinuation
        WorkContinuation continuation = mWorkManager
                // 創建任務序列, 並且指定唯一的名稱, 指定任務的替換策略
                // OneTimeWorkRequest: 任務只執行一遍
                .beginUniqueWork(MainModel.workSequenceName,
                        ExistingWorkPolicy.REPLACE,
                        cleanupWorkerRequest.build());
        // blurWorkerRequest
        OneTimeWorkRequest.Builder blurWorkerRequest = new OneTimeWorkRequest.Builder(BlurWorker.class);
        Data.Builder blurWorkerRequestData = new Data.Builder();
        blurWorkerRequestData.putString(MainModel.keyNotificationTitle, MainModel.notificationTitle);
        blurWorkerRequestData.putString(MainModel.keyNotificationSummary, MainModel.notificationSummary);
        if (mImageUri != null)
            blurWorkerRequestData.putString(MainModel.keyImageUri, mImageUri.toString());
        blurWorkerRequest.setInputData(blurWorkerRequestData.build());
        // 把blurWorkerRequest加入到執行隊列中
        continuation = continuation.then(blurWorkerRequest.build());
        // Constraints: 指定任務執行的限制條件
        Constraints constraints = new Constraints.Builder()
                .setRequiresCharging(true) // 是否在充電狀態下執行任務
                .build();
        // SaveImageToFileWorker
        OneTimeWorkRequest save = new OneTimeWorkRequest.Builder(SaveImageToFileWorker.class)
                .setConstraints(constraints) // 設置指定任務執行的限制條件
                .addTag(MainModel.tagWorkInfos) // 為任務設置Tag標籤
                .build();
        // 把SaveImageToFileWorker加入到執行隊列中
        continuation = continuation.then(save);
        // 開始工作
        continuation.enqueue();
    }

    /**
     * Cancel work using the work's unique name
     */
    public void cancelWork() {
        mWorkManager.cancelUniqueWork(MainModel.tagWorkInfos);
    }

    private Uri uriOrNull(String uriString) {
        if (!TextUtils.isEmpty(uriString)) return Uri.parse(uriString);
        return null;
    }

    public void setImageUri(String uri) {
        mImageUri = uriOrNull(uri);
    }

    public void setOutputUri(String outputImageUri) {
        mOutputUri = uriOrNull(outputImageUri);
    }

    public Uri getOutputUri() {
        return mOutputUri;
    }

    public LiveData<List<WorkInfo>> getOutputWorkInfo() {
        return mSavedWorkInfo;
    }

    public String getKeyImageUri() {
        return MainModel.keyImageUri;
    }
}