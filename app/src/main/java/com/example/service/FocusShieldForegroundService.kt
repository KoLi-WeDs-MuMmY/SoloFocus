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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground Service that keeps the System Focus blocker process active in the background
 * and runs a single guarded UsageStatsManager fallback watcher.
 */
class FocusShieldForegroundService : Service() {

    companion object {
        const val CHANNEL_ID = "focus_shield_channel"
        const val NOTIFICATION_ID = 9101

        @Volatile
        private var isServiceRunning: Boolean = false

        fun startShield(context: Context) {
            if (isServiceRunning) return
            try {
                val appCtx = context.applicationContext
                val intent = Intent(appCtx, FocusShieldForegroundService::class.java)
                ContextCompat.startForegroundService(appCtx, intent)
            } catch (_: Exception) {
            }
        }

        fun stopShield(context: Context) {
            try {
                isServiceRunning = false
                val appCtx = context.applicationContext
                val intent = Intent(appCtx, FocusShieldForegroundService::class.java)
                appCtx.stopService(intent)
            } catch (_: Exception) {
            }
        }
    }

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Default + serviceJob)
    private var watcherJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        isServiceRunning = true
        FocusRepository.init(applicationContext)
        ensureShieldChannelCreated()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isServiceRunning = true
        if (!FocusRepository.isReady()) {
            FocusRepository.init(applicationContext)
        }
        val notification = buildOngoingShieldNotification()

        try {
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
        } catch (_: Exception) {
        }

        ensureSingleUsageStatsWatcher()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun ensureSingleUsageStatsWatcher() {
        if (watcherJob?.isActive == true) return
        watcherJob = serviceScope.launch {
            while (isActive) {
                delay(500L)

                // Never re-trigger while the 3s penalty overlay is already on screen or cooling down
                if (FocusBlockerService.isOverlayCurrentlyShowing()) {
                    continue
                }

                val now = System.currentTimeMillis()
                if (!FocusRepository.isDungeonLockActive(now)) {
                    continue
                }

                val blocked = FocusRepository.blockedPackages.value
                if (blocked.isEmpty()) {
                    continue
                }

                val topPkg = queryCurrentForegroundPackage(now) ?: continue
                val ownPkg = packageName
                if (topPkg == ownPkg ||
                    topPkg == "com.example" ||
                    topPkg.startsWith("com.aistudio.systemfocus")
                ) {
                    continue
                }

                if (blocked.contains(topPkg) && !FocusRepository.isPackageTemporarilyUnsealed(topPkg, now)) {
                    FocusRepository.recordBlockedInterception("UsageStats Watcher Intercepted: $topPkg")
                    FocusBlockerService.triggerBlockFromAnywhere(applicationContext, topPkg)
                }
            }
        }
    }

    private fun queryCurrentForegroundPackage(now: Long): String? {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return null
        val events = try {
            usm.queryEvents(now - 4000L, now)
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
        isServiceRunning = false
        watcherJob?.cancel()
        serviceJob.cancel()
        super.onDestroy()
    }
}
