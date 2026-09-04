package com.example.usbcamera

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.usbcamera.databinding.ActivityMainBinding
import com.jiangdg.ausbc.CameraClient
import com.jiangdg.ausbc.callback.ICameraStateCallBack
import com.jiangdg.ausbc.camera.bean.CameraRequest
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var cameraClient: CameraClient? = null
    private var isRecording = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        checkAndRequestPermissions()
        initCameraClient()
        setupListeners()
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO
        )
        val needed = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        if (needed.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, needed.toTypedArray(), 1001)
        }
    }

    private fun initCameraClient() {
        val request = CameraRequest.Builder()
            .setPreviewWidth(1280)
            .setPreviewHeight(720)
            .setRenderMode(CameraRequest.RenderMode.OPENGL)
            .create()

        cameraClient = CameraClient.newBuilder(this)
            .setEnableGLES(true)
            .setRawImage(false)
            .setCameraStrategy(CameraClient.STRATEGY_UVC)
            .setCameraRequest(request)
            .openDebug(true)
            .build()

        cameraClient?.apply {
            setCameraStateCallBack(object : ICameraStateCallBack {
                override fun onCameraState(
                    self: ICameraStateCallBack.State,
                    msg: String?
                ) {
                    when (self) {
                        ICameraStateCallBack.State.OPENED -> {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Camera opened", Toast.LENGTH_SHORT).show()
                            }
                        }
                        ICameraStateCallBack.State.CLOSED -> {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Camera closed", Toast.LENGTH_SHORT).show()
                            }
                        }
                        ICameraStateCallBack.State.ERROR -> {
                            runOnUiThread {
                                Toast.makeText(this@MainActivity, "Camera error: $msg", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            })
            bindSurfaceView(binding.cameraPreviewView)
        }
    }

    private fun setupListeners() {
        binding.btnCapture.setOnClickListener {
            val fileName = "IMG_${getTimestamp()}.jpg"
            val outputDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val outputFile = File(outputDir, fileName)

            cameraClient?.captureImage(outputFile.absolutePath) { success, path ->
                runOnUiThread {
                    if (success) {
                        Toast.makeText(this, "Saved: $path", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Capture failed", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        binding.btnRecord.setOnClickListener {
            if (!isRecording) {
                val fileName = "VID_${getTimestamp()}.mp4"
                val outputDir = getExternalFilesDir(Environment.DIRECTORY_MOVIES)
                val outputFile = File(outputDir, fileName)

                cameraClient?.startRecord(outputFile.absolutePath) { isStarted, msg ->
                    runOnUiThread {
                        if (isStarted) {
                            isRecording = true
                            binding.btnRecord.text = "Stop Record"
                            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this, "Record error: $msg", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                cameraClient?.stopRecord { _, path ->
                    runOnUiThread {
                        isRecording = false
                        binding.btnRecord.text = "Start Record"
                        Toast.makeText(this, "Video saved: $path", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }

    private fun getTimestamp(): String {
        return SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraClient?.unBindSurfaceView()
        cameraClient?.destroy()
    }
}
