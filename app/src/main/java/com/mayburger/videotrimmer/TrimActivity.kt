package com.mayburger.videotrimmer

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kotlinx.android.synthetic.main.activity_trim.*

class TrimActivity : AppCompatActivity(){
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_trim)

        val trimmer = timeLine
        val i = intent;
        if (i != null){
            val path = i.getStringExtra(EXTRA_URI)
            trimmer.setVideoURI(Uri.parse(path))
            trimmer.setMaxDuration(30000)
        }

    }

    companion object{
        private const val EXTRA_URI = "uri"
        fun newIntent(context: Context, uri:String?){
            val i = Intent(context, TrimActivity::class.java)
            i.putExtra(EXTRA_URI,uri)
            context.startActivity(i)
        }
    }
}