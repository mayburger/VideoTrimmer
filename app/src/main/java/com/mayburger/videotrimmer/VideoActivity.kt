package com.mayburger.videotrimmer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.MediaController
import androidx.appcompat.app.AppCompatActivity
import com.mayburger.videotrimmer.utils.FileUtil
import kotlinx.android.synthetic.main.activity_video.*

class VideoActivity : AppCompatActivity(){

    companion object{
        private const val EXTRA_URI = "uri"
        fun newIntent(context: Context, uri:String?){
            val i = Intent(context, VideoActivity::class.java)
            i.putExtra(EXTRA_URI,uri)
            context.startActivity(i)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video)
        val mediaController = MediaController(this)
        mediaController.setAnchorView(videoView)
        val videoUri = (Uri.parse(intent.getStringExtra(EXTRA_URI)))
        videoView.setVideoURI(videoUri)
        println("This is the duration from FileUtil ${FileUtil.getVideoDuration(this,videoUri)}")
        videoView.setMediaController(mediaController)
    }

    override fun onBackPressed() {
        finish()
    }
}