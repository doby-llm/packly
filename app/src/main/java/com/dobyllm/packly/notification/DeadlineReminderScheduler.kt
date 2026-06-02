package com.dobyllm.packly.notification

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.dobyllm.packly.core.model.PacklyTrip
import com.dobyllm.packly.core.model.TripId
import com.dobyllm.packly.core.model.TripStatus
import com.dobyllm.packly.core.time.toInstantOrNull

private const val CHANNEL_ID = "packly_deadline_reminders"
private const val CHANNEL_NAME = "Packing reminders"
private const val ACTION_DEADLINE_REMINDER = "com.dobyllm.packly.action.DEADLINE_REMINDER"
internal const val EXTRA_TRIP_ID = "com.dobyllm.packly.extra.TRIP_ID"

class DeadlineReminderScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val lastPlans = mutableMapOf<TripId, ReminderPlan?>()

    fun syncAll(trips: List<PacklyTrip>) {
        trips.forEach(::syncTrip)
    }

    fun syncTrip(trip: PacklyTrip) {
        val nextPlan = trip.reminderPlan()
        if (lastPlans.containsKey(trip.id) && lastPlans[trip.id] == nextPlan) return
        lastPlans[trip.id] = nextPlan
        if (nextPlan != null) {
            schedule(trip)
        } else {
            cancel(trip.id)
        }
    }

    fun cancel(tripId: TripId) {
        existingPendingIntent(tripId)?.let(alarmManager::cancel)
    }

    private fun schedule(trip: PacklyTrip) {
        val deadline = trip.packBy.toInstantOrNull() ?: return
        val triggerAtMillis = deadline.toEpochMilli().coerceAtLeast(System.currentTimeMillis() + 1_000L)
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            triggerAtMillis,
            pendingIntent(trip.id, PendingIntent.FLAG_UPDATE_CURRENT),
        )
    }

    private fun pendingIntent(tripId: TripId, updateFlag: Int): PendingIntent {
        val intent = Intent(context, DeadlineReminderReceiver::class.java).apply {
            action = ACTION_DEADLINE_REMINDER
            putExtra(EXTRA_TRIP_ID, tripId)
        }
        return PendingIntent.getBroadcast(
            context,
            tripId.stableRequestCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or updateFlag,
        )
    }

    private fun existingPendingIntent(tripId: TripId): PendingIntent? {
        val intent = Intent(context, DeadlineReminderReceiver::class.java).apply {
            action = ACTION_DEADLINE_REMINDER
            putExtra(EXTRA_TRIP_ID, tripId)
        }
        return PendingIntent.getBroadcast(
            context,
            tripId.stableRequestCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE,
        )
    }
}

private data class ReminderPlan(val tripId: TripId, val packBy: String)

private fun PacklyTrip.reminderPlan(): ReminderPlan? =
    if (shouldHaveReminder()) ReminderPlan(id, packBy.orEmpty()) else null

fun PacklyTrip.shouldHaveReminder(): Boolean =
    status == TripStatus.Active && packBy.toInstantOrNull() != null && entries.any { !it.isPacked }

fun PacklyTrip.remainingPackingCount(): Int = entries.filterNot { it.isPacked }.sumOf { it.quantity.coerceAtLeast(1) }

fun createDeadlineReminderChannel(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
    val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
        description = "Reminds you when a trip still has unpacked items at its Pack by deadline."
    }
    manager.createNotificationChannel(channel)
}

fun canPostPacklyNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

internal fun notificationChannelId(): String = CHANNEL_ID

internal fun TripId.stableRequestCode(): Int = fold(17) { acc, char -> 31 * acc + char.code }
