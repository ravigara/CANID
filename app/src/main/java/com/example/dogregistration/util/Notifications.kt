package com.example.dogregistration.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat

private const val CHANNEL_ID = "dog_registration_channel"
private const val CHANNEL_NAME = "Dog Registration"
private const val CHANNEL_DESC = "Notifications for dog registration events"

fun createNotificationChannelIfNeeded(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val existing = nm.getNotificationChannel(CHANNEL_ID)
        if (existing == null) {
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT)
            channel.description = CHANNEL_DESC
            nm.createNotificationChannel(channel)
        }
    }
}
fun sendRegistrationNotification(context: Context, dogName: String): Boolean {
    // On Android 13+ we need POST_NOTIFICATIONS runtime permission
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ActivityCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!granted) return false
    }

    createNotificationChannelIfNeeded(context)
    val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val notif = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.ic_dialog_info)
        .setContentTitle("Registration successful")
        .setContentText("Successfully registered: ${if (dogName.isBlank()) "Unnamed dog" else dogName}")
        .setAutoCancel(true)
        .build()
    nm.notify(("dog_reg_${System.currentTimeMillis()}").hashCode(), notif)
    return true
}
