package dev.note11.rsp_demo.vision.view

import android.content.Context
import kotlin.jvm.JvmOverloads
import android.widget.RelativeLayout
import android.widget.TextView
import androidx.annotation.Px
import dev.note11.rsp_demo.R
import androidx.annotation.StyleRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.util.AttributeSet

class ResultRowView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0,
    defStyleRes: Int = 0
) : RelativeLayout(context, attrs, defStyleAttr, defStyleRes) {
    val nameTextView: TextView
    val scoreTextView: TextView

    @Px
    private var mProgressBarHeightPx = 0

    @Px
    private var mProgressBarPaddingPx = 0
    private var mProgressBarDrawable: Drawable? = null
    private var mProgressBarProgressStateDrawable: Drawable? = null
    private var mIsInProgress = true
    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        val drawable =
            if (mIsInProgress) mProgressBarProgressStateDrawable else mProgressBarDrawable
        if (drawable != null) {
            val h = canvas.height
            val w = canvas.width
            drawable.setBounds(0, h - mProgressBarHeightPx, w, h)
            drawable.draw(canvas)
        }
    }

    fun setProgressState(isInProgress: Boolean) {
        val changed = isInProgress != mIsInProgress
        mIsInProgress = isInProgress
        if (isInProgress) {
            nameTextView.visibility = INVISIBLE
            scoreTextView.visibility = INVISIBLE
        } else {
            nameTextView.visibility = VISIBLE
            scoreTextView.visibility = VISIBLE
        }
        if (changed) {
            invalidate()
        }
    }

    init {
        inflate(context, R.layout.image_classification_result_row, this)
        nameTextView = findViewById(R.id.result_row_name_text)
        scoreTextView = findViewById(R.id.result_row_score_text)
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.ResultRowView,
            defStyleAttr, defStyleRes
        )
        try {
            @StyleRes val textAppearanceResId = a.getResourceId(
                R.styleable.ResultRowView_textAppearance,
                R.style.TextAppearanceImageClassificationResultTop2Plus
            )
            nameTextView.setTextAppearance(context, textAppearanceResId)
            scoreTextView.setTextAppearance(context, textAppearanceResId)
            @DimenRes val progressBarHeightDimenResId =
                a.getResourceId(R.styleable.ResultRowView_progressBarHeightRes, 0)
            mProgressBarHeightPx =
                if (progressBarHeightDimenResId != 0) resources.getDimensionPixelSize(
                    progressBarHeightDimenResId
                ) else 0
            @DimenRes val progressBarPaddingDimenResId =
                a.getResourceId(R.styleable.ResultRowView_progressBarPaddingRes, 0)
            mProgressBarPaddingPx =
                if (progressBarPaddingDimenResId != 0) resources.getDimensionPixelSize(
                    progressBarPaddingDimenResId
                ) else 0
            setPadding(
                paddingLeft, paddingTop, paddingRight,
                bottom + mProgressBarPaddingPx + mProgressBarHeightPx
            )
            @DrawableRes val progressBarDrawableResId =
                a.getResourceId(R.styleable.ResultRowView_progressBarDrawableRes, 0)
            mProgressBarDrawable = if (progressBarDrawableResId != 0) resources.getDrawable(
                progressBarDrawableResId,
                null
            ) else null
            @DrawableRes val progressBarDrawableProgressStateResId =
                a.getResourceId(R.styleable.ResultRowView_progressBarDrawableProgressStateRes, 0)
            mProgressBarProgressStateDrawable =
                if (progressBarDrawableResId != 0) resources.getDrawable(
                    progressBarDrawableProgressStateResId,
                    null
                ) else null
        } finally {
            a.recycle()
        }
    }
}