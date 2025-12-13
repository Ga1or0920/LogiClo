package com.example.myapplication.domain.usecase

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.myapplication.domain.model.WearFeedback
import com.example.myapplication.domain.worker.WearFeedbackReminderWorker
import java.time.Duration
import java.util.Date

class WearFeedbackReminderScheduler(private val context: Context) {

    fun updateSchedule(latestPending: WearFeedback?) {
        val workManager = WorkManager.getInstance(context)
        if (latestPending == null) {
            workManager.cancelUniqueWork(REMINDER_WORK_NAME)
            return
        }

        val now = Date()
        val wornAt = latestPending.wornAt
        val delay = Duration.between(wornAt.toInstant(), now.toInstant()).plusHours(REMINDER_DELAY_HOURS)
        if (delay.isNegative || delay.isZero) {
            workManager.cancelUniqueWork(REMINDER_WORK_NAME)
            return
        }

        val request = OneTimeWorkRequestBuilder<WearFeedbackReminderWorker>()
            .setInitialDelay(delay)
            .build()
        workManager.enqueueUniqueWork(
            REMINDER_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    companion object {
        private const val REMINDER_WORK_NAME = "wear_feedback_reminder"
        private const val REMINDER_DELAY_HOURS = 3L
    }
}
