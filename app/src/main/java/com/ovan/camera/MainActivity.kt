package com.ovan.camera

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.view.MotionEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.ovan.camera.databinding.ActivityMainBinding
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    private lateinit var b: ActivityMainBinding
    private var capture: ImageCapture? = null
    private var camera: Camera? = null
    private var lens = CameraSelector.LENS_FACING_BACK
    private var flash = ImageCapture.FLASH_MODE_AUTO
    private var modeIndex = 0
    private var timerSeconds = 0
    private var gridVisible = true
    private val handler = Handler(Looper.getMainLooper())
    private var isCountingDown = false

    private val modes = listOf("SMART HDR", "NATURAL", "IPHONE STYLE", "NIGHT", "PORTRAIT")
    private val timerValues = listOf(0, 3, 10)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)

        setupControls()
        if (hasPermission()) startCamera() else requestCameraPermission()
    }

    private fun setupControls() {
        b.shutterButton.setOnClickListener { takePhoto() }
        b.switchButton.setOnClickListener {
            if (isCountingDown) return@setOnClickListener
            lens = if (lens == CameraSelector.LENS_FACING_BACK) CameraSelector.LENS_FACING_FRONT else CameraSelector.LENS_FACING_BACK
            startCamera()
        }
        b.flashButton.setOnClickListener { cycleFlash() }
        b.modeButton.setOnClickListener {
            modeIndex = (modeIndex + 1) % modes.size
            updateMode()
        }
        b.timerButton.setOnClickListener { cycleTimer() }
        b.gridButton.setOnClickListener {
            gridVisible = !gridVisible
            b.overlay.setGridVisible(gridVisible)
            b.gridButton.text = if (gridVisible) "⊞ GRID" else "⊞ OFF"
        }
        b.galleryButton.setOnClickListener { openGallery() }

        b.zoomSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val maxZoom = camera?.cameraInfo?.zoomState?.value?.maxZoomRatio ?: 8f
                val ratio = 1f + (progress / 100f) * (maxZoom - 1f)
                camera?.cameraControl?.setZoomRatio(ratio.coerceAtMost(maxZoom))
                b.zoomText.text = String.format("%.1fx", ratio.coerceAtMost(maxZoom))
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        b.exposureSeek.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (!fromUser) return
                val state = camera?.cameraInfo?.exposureState ?: return
                val range = state.exposureCompensationRange
                val normalized = progress / 100f
                val value = range.lower + ((range.upper - range.lower) * normalized).toInt()
                camera?.cameraControl?.setExposureCompensationIndex(value)
                b.exposureText.text = String.format("EV %+d", value)
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) = Unit
            override fun onStopTrackingTouch(seekBar: SeekBar?) = Unit
        })

        b.previewView.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP && !isCountingDown) {
                val point = b.previewView.meteringPointFactory.createPoint(event.x, event.y)
                camera?.cameraControl?.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                b.overlay.showFocus(event.x, event.y)
            }
            true
        }

        updateMode()
        updateTimerButton()
        updateFlashButton()
    }

    private fun startCamera() {
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            try {
                val provider = future.get()
                val selector = CameraSelector.Builder().requireLensFacing(lens).build()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = b.previewView.surfaceProvider
                }
                capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MAXIMIZE_QUALITY)
                    .setFlashMode(flash)
                    .build()

                provider.unbindAll()
                camera = provider.bindToLifecycle(this, selector, preview, capture)
                b.zoomSeek.progress = 0
                b.zoomText.text = "1.0x"
                b.exposureSeek.progress = 50
                b.exposureText.text = "EV 0"
            } catch (e: Exception) {
                Toast.makeText(this, "Kamera tidak dapat dimulai: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        if (isCountingDown) return
        val c = capture ?: return
        if (timerSeconds > 0) {
            runCountdown(c)
        } else {
            savePhoto(c)
        }
    }

    private fun runCountdown(c: ImageCapture) {
        isCountingDown = true
        b.shutterButton.isEnabled = false
        b.countdownText.visibility = View.VISIBLE
        var remaining = timerSeconds
        b.countdownText.text = remaining.toString()

        val tick = object : Runnable {
            override fun run() {
                remaining--
                if (remaining <= 0) {
                    b.countdownText.visibility = View.GONE
                    isCountingDown = false
                    b.shutterButton.isEnabled = true
                    savePhoto(c)
                } else {
                    b.countdownText.text = remaining.toString()
                    handler.postDelayed(this, 1000L)
                }
            }
        }
        handler.postDelayed(tick, 1000L)
    }

    private fun savePhoto(c: ImageCapture) {
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, "OVAN_${System.currentTimeMillis()}.jpg")
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/OVAN Camera")
        }
        val output = ImageCapture.OutputFileOptions.Builder(
            contentResolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            values
        ).build()

        c.takePicture(output, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
            override fun onImageSaved(result: ImageCapture.OutputFileResults) {
                Toast.makeText(this@MainActivity, "Foto tersimpan • OVAN Camera V4", Toast.LENGTH_SHORT).show()
            }
            override fun onError(exception: ImageCaptureException) {
                Toast.makeText(this@MainActivity, "Gagal mengambil foto: ${exception.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun cycleFlash() {
        flash = when (flash) {
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
        updateFlashButton()
        capture?.flashMode = flash
    }

    private fun updateFlashButton() {
        b.flashButton.text = when (flash) {
            ImageCapture.FLASH_MODE_AUTO -> "⚡ A"
            ImageCapture.FLASH_MODE_ON -> "⚡ ON"
            else -> "⚡ OFF"
        }
    }

    private fun cycleTimer() {
        val current = timerValues.indexOf(timerSeconds).let { if (it < 0) 0 else it }
        timerSeconds = timerValues[(current + 1) % timerValues.size]
        updateTimerButton()
    }

    private fun updateTimerButton() {
        b.timerButton.text = if (timerSeconds == 0) "⏱ OFF" else "⏱ ${timerSeconds}s"
    }

    private fun updateMode() {
        val name = modes[modeIndex]
        b.modeButton.text = name
        b.modeText.text = "OVAN $name"
        b.statusText.text = when (modeIndex) {
            0 -> "Smart HDR UI • detail & highlight preset"
            1 -> "Natural color • balanced contrast"
            2 -> "Soft highlights • natural contrast"
            3 -> "Low-light preset • exposure control"
            else -> "Subject focus • natural portrait preset"
        }
        b.exposureSeek.visibility = if (modeIndex == 3) View.VISIBLE else View.GONE
        b.exposureText.visibility = if (modeIndex == 3) View.VISIBLE else View.GONE
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, "image/*")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(intent)
        } catch (_: Exception) {
            startActivity(Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI))
        }
    }

    private fun requestCameraPermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA)
    }

    private fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CAMERA && hasPermission()) startCamera()
        else Toast.makeText(this, "Izin kamera diperlukan untuk menggunakan OVAN Camera.", Toast.LENGTH_LONG).show()
    }

    override fun onDestroy() {
        handler.removeCallbacksAndMessages(null)
        super.onDestroy()
    }

    companion object { private const val REQUEST_CAMERA = 10 }
}
