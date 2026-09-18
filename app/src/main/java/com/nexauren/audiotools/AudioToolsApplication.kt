package com.nexauren.audiotools

import android.app.Application
import com.nexauren.audiotools.notifications.NotificationCenter
import com.nexauren.audiotools.update.UpdateScheduler

class AudioToolsApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        NotificationCenter.createChannels(this)
        UpdateScheduler.schedule(this)
    }
}
