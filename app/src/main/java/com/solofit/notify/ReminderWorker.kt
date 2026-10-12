package com.solofit.notify

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.Worker
import androidx.work.WorkerParameters
import com.solofit.MainActivity
import com.solofit.R
import com.solofit.data.PlayerRepository
import java.time.LocalDate
import java.time.LocalTime
import java.util.concurrent.TimeUnit

/** Периодически проверяет, выполнен ли квест, и после «часа X» напоминает о штрафе. */
class ReminderWorker(context: Context, params: WorkerParameters) : Worker(context, params) {

    override fun doWork(): Result {
        val state = PlayerRepository(applicationContext).load()
        if (!state.accepted) return Result.success()

        val q = state.quest
        val doneToday = q.date == LocalDate.now().toString() && (q.completed || q.rested)
        val hour = LocalTime.now().hour
        if (doneToday || hour < state.reminderHour) return Result.success()

        val text = if (hour >= 22) {
            "До полуночи осталось мало времени. Штрафная зона ждёт тех, кто не выполнил квест."
        } else {
            "Ежедневный квест не выполнен. Невыполнение приведёт к штрафу."
        }
        notify("[Система] Игрок ${state.name}", text)
        return Result.success()
    }

    private fun notify(title: String, text: String) {
        val ctx = applicationContext
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = ctx.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "Напоминания Системы", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val open = PendingIntent.getActivity(
            ctx, 0, Intent(ctx, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID, n)
    }

    companion object {
        private const val CHANNEL_ID = "system_reminders"
        private const val NOTIFICATION_ID = 1

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ReminderWorker>(2, TimeUnit.HOURS).build()
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "daily_quest_reminder", ExistingPeriodicWorkPolicy.KEEP, request
            )
        }
    }
}
