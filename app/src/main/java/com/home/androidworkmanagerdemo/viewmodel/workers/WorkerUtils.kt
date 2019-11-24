package com.home.androidworkmanagerdemo.viewmodel.workers

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.renderscript.Allocation
import android.renderscript.Element
import android.renderscript.RenderScript
import android.renderscript.ScriptIntrinsicBlur
import android.util.Log
import androidx.annotation.WorkerThread
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.home.androidworkmanagerdemo.R
import com.home.androidworkmanagerdemo.model.MainModel
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.util.*

internal object WorkerUtils {

    private val VERBOSE_NOTIFICATION_CHANNEL_NAME: CharSequence = "Verbose WorkManager Notifications"
    private var VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION = "Shows notifications whenever work starts"
    private const val CHANNEL_ID = "VERBOSE_NOTIFICATION"
    private const val NOTIFICATION_ID = 1
    private const val DELAY_TIME_MILLIS: Long = 3000

    /**
     * Create a Notification that is shown as a heads-up notification if possible.
     * For this codelab, this is used to show a notification so that you know when different steps
     * of the background work chain are starting
     */
    fun makeStatusNotification(title: String, summary: String, content: String, context: Context) {
        // Make a channel if necessary
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val name = VERBOSE_NOTIFICATION_CHANNEL_NAME
            val description = VERBOSE_NOTIFICATION_CHANNEL_DESCRIPTION
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            channel.description = description
            // Add the channel
            val notificationManager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
        // Create the notification
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVibrate(LongArray(0))
        // 創建大文本樣式
        val bigTextStyle = NotificationCompat.BigTextStyle()
        bigTextStyle.setBigContentTitle(title)
            .setSummaryText(summary)
            .bigText(content)
        builder.setStyle(bigTextStyle) // 設置大文本樣式
        // Show the notification
        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, builder.build())
    }

    /**
     * Method for sleeping for a fixed about of time to emulate slower work
     */
    fun sleep() {
        try {
            Thread.sleep(DELAY_TIME_MILLIS, 0)
        } catch (e: InterruptedException) {
            Log.d("more", e.message!!)
        }
    }

    /**
     * Blurs the given Bitmap image
     *
     * @param bitmap             Image to blur
     * @param applicationContext Application context
     * @return Blurred bitmap image
     */
    @WorkerThread
    fun blurBitmap(
        bitmap: Bitmap,
        applicationContext: Context
    ): Bitmap {
        var rsContext: RenderScript? = null
        try {
            // Create the output bitmap
            val output = Bitmap.createBitmap(bitmap.width, bitmap.height, bitmap.config)
            // Blur the image
            rsContext = RenderScript.create(applicationContext, RenderScript.ContextType.DEBUG)
            val inAlloc = Allocation.createFromBitmap(rsContext, bitmap)
            val outAlloc = Allocation.createTyped(rsContext, inAlloc.type)
            val theIntrinsic = ScriptIntrinsicBlur.create(rsContext, Element.U8_4(rsContext))
            theIntrinsic.setRadius(10f)
            theIntrinsic.setInput(inAlloc)
            theIntrinsic.forEach(outAlloc)
            outAlloc.copyTo(output)
            return output
        } finally {
            rsContext?.finish()
        }
    }

    /**
     * Writes bitmap to a temporary file and returns the Uri for the file
     *
     * @param applicationContext Application context
     * @param bitmap             Bitmap to write to temp file
     * @return Uri for temp file with bitmap
     * @throws FileNotFoundException Throws if bitmap file cannot be found
     */
    @Throws(FileNotFoundException::class)
    fun writeBitmapToFile(
        applicationContext: Context,
        bitmap: Bitmap
    ): Uri {
        val name = String.format("blur-filter-output-%s.png", UUID.randomUUID().toString())
        val outputDir = File(applicationContext.filesDir, MainModel.outputPath)
        if (!outputDir.exists()) outputDir.mkdirs() // should succeed
        val outputFile = File(outputDir, name)
        var out: FileOutputStream? = null
        try {
            out = FileOutputStream(outputFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 0 /* ignored for PNG */, out)
        } finally {
            if (out != null)
                try {
                    out.close()
                } catch (ignore: IOException) {
                }
        }
        return Uri.fromFile(outputFile)
    }
}