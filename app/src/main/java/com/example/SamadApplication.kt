package com.example

import android.app.Application
import android.util.Log
import com.example.notification.NotificationHelper
import com.example.work.WorkScheduler

class SamadApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        try {
            NotificationHelper.createNotificationChannel(this)
        } catch (t: Throwable) {
            Log.w("SamadApplication", "Notification channel init skipped: ${t.message}")
        }
        try {
            WorkScheduler.schedulePeriodicSync(this)
        } catch (t: Throwable) {
            Log.w("SamadApplication", "Periodic sync scheduling skipped: ${t.message}")
        }
    }
}
