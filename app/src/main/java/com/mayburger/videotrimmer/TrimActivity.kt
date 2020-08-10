package com.mayburger.videotrimmer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mayburger.videotrimmer.trimmer.interfaces.OnTrimVideoListener
import kotlinx.android.synthetic.main.activity_trim.*

class TrimActivity : AppCompatActivity(), OnTrimVideoListener {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trim)

        val trimmer = timeLine
        val i = intent;
        if (i != null) {
            val path = i.getStringExtra(EXTRA_URI)
            trimmer.setVideoURI(Uri.parse(path))
            trimmer.setOnTrimVideoListener(this)
        }
    }

    companion object {
        private const val EXTRA_URI = "uri"
        fun newIntent(context: Context, uri: String?) {
            val i = Intent(context, TrimActivity::class.java)
            i.putExtra(EXTRA_URI, uri)
            context.startActivity(i)
        }
    }

    override fun onTrimStarted() {

    }

    override fun getResult(uri: Uri?) {
        VideoActivity.newIntent(this, uri?.toString())
        finish()
    }

    override fun onError(message: String?) {

    }

    override fun cancelAction() {

    }
}