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
//        content://com.google.android.apps.photos.contentprovider/-1/2/content%3A%2F%2Fmedia%2Fexternal%2Fvideo%2Fmedia%2F107700/ORIGINAL/NONE/video%2Fmp4/1845840048
//        /-1/2/content:/media/external/video/media/107700/ORIGINAL/NONE/video/mp4/975675596
        if (i != null) {
            val path = i.getStringExtra(EXTRA_URI)
            trimmer.setVideoURI(Uri.parse(path))
            trimmer.setMaxDuration(30000)
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
    }

    override fun onError(message: String?) {

    }

    override fun cancelAction() {

    }
}