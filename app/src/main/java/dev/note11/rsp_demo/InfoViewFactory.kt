package dev.note11.rsp_demo

import android.content.Context
import android.view.LayoutInflater
import android.view.View

object InfoViewFactory {
    fun newErrorDialogView(context: Context?): View {
        val inflater = LayoutInflater.from(context)
        return inflater.inflate(R.layout.error_dialog, null, false)
    }
}