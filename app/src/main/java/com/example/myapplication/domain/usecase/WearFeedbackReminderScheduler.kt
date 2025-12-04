package com.example.myapplication.domain.usecase

import android.content.Context
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.myapplication.domain.model.WearFeedbackEntry
import com.example.myapplication.domain.worker.WearFeedbackReminderWorker
import com.example.myapplication.util.time.InstantCompat
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.util.Calendar
import java.util.concurrent.TimeUnit

private const val REMINDER_WORK_NAME = "wear_feedback_reminder"

class WearFeedbackReminderScheduler(
    context: Context
) {
    private val appContext = context.applicationContext

    fun updateSchedule(entry: WearFeedbackEntry?) {
        if (entry == null || !entry.isPending) {
            cancel()
            return
        }
        schedule(entry)
    }

    private fun schedule(entry: WearFeedbackEntry) {
        val delayMillis = computeDelayUntilReminder()
        val request = OneTimeWorkRequestBuilder<WearFeedbackReminderWorker>()
            .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
            .setInputData(
                workDataOf(
                    WearFeedbackReminderWorker.KEY_ENTRY_ID to entry.id
                )
            )
            .addTag(REMINDER_WORK_NAME)
            .build()
        withWorkManager { manager ->
            manager.enqueueUniqueWork(REMINDER_WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }

    private fun cancel() {
        withWorkManager { manager ->
            manager.cancelUniqueWork(REMINDER_WORK_NAME)
        }
    }

    private fun computeDelayUntilReminder(): Long {
        val nowInstant = InstantCompat.nowOrNull()
        return if (nowInstant != null) {
            computeDelayFromInstant(nowInstant)
        } else {
            computeDelayWithCalendar(System.currentTimeMillis())
        }
    }

    private fun computeDelayFromInstant(now: Instant): Long {
        val zone = ZoneId.systemDefault()
        val nowZoned = now.atZone(zone)
        val targetToday = nowZoned.withHour(21).withMinute(0).withSecond(0).withNano(0)
        val target = if (nowZoned.isBefore(targetToday)) {
            targetToday
        } else {
            targetToday.plusDays(1)
        }
        val duration = Duration.between(nowZoned, target)
        return duration.toMillis().coerceAtLeast(MIN_DELAY_MILLIS)
    }

    private fun computeDelayWithCalendar(currentTimeMillis: Long): Long {
        val calendar = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
        }
        val target = Calendar.getInstance().apply {
            timeInMillis = currentTimeMillis
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= calendar.timeInMillis) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }
        val delay = target.timeInMillis - currentTimeMillis
        return delay.coerceAtLeast(MIN_DELAY_MILLIS)
    }

    companion object {
        private const val MIN_DELAY_MILLIS = 1_000L
        private const val TAG = "WearFbReminder"
    }

    private inline fun withWorkManager(block: (WorkManager) -> Unit) {
        runCatching { WorkManager.getInstance(appContext) }
            .mapCatching(block)
            .onFailure { throwable ->
                Log.w(TAG, "WorkManager unavailable; skipping schedule", throwable)
            }
    }
}
