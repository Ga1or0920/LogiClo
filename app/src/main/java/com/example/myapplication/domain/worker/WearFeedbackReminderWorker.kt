package com.example.myapplication.domain.worker

import android.Manifest
import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.myapplication.MainActivity
import com.example.myapplication.R
import com.example.myapplication.data.local.LaundryLoopDatabase
import com.example.myapplication.data.local.entity.WearFeedbackEntity
import com.example.myapplication.data.local.entity.toDomain
import com.example.myapplication.domain.usecase.WearFeedbackReminderScheduler
import com.example.myapplication.ui.feedback.FeedbackDestinations
import kotlinx.coroutines.flow.first

class WearFeedbackReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val database = LaundryLoopDatabase.getInstance(applicationContext)
        val dao = database.wearFeedbackDao()
        val entryEntity: WearFeedbackEntity = dao.observeLatestPending().first() ?: return Result.success()

        if (entryEntity.isComfortable != null) {
            return Result.success()
        }

        val clothingDao = database.clothingItemDao()
        val itemNames = entryEntity.itemIds.split(",").mapNotNull { clothingDao.getById(it)?.name }

        ensureChannel()
        if (!canPostNotifications()) {
            return Result.success()
        }

        val notificationManager = NotificationManagerCompat.from(applicationContext)
        if (!notificationManager.areNotificationsEnabled()) {
            return Result.success()
        }

        val pendingIntent = createLaunchIntent()
        val contentText = buildContentText(itemNames)

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_wear)
            .setContentTitle(applicationContext.getString(R.string.notification_feedback_title))
            .setContentText(contentText)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(contentText)
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        postNotification(notificationManager, notification)

        // Schedule the next reminder for tomorrow if the entry remains pending.
        val entry = entryEntity.toDomain()
        WearFeedbackReminderScheduler(applicationContext).updateSchedule(entry)

        return Result.success()
    }

    @SuppressLint("MissingPermission")
    private fun postNotification(
        notificationManager: NotificationManagerCompat,
        notification: android.app.Notification
    ) {
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun buildContentText(itemNames: List<String>): String {
        val context = applicationContext
        return when {
            itemNames.size >= 2 ->
                context.getString(R.string.notification_feedback_body_with_items, itemNames[0], itemNames[1])
            itemNames.size == 1 ->
                context.getString(R.string.notification_feedback_body_single, itemNames[0])
            else -> context.getString(R.string.notification_feedback_body_generic)
        }
    }

    private fun ensureChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    applicationContext.getString(R.string.notification_feedback_channel_name),
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = applicationContext.getString(R.string.notification_feedback_channel_description)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }

    private fun canPostNotifications(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun createLaunchIntent() = androidx.core.app.TaskStackBuilder.create(applicationContext).run {
        val launchIntent = Intent(applicationContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            putExtra(MainActivity.EXTRA_TARGET_DESTINATION, FeedbackDestinations.Pending)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        addNextIntentWithParentStack(launchIntent)
        getPendingIntent(
            REQUEST_CODE,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val KEY_ENTRY_ID = "entry_id"
        private const val CHANNEL_ID = "feedback_reminders"
        private const val NOTIFICATION_ID = 2001
        private const val REQUEST_CODE = 201
    }
}
