package com.nexauren.audiotools

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp
import com.nexauren.audiotools.notifications.NotificationCenter
import com.nexauren.audiotools.update.UpdateScheduler

class AudioToolsApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
            }
        } catch (exception: Exception) {
            Log.e(
                "AudioToolsApplication",
                "Firebase initialization failed at application startup.",
                exception
            )
        }

        NotificationCenter.createChannels(this)
        UpdateScheduler.schedule(this)
    }
}
