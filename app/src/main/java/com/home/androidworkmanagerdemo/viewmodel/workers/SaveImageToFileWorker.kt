package com.home.androidworkmanagerdemo.viewmodel.workers

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.MediaStore
import android.text.TextUtils
import androidx.work.Data
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.home.androidworkmanagerdemo.model.MainModel
import java.text.SimpleDateFormat
import java.util.*

class SaveImageToFileWorker(appContext: Context, workerParams: WorkerParameters) :
    Worker(appContext, workerParams) {

    companion object {
        private const val TITLE = "Blurred Image"
        @SuppressLint("ConstantLocale")
        private val DATE_FORMATTER =
            SimpleDateFormat("yyyy.MM.dd 'at' HH:mm:ss z", Locale.getDefault())
    }

    override fun doWork(): Result {
        val applicationContext = applicationContext
        val title = MainModel.notificationTitle
        val summary = MainModel.notificationSummary
        val imageUri = inputData.getString(MainModel.keyImageUri)
        val content = "Saving image"
        WorkerUtils.makeStatusNotification(title!!, summary!!, content, applicationContext)
        WorkerUtils.sleep()
        val resolver = applicationContext.contentResolver
        try {
            val bitmap =
                BitmapFactory.decodeStream(resolver.openInputStream(Uri.parse(imageUri)))
            val imageUrl =
                MediaStore.Images.Media.insertImage(
                    resolver,
                    bitmap,
                    TITLE,
                    DATE_FORMATTER.format(Date())
                )
            if (TextUtils.isEmpty(imageUrl)) return Result.failure()
            val outputData = Data.Builder()
                .putString(MainModel.keyImageUri, imageUrl)
                .build()
            return Result.success(outputData)
        } catch (exception: Exception) {
            val exceptionContent = "Unable to save image to Gallery, " +
                    "exception: " + exception.toString()
            WorkerUtils.makeStatusNotification(
                title,
                summary,
                exceptionContent,
                applicationContext
            )
            return Result.failure()
        }
    }
}
