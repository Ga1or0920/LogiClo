package com.example.myapplication.domain.usecase

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.myapplication.domain.worker.FeedbackAlarmReceiver
import java.util.Calendar

/**
 * デバッグ用: AlarmManager (RTC_WAKEUP) を使った通知スケジューラ
 * システム時刻の変更に反応して21:00に通知を送信する
 */
class DebugFeedbackAlarmScheduler(context: Context) {
    private val appContext = context.applicationContext
    private val alarmManager = appContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    companion object {
        private const val TAG = "DebugFeedbackAlarm"
        private const val REQUEST_CODE = 21001
    }

    /**
     * 次の21:00にアラームをスケジュール
     * RTC_WAKEUP を使用するため、システム時刻の変更に反応する
     */
    fun scheduleAt2100() {
        val intent = Intent(appContext, FeedbackAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 次の21:00を計算
        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 21)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

            // 既に21:00を過ぎていたら翌日
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.DAY_OF_YEAR, 1)
            }
        }

        // RTC_WAKEUP: システム時刻ベース + デバイスをスリープから起こす
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Dozeモード対策
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    calendar.timeInMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Alarm scheduled for ${calendar.time}")
        } catch (e: SecurityException) {
            Log.w(TAG, "Cannot schedule exact alarm: ${e.message}")
            // フォールバック: 非正確なアラーム
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }
    }

    /**
     * スケジュールされたアラームをキャンセル
     */
    fun cancel() {
        val intent = Intent(appContext, FeedbackAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pendingIntent)
        Log.d(TAG, "Alarm cancelled")
    }

    /**
     * 現在のスケジュール状態を確認
     */
    fun isScheduled(): Boolean {
        val intent = Intent(appContext, FeedbackAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            appContext,
            REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        return pendingIntent != null
    }
}
