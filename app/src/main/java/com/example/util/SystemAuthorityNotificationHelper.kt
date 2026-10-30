package com.example.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R

object SystemAuthorityNotificationHelper {

    private const val CHANNEL_ID = "system_authority_hud_channel"
    private const val NOTIFICATION_ID_PERMISSIONS = 1001
    private const val NOTIFICATION_ID_PENALTY = 1002

    fun ensureChannelCreated(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "System Authority & Dungeon Alerts",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Provides quick access to unlock Restricted Settings, Overlay, and Accessibility permissions."
                    enableVibration(true)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /**
     * Posts a high-priority Solo Leveling System Notification with direct action buttons to:
     * 1. Unlock Android 13+ "Restricted Settings" (App Info -> 3-dots -> Allow restricted settings)
     * 2. Grant "Display Over Other Apps" (SYSTEM_ALERT_WINDOW)
     * 3. Grant "Accessibility Service" (FocusBlockerService)
     */
    fun postPermissionAccessNotification(
        context: Context,
        isAccessibilityEnabled: Boolean,
        canDrawOverlays: Boolean
    ) {
        if (!hasNotificationPermission(context)) return
        ensureChannelCreated(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 1: App Details (to unlock Android 13+ "Allow restricted settings")
        val appDetailsIntent = Intent(
            Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val appDetailsPendingIntent = PendingIntent.getActivity(
            context,
            1,
            appDetailsIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 2: Display Over Other Apps (SYSTEM_ALERT_WINDOW)
        val overlayIntent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val overlayPendingIntent = PendingIntent.getActivity(
            context,
            2,
            overlayIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Action 3: Accessibility Settings
        val accessibilityIntent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val accessibilityPendingIntent = PendingIntent.getActivity(
            context,
            3,
            accessibilityIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val statusSummary = buildString {
            append("Overlay: ")
            append(if (canDrawOverlays) "ONLINE" else "LOCKED")
            append(" • Accessibility: ")
            append(if (isAccessibilityEnabled) "ONLINE" else "LOCKED")
        }

        val bigText = buildString {
            append(statusSummary)
            append("\nIf Accessibility or Overlay is grayed out ('Restricted setting'), tap [ UNLOCK RESTRICTED ] -> top-right ⋮ menu -> 'Allow restricted settings', then enable [ OVERLAY ] and [ ACCESSIBILITY ].")
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("[ ! SYSTEM AUTHORITY: PERMISSION ACCESS ]")
            .setContentText(statusSummary)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(false)
            .addAction(0, "UNLOCK RESTRICTED", appDetailsPendingIntent)
            .addAction(0, "OVERLAY", overlayPendingIntent)
            .addAction(0, "ACCESSIBILITY", accessibilityPendingIntent)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PERMISSIONS, builder.build())
        } catch (_: SecurityException) {
            // Ignored if permission was revoked
        }
    }

    fun dismissPermissionNotification(context: Context) {
        try {
            NotificationManagerCompat.from(context).cancel(NOTIFICATION_ID_PERMISSIONS)
        } catch (_: Exception) {
        }
    }

    fun postPenaltyAlertNotification(context: Context, title: String, detail: String) {
        if (!hasNotificationPermission(context)) return
        ensureChannelCreated(context)

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            4,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(detail)
            .setStyle(NotificationCompat.BigTextStyle().bigText(detail))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentPendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID_PENALTY, builder.build())
        } catch (_: SecurityException) {
        }
    }
}
