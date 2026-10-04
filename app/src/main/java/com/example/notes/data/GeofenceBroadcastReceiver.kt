package com.example.notes.data

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.notes.MainActivity
import com.example.notes.NotesApplication
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class GeofenceBroadcastReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val geofencingEvent = GeofencingEvent.fromIntent(intent) ?: return
        if (geofencingEvent.hasError()) {
            Log.e("GeofenceReceiver", "GeofencingEvent error code: ${geofencingEvent.errorCode}")
            return
        }

        val geofenceTransition = geofencingEvent.geofenceTransition
        if (geofenceTransition == Geofence.GEOFENCE_TRANSITION_ENTER) {
            val triggeringGeofences = geofencingEvent.triggeringGeofences ?: return

            // Use goAsync to process database query asynchronously in BroadcastReceiver
            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val app = context.applicationContext as? NotesApplication
                    val repository = app?.repository

                    for (geofence in triggeringGeofences) {
                        val todoId = geofence.requestId.toLongOrNull() ?: continue
                        val todo = repository?.getTodoById(todoId)
                        if (todo != null && !todo.isCompleted) {
                            sendNotification(context, todo.title, todo.locationName ?: "your set location")
                        }
                    }
                } catch (e: Exception) {
                    Log.e("GeofenceReceiver", "Error retrieving todo or sending notification", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }

    private fun sendNotification(context: Context, title: String, location: String) {
        val channelId = "todo_geofence_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Todo Geofence Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifies you when you are near tasks with locations set."
            }
            notificationManager.createNotificationChannel(channel)
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Task Reminder Nearby!")
            .setContentText("Don't forget: \"$title\" at $location")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}
