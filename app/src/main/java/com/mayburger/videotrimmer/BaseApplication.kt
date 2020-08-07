package com.mayburger.videotrimmer

import android.app.Application
import com.orhanobut.hawk.Hawk


class BaseApplication:Application() {
    override fun onCreate() {
        super.onCreate()
        Hawk.init(this).build()
    }
}