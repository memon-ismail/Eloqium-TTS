package org.eloqium.tts.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import org.eloqium.tts.ui.MainActivity

class EloqiumForegroundService : Service() {

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: Initializing foreground service")
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val settings = Settings(applicationContext)
        if (!settings.persistentNotification) {
            Log.i(TAG, "Persistent notification disabled in settings, stopping self")
            stopForegroundCompat()
            stopSelf()
            return START_NOT_STICKY
        }

        val notification = buildNotification()
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(
                    NOTIFICATION_ID,
                    notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                )
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }
            Log.i(TAG, "startForeground succeeded (mediaPlayback)")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to start foreground service", t)
        }

        return START_STICKY
    }

    override fun onDestroy() {
        Log.i(TAG, "onDestroy: stopping foreground service")
        stopForegroundCompat()
        super.onDestroy()
    }

    private fun stopForegroundCompat() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                stopForeground(STOP_FOREGROUND_REMOVE)
            } else {
                @Suppress("DEPRECATION")
                stopForeground(true)
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error in stopForeground", t)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Eloqium Foreground Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Eloqium active for background speech reliability"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val launchIntent = Intent(this, MainActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            launchIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Eloqium TTS")
            .setContentText("Eloqium TTS is running in the background.")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    companion object {
        private const val TAG = "EloqiumForeground"
        const val CHANNEL_ID = SettingsDefaults.FOREGROUND_CHANNEL_ID
        const val NOTIFICATION_ID = SettingsDefaults.FOREGROUND_NOTIFICATION_ID

        fun syncService(context: Context, settings: Settings) {
            if (settings.persistentNotification) {
                start(context)
            } else {
                stop(context)
            }
        }

        fun start(context: Context) {
            try {
                val intent = Intent(context, EloqiumForegroundService::class.java)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to start EloqiumForegroundService", t)
            }
        }

        fun stop(context: Context) {
            try {
                val intent = Intent(context, EloqiumForegroundService::class.java)
                context.stopService(intent)
            } catch (t: Throwable) {
                Log.w(TAG, "Failed to stop EloqiumForegroundService", t)
            }
        }
    }
}
