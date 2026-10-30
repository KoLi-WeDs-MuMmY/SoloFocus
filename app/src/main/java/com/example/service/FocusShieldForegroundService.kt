package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.FocusRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service that prevents Android OS from freezing or throttling
 * the application process when the user minimizes the app.
 * Also runs a 350ms UsageStatsManager fallback watcher to guarantee interception
 * across all devices and OEM battery optimizers.
 */
class FocusShieldForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "focus_shield_channel"
        const val NOTIFICATION_ID = 9101

        fun startShield(context: Context) {
            try {
                val intent = Intent(context, FocusShieldForegroundService::class.java)
                ContextCompat.startForegroundService(context, intent)
            } catch (_: Exception) {
            }
        }

        fun stopShield(context: Context) {
            try {
                val intent = Intent(context, FocusShieldForegroundService::class.java)
                context.stopService(intent)
            } catch (_: Exception) {
            }
        }
    }

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var lastInterceptionTime = 0L

    override fun onCreate() {
        super.onCreate()
        FocusRepository.init(applicationContext)
        ensureShieldChannelCreated()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        FocusRepository.init(applicationContext)
        val notification = buildOngoingShieldNotification()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            } else {
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            }
            startForeground(NOTIFICATION_ID, notification, foregroundServiceType)
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }

        startUsageStatsWatcher()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun startUsageStatsWatcher() {
        serviceScope.launch {
            while (isActive) {
                delay(350L)
                val now = System.currentTimeMillis()
                if (now - lastInterceptionTime < 600L) {
                    continue
                }

                if (!FocusRepository.isDungeonLockActive(now)) {
                    continue
                }

                val blocked = FocusRepository.blockedPackages.value
                if (blocked.isEmpty()) {
                    continue
                }

                val topPkg = queryCurrentForegroundPackage()
                val ownPkg = packageName
                if (topPkg == null || topPkg == ownPkg || topPkg == "com.example") {
                    continue
                }

                if (blocked.contains(topPkg) && !FocusRepository.isPackageTemporarilyUnsealed(topPkg, now)) {
                    lastInterceptionTime = now
                    FocusRepository.recordBlockedInterception("UsageStats Watcher Intercepted: $topPkg")
                    FocusBlockerService.triggerBlockFromAnywhere(applicationContext, topPkg)
                }
            }
        }
    }

    private fun queryCurrentForegroundPackage(): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val events = try {
            usm.queryEvents(now - 3000L, now)
        } catch (_: Exception) {
            return null
        }

        var lastForegroundPkg: String? = null
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            ) {
                lastForegroundPkg = event.packageName
            }
        }
        return lastForegroundPkg
    }

    private fun ensureShieldChannelCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Dungeon Active Shield",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps the System Focus blocker active in the background"
                setShowBadge(false)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildOngoingShieldNotification(): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("[ SYSTEM FOCUS HUD: DUNGEON SHIELD ACTIVE ]")
            .setContentText("Protecting your focus against sealed distraction apps.")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        serviceJob.cancel()
        super.onDestroy()
    }
}
