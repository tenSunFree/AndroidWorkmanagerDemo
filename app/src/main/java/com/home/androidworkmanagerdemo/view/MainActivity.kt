package com.home.androidworkmanagerdemo.view

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.text.TextUtils
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import com.bumptech.glide.Glide
import com.home.androidworkmanagerdemo.R
import com.home.androidworkmanagerdemo.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_IMAGE = 100
        private const val REQUEST_CODE_PERMISSIONS = 101
    }

    private var mainViewModel: MainViewModel? = null
    private val permissions = mutableListOf(
        Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        initializeMainViewModel()
        requestPermissionsIfNecessary()
        initializeClickListener()
    }

    private fun initializeMainViewModel() {
        mainViewModel = ViewModelProviders.of(this).get(MainViewModel::class.java)
        mainViewModel!!.outputWorkInfo.observe(this, Observer {
            // 捕獲單個WorkInfo, 以便顯示WorkInfo的整個過程都在一個位置
            // 如果沒有匹配的工作信息, 則什麼也不做
            if (it == null || it.isEmpty()) return@Observer
            // 只關心一個輸出狀態, 每個延續只有一個標記為TAG_OUTPUT的工作程序
            val workInfo = it[0]
            val finished = workInfo.state.isFinished
            if (!finished) showWorkInProgress()
            else {
                showWorkFinished()
                val outputData = workInfo.outputData;
                val outputImageUri = outputData.getString(mainViewModel!!.keyImageUri)
                if (!TextUtils.isEmpty(outputImageUri)) {
                    mainViewModel!!.setOutputUri(outputImageUri)
                    progress_bar.visibility = View.GONE
                    image_view_ok.visibility = View.GONE
                    image_view_cance.visibility = View.GONE
                    image_view_folder.visibility = View.VISIBLE
                }
            }
        })
    }

    private fun initializeClickListener() {
        image_view_ok.setOnClickListener { mainViewModel!!.applyBlur() }
        image_view_cance.setOnClickListener { jumpGetPicture() }
        image_view_folder.setOnClickListener {
            val currentUri = mainViewModel!!.outputUri
            if (currentUri != null) {
                val actionView = Intent(Intent.ACTION_VIEW, currentUri)
                if (actionView.resolveActivity(packageManager) != null) startActivity(actionView)
            }
        }
    }

    /**
     * 請求權限
     */
    private fun requestPermissionsIfNecessary() {
        if (!checkAllPermissions())
            ActivityCompat.requestPermissions(
                this,
                permissions.toTypedArray(),
                REQUEST_CODE_PERMISSIONS
            )
        else jumpGetPicture()
    }

    private fun checkAllPermissions(): Boolean {
        var hasPermissions = true
        for (permission in permissions)
            hasPermissions =
                hasPermissions and (ContextCompat.checkSelfPermission(this, permission)
                        == PackageManager.PERMISSION_GRANTED)
        return hasPermissions
    }

    /**
     * 從文件系統獲取圖像
     */
    private fun jumpGetPicture() {
        val chooseIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(
            chooseIntent,
            REQUEST_CODE_IMAGE
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS)
            if (checkAllPermissions()) jumpGetPicture()
            else toast("取得權限失敗")
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_IMAGE) showSelectedPicture(data!!)
            else toast("Unknown request code.")
        } else toast(String.format("Unexpected Result code %s", resultCode))
    }

    private fun showSelectedPicture(data: Intent) {
        var imageUri: Uri? = null
        val index = 0
        if (data.clipData != null) imageUri = data.clipData!!.getItemAt(index).uri
        else if (data.data != null) imageUri = data.data
        if (imageUri == null) toast("Invalid input image Uri.")
        else {
            Glide.with(this).load(imageUri).into(image_view_selected_picture)
            progress_bar.visibility = View.GONE
            image_view_ok.visibility = View.VISIBLE
            image_view_cance.visibility = View.VISIBLE
            image_view_folder.visibility = View.GONE
            mainViewModel!!.setImageUri(imageUri.toString())
        }
    }

    private fun showWorkInProgress() {
        progress_bar.visibility = View.VISIBLE
        image_view_ok.visibility = View.GONE
        image_view_cance.visibility = View.GONE
        image_view_folder.visibility = View.GONE
    }

    private fun showWorkFinished() {
        progress_bar.visibility = View.GONE
        image_view_ok.visibility = View.VISIBLE
        image_view_cance.visibility = View.VISIBLE
        image_view_folder.visibility = View.GONE
    }

    override fun onDestroy() {
        mainViewModel!!.cancelWork()
        super.onDestroy()
    }
}