package dev.note11.rsp_demo.vision

import android.Manifest
import android.view.TextureView
import android.os.Bundle
import androidx.core.app.ActivityCompat
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.camera.core.Preview.OnPreviewOutputUpdateListener
import androidx.camera.core.Preview.PreviewOutput
import android.os.SystemClock
import android.util.Size
import androidx.annotation.*
import androidx.camera.core.*
import dev.note11.rsp_demo.BaseModuleActivity
import dev.note11.rsp_demo.StatusBarUtils

abstract class AbstractCameraXActivity<R> : BaseModuleActivity() {
    private var mLastAnalysisResultTime: Long = 0
    protected abstract val contentViewLayoutId: Int
    protected abstract val cameraPreviewTextureView: TextureView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        StatusBarUtils.setStatusBarOverlay(window, true)
        setContentView(contentViewLayoutId)
        startBackgroundThread()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                PERMISSIONS,
                REQUEST_CODE_CAMERA_PERMISSION
            )
        } else {
            setupCameraX()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_CAMERA_PERMISSION) {
            if (grantResults[0] == PackageManager.PERMISSION_DENIED) {
                Toast.makeText(
                    this,
                    "You can't use image classification example without granting CAMERA permission",
                    Toast.LENGTH_LONG
                )
                    .show()
                finish()
            } else {
                setupCameraX()
            }
        }
    }

    private fun setupCameraX() {
        val textureView = cameraPreviewTextureView
        val previewConfig = PreviewConfig.Builder()
            .apply { setLensFacing(CameraX.LensFacing.FRONT) }.build()
        val preview = Preview(previewConfig)
        preview.onPreviewOutputUpdateListener =
            OnPreviewOutputUpdateListener { output: PreviewOutput ->
                textureView.setSurfaceTexture(output.surfaceTexture)
            }
        val imageAnalysisConfig = ImageAnalysisConfig.Builder()
            .apply { setLensFacing(CameraX.LensFacing.FRONT) }
            .setTargetResolution(Size(224, 224))
            .setCallbackHandler(mBackgroundHandler!!)
            .setImageReaderMode(ImageAnalysis.ImageReaderMode.ACQUIRE_LATEST_IMAGE)
            .build()
        val imageAnalysis = ImageAnalysis(imageAnalysisConfig)
        imageAnalysis.analyzer = ImageAnalysis.Analyzer { image: ImageProxy, rotationDegrees: Int ->
            if (SystemClock.elapsedRealtime() - mLastAnalysisResultTime < 500) {
                return@Analyzer
            }
            val result = analyzeImage(image, rotationDegrees)
            if (result != null) {
                mLastAnalysisResultTime = SystemClock.elapsedRealtime()
                runOnUiThread { applyToUiAnalyzeImageResult(result) }
            }
        }
        CameraX.bindToLifecycle(this, preview, imageAnalysis)
    }

    @WorkerThread
    protected abstract fun analyzeImage(image: ImageProxy, rotationDegrees: Int): R?

    @UiThread
    protected abstract fun applyToUiAnalyzeImageResult(result: R)

    companion object {
        private const val REQUEST_CODE_CAMERA_PERMISSION = 200
        private val PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }
}