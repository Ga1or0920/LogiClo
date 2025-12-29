package com.example.myapplication.domain.worker

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager

/**
 * AlarmManager から呼び出される BroadcastReceiver
 * システム時刻が21:00になったときに通知をトリガーする
 */
class FeedbackAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        // WorkManager を使って通知を送信
        val request = OneTimeWorkRequestBuilder<WearFeedbackReminderWorker>().build()
        WorkManager.getInstance(context).enqueue(request)
    }
}
