package dev.note11.rsp_demo

import android.os.Bundle
import android.os.Handler
import androidx.annotation.UiThread
import androidx.appcompat.app.AppCompatActivity
import android.os.HandlerThread
import android.util.Log
import android.view.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.widget.Toolbar

open class BaseModuleActivity : AppCompatActivity() {
    private var mBackgroundThread: HandlerThread? = null
    protected var mBackgroundHandler: Handler? = null
    private var mUIHandler: Handler? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mUIHandler = Handler(mainLooper)
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        toolbar?.let { setSupportActionBar(it) }
        startBackgroundThread()
    }

    protected fun startBackgroundThread() {
        mBackgroundThread = HandlerThread("ModuleActivity")
        mBackgroundThread!!.start()
        mBackgroundHandler = Handler(mBackgroundThread!!.looper)
    }

    override fun onDestroy() {
        stopBackgroundThread()
        super.onDestroy()
    }

    private fun stopBackgroundThread() {
        mBackgroundThread!!.quitSafely()
        try {
            mBackgroundThread!!.join()
            mBackgroundThread = null
            mBackgroundHandler = null
        } catch (e: InterruptedException) {
            Log.e("Demo", "Error on stopping background thread", e)
        }
    }

    protected open val infoViewAdditionalText: String? get() = null

    @UiThread
    protected fun showErrorDialog(clickListener: View.OnClickListener) {
        val view = InfoViewFactory.newErrorDialogView(this)
        val builder = AlertDialog.Builder(this, R.style.CustomDialog)
            .setCancelable(false)
            .setView(view)
        val alertDialog = builder.show()
        view.setOnClickListener { v: View? ->
            clickListener.onClick(v)
            alertDialog.dismiss()
        }
    }
}