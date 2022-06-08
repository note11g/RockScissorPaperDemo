package dev.note11.rsp_demo.vision

import android.widget.TextView
import android.view.TextureView
import android.os.Bundle
import androidx.camera.core.ImageProxy
import org.pytorch.Tensor
import android.view.ViewStub
import org.pytorch.LiteModuleLoader
import org.pytorch.torchvision.TensorImageUtils
import org.pytorch.IValue
import android.os.SystemClock
import android.util.Log
import android.view.View
import androidx.annotation.*
import dev.note11.rsp_demo.R
import dev.note11.rsp_demo.Utils
import dev.note11.rsp_demo.vision.view.ResultRowView
import org.pytorch.Module
import java.io.File
import java.lang.Exception
import java.nio.FloatBuffer
import java.util.*

class ImageClassificationActivity : AbstractCameraXActivity<ImageClassificationActivity.AnalysisResult?>() {
    class AnalysisResult(
        val topNClassNames: Array<String?>, val topNScores: FloatArray,
        val moduleForwardDuration: Long, val analysisDuration: Long
    )

    private var mAnalyzeImageErrorState = false
    private val mResultRowViews = arrayOfNulls<ResultRowView>(TOP_K)
    private lateinit var mFpsText: TextView
    private lateinit var mMsText: TextView
    private lateinit var mMsAvgText: TextView
    private lateinit var mInputTensorBuffer: FloatBuffer
    private lateinit var mInputTensor: Tensor
    private var mModule: Module? = null
    private var mMovingAvgSum: Long = 0
    private val mMovingAvgQueue: Queue<Long> = LinkedList()
    override val contentViewLayoutId: Int = R.layout.activity_image_classification
    override val cameraPreviewTextureView: TextureView
        get() = (findViewById<View>(R.id.image_classification_texture_view_stub) as ViewStub)
            .inflate()
            .findViewById(R.id.image_classification_texture_view)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val headerResultRowView =
            findViewById<ResultRowView>(R.id.image_classification_result_header_row)
        headerResultRowView.nameTextView.setText(R.string.image_classification_results_header_row_name)
        headerResultRowView.scoreTextView.setText(R.string.image_classification_results_header_row_score)
        mResultRowViews[0] = findViewById(R.id.image_classification_top1_result_row)
        mResultRowViews[1] = findViewById(R.id.image_classification_top2_result_row)
        mResultRowViews[2] = findViewById(R.id.image_classification_top3_result_row)
        mFpsText = findViewById(R.id.image_classification_fps_text)
        mMsText = findViewById(R.id.image_classification_ms_text)
        mMsAvgText = findViewById(R.id.image_classification_ms_avg_text)
    }

    override fun applyToUiAnalyzeImageResult(result: AnalysisResult?) {
        mMovingAvgSum += result!!.moduleForwardDuration
        mMovingAvgQueue.add(result.moduleForwardDuration)
        if (mMovingAvgQueue.size > MOVING_AVG_PERIOD) {
            mMovingAvgSum -= mMovingAvgQueue.remove()
        }
        for (i in 0 until TOP_K) {
            val rowView = mResultRowViews[i]
            rowView!!.nameTextView.text = result.topNClassNames[i]
            rowView.scoreTextView.text = String.format(
                Locale.US, SCORES_FORMAT,
                result.topNScores[i]
            )
            rowView.setProgressState(false)
        }
        mMsText.text = String.format(
            Locale.US,
            FORMAT_MS,
            result.moduleForwardDuration
        )
        if (mMsText.visibility != View.VISIBLE) {
            mMsText.visibility = View.VISIBLE
        }
        mFpsText.text = String.format(
            Locale.US,
            FORMAT_FPS,
            1000f / result.analysisDuration
        )
        if (mFpsText.visibility != View.VISIBLE) {
            mFpsText.visibility = View.VISIBLE
        }
        if (mMovingAvgQueue.size == MOVING_AVG_PERIOD) {
            val avgMs = mMovingAvgSum.toFloat() / MOVING_AVG_PERIOD
            mMsAvgText.text = String.format(
                Locale.US,
                FORMAT_AVG_MS,
                avgMs
            )
            if (mMsAvgText.visibility != View.VISIBLE) {
                mMsAvgText.visibility = View.VISIBLE
            }
        }
    }


    override val infoViewAdditionalText: String = MODULE_ASSET_NAME

    private fun importModule() {
        val moduleFileAbsoluteFilePath =
            Utils.assetFilePath(this, MODULE_ASSET_NAME)?.let { File(it).absolutePath }
        mModule = LiteModuleLoader.load(moduleFileAbsoluteFilePath)
        mInputTensorBuffer =
            Tensor.allocateFloatBuffer(3 * INPUT_TENSOR_WIDTH * INPUT_TENSOR_HEIGHT)
        mInputTensor = Tensor.fromBlob(
            mInputTensorBuffer,
            longArrayOf(1, 3, INPUT_TENSOR_HEIGHT.toLong(), INPUT_TENSOR_WIDTH.toLong())
        )
    }

    @WorkerThread
    override fun analyzeImage(image: ImageProxy, rotationDegrees: Int): AnalysisResult? {
        return if (mAnalyzeImageErrorState) {
            null
        } else try {
            if (mModule == null) importModule()

            val startTime = SystemClock.elapsedRealtime()
            TensorImageUtils.imageYUV420CenterCropToFloatBuffer(
                image.image, rotationDegrees,
                INPUT_TENSOR_WIDTH, INPUT_TENSOR_HEIGHT,
                TensorImageUtils.TORCHVISION_NORM_MEAN_RGB,
                TensorImageUtils.TORCHVISION_NORM_STD_RGB,
                mInputTensorBuffer, 0
            )
            val moduleForwardStartTime = SystemClock.elapsedRealtime()
            val outputTensor = mModule!!.forward(IValue.from(mInputTensor)).toTensor()
            val moduleForwardDuration = SystemClock.elapsedRealtime() - moduleForwardStartTime
            val scores = outputTensor.dataAsFloatArray
            val ixs = Utils.topK(scores, TOP_K)
            val topKClassNames = arrayOfNulls<String>(TOP_K)
            val topKScores = FloatArray(TOP_K)
            for (i in 0 until TOP_K) {
                val ix = ixs[i]
                topKClassNames[i] = CLASSES[ix]
                topKScores[i] = scores[ix]
            }
            val analysisDuration = SystemClock.elapsedRealtime() - startTime
            AnalysisResult(topKClassNames, topKScores, moduleForwardDuration, analysisDuration)
        } catch (e: Exception) {
            Log.e(TAG, "Error during image analysis", e)
            mAnalyzeImageErrorState = true
            runOnUiThread { if (!isFinishing) showErrorDialog { v: View? -> finish() } }
            null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mModule?.destroy()
    }

    companion object {
        private const val MODULE_ASSET_NAME: String = "model_layer2.pt"
        private val CLASSES = listOf("paper", "rock", "scissors")
        private const val INPUT_TENSOR_WIDTH = 224 // todo : resolution upscale
        private const val INPUT_TENSOR_HEIGHT = 224 // todo : resolution upscale
        private const val TOP_K = 3
        private const val TAG = "ModelDemo"
        private const val MOVING_AVG_PERIOD = 10
        private const val FORMAT_MS = "%dms"
        private const val FORMAT_AVG_MS = "avg:%.0fms"
        private const val FORMAT_FPS = "%.1fFPS"
        private const val SCORES_FORMAT = "%.2f"
    }
}