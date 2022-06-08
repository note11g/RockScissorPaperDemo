package dev.note11.rsp_demo

import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import android.view.*

object StatusBarUtils {
    fun setStatusBarOverlay(window: Window, showStatusBarAsOverlay: Boolean) {
        val decorView = window.decorView
        ViewCompat.setOnApplyWindowInsetsListener(
            decorView
        ) { v: View?, insets: WindowInsetsCompat? ->
            val defaultInsets = ViewCompat.onApplyWindowInsets(
                v!!, insets!!
            )
            defaultInsets.replaceSystemWindowInsets(
                defaultInsets.systemWindowInsetLeft,
                if (showStatusBarAsOverlay) 0 else defaultInsets.systemWindowInsetTop,
                defaultInsets.systemWindowInsetRight,
                defaultInsets.systemWindowInsetBottom
            )
        }
        ViewCompat.requestApplyInsets(decorView)
    }
}