package com.example.notes.data

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.notes.data.local.TodoEntity
import com.google.android.gms.location.Geofence
import com.google.android.gms.location.GeofencingClient
import com.google.android.gms.location.GeofencingRequest
import com.google.android.gms.location.LocationServices

class GeofenceHelper(private val context: Context) {

    private val geofencingClient: GeofencingClient = LocationServices.getGeofencingClient(context)

    private val geofencePendingIntent: PendingIntent by lazy {
        val intent = Intent(context, GeofenceBroadcastReceiver::class.java)
        PendingIntent.getBroadcast(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
    }

    @SuppressLint("MissingPermission")
    fun addGeofence(todo: TodoEntity, radiusInMeters: Float = 150f) {
        val lat = todo.latitude ?: return
        val lng = todo.longitude ?: return
        
        val geofence = Geofence.Builder()
            .setRequestId(todo.id.toString())
            .setCircularRegion(lat, lng, radiusInMeters)
            .setExpirationDuration(Geofence.NEVER_EXPIRE)
            .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER)
            .build()

        val request = GeofencingRequest.Builder()
            .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_ENTER)
            .addGeofence(geofence)
            .build()

        try {
            geofencingClient.addGeofences(request, geofencePendingIntent)
                .addOnSuccessListener {
                    Log.d("GeofenceHelper", "Geofence added successfully for Todo ${todo.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("GeofenceHelper", "Failed to add geofence for Todo ${todo.id}", e)
                }
        } catch (e: SecurityException) {
            Log.e("GeofenceHelper", "SecurityException: Location permission missing", e)
        }
    }

    fun removeGeofence(todoId: Long) {
        val requestIds = listOf(todoId.toString())
        try {
            geofencingClient.removeGeofences(requestIds)
                .addOnSuccessListener {
                    Log.d("GeofenceHelper", "Geofence removed for Todo $todoId")
                }
                .addOnFailureListener { e ->
                    Log.e("GeofenceHelper", "Failed to remove geofence for Todo $todoId", e)
                }
        } catch (e: SecurityException) {
            Log.e("GeofenceHelper", "SecurityException when removing geofence", e)
        }
    }
}
