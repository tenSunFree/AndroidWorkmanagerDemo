package com.home.androidworkmanagerdemo.viewmodel.workers

import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.text.TextUtils
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.home.androidworkmanagerdemo.model.MainModel
import java.io.FileNotFoundException

class BlurWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    override fun doWork(): Result {
        val applicationContext = applicationContext
        val title = inputData.getString(MainModel.keyNotificationTitle)
        val summary = inputData.getString(MainModel.keyNotificationSummary)
        val imageUri = inputData.getString(MainModel.keyImageUri)
        val content = "Blurring image"
        WorkerUtils.makeStatusNotification(title!!, summary!!, content, applicationContext)
        WorkerUtils.sleep()
        try {
            if (TextUtils.isEmpty(imageUri)) throw IllegalArgumentException("Invalid input uri")
            val resolver = applicationContext.contentResolver
            // Create a bitmap
            val bitmap = BitmapFactory.decodeStream(resolver.openInputStream(Uri.parse(imageUri)))
            // Blur the bitmap
            val output = WorkerUtils.blurBitmap(bitmap, applicationContext)
            // Write bitmap to a temp file
            val outputUri = WorkerUtils.writeBitmapToFile(applicationContext, output)
            // Return the output for the temp file
            val outputData =
                Data.Builder().putString(MainModel.keyImageUri, outputUri.toString()).build()
            return Result.success(outputData)
        } catch (fileNotFoundException: FileNotFoundException) {
            val fileNotFoundExceptionContent = "Failed to decode input stream, " +
                    "fileNotFoundException: " + fileNotFoundException.toString()
            WorkerUtils.makeStatusNotification(
                title,
                summary,
                fileNotFoundExceptionContent,
                applicationContext
            )
            throw RuntimeException("Failed to decode input stream", fileNotFoundException)
        } catch (throwable: Throwable) {
            val throwableContent = "Error applying blur, " +
                    "throwable: " + throwable.toString()
            WorkerUtils.makeStatusNotification(title, summary, throwableContent, applicationContext)
            return Result.failure()
        }
    }
}