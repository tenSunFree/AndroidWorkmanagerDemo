package com.home.androidworkmanagerdemo.viewmodel.workers

import android.content.Context
import android.text.TextUtils
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.home.androidworkmanagerdemo.model.MainModel
import java.io.File

class CleanupWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val applicationContext = applicationContext
        val title = inputData.getString(MainModel.keyNotificationTitle)
        val summary = inputData.getString(MainModel.keyNotificationSummary)
        val content = "Cleaning up old temporary files"
        WorkerUtils.makeStatusNotification(title!!, summary!!, content, applicationContext)
        WorkerUtils.sleep()
        try {
            val outputDirectory = File(applicationContext.filesDir, MainModel.outputPath)
            if (outputDirectory.exists()) {
                val entries = outputDirectory.listFiles()
                if (entries != null && entries.isNotEmpty())
                    for (entry in entries) {
                        val name = entry.name
                        if (!TextUtils.isEmpty(name) && name.endsWith(".png")) entry.delete()
                    }
            }
            return Result.success()
        } catch (exception: Exception) {
            val exceptionContent = "Error cleaning up, " +
                    "exception: " + exception.toString()
            WorkerUtils.makeStatusNotification(title, summary, exceptionContent, applicationContext)
            return Result.failure()
        }
    }
}
