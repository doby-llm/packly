package com.dobyllm.packly.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.dobyllm.packly.MainActivity
import com.dobyllm.packly.R
import com.dobyllm.packly.data.repository.DataStorePacklyRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class DeadlineReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val tripId = intent.getStringExtra(EXTRA_TRIP_ID) ?: return
        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = DataStorePacklyRepository.get(context.applicationContext)
                val trip = repository.appState.first().trips.firstOrNull { it.id == tripId }
                if (trip != null && trip.shouldHaveReminder() && canPostPacklyNotifications(context)) {
                    postReminder(context, tripId, trip.name, trip.remainingPackingCount())
                }
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun postReminder(context: Context, tripId: String, tripName: String, remainingCount: Int) {
        val contentIntent = PendingIntent.getActivity(
            context,
            tripId.stableRequestCode(),
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra(EXTRA_TRIP_ID, tripId)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val body = "$remainingCount items still need packing for $tripName."
        val notification = NotificationCompat.Builder(context, notificationChannelId())
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Packly reminder")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .build()
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(tripId.stableRequestCode(), notification)
    }
}
