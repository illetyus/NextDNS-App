package com.example.data.notifications

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object NotificationWorkScheduler {
  private const val PERIODIC_WORK_NAME = "nextdns_notification_monitor"
  private const val IMMEDIATE_WORK_NAME = "nextdns_notification_monitor_now"

  suspend fun reconcile(
    context: Context,
    runImmediately: Boolean = false
  ) {
    val appContext = context.applicationContext
    val preferences = NotificationPreferences(appContext)
    val settings = preferences.currentSettings()
    val workManager = WorkManager.getInstance(appContext)

    if (!settings.anyEnabled) {
      workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
      workManager.cancelUniqueWork(IMMEDIATE_WORK_NAME)
      return
    }

    val constraints = Constraints.Builder()
      .setRequiredNetworkType(NetworkType.CONNECTED)
      .setRequiresBatteryNotLow(true)
      .build()

    val periodic = PeriodicWorkRequestBuilder<NextDnsNotificationWorker>(
      NotificationPolicy.PERIODIC_INTERVAL_MINUTES,
      TimeUnit.MINUTES
    )
      .setConstraints(constraints)
      .setBackoffCriteria(
        BackoffPolicy.EXPONENTIAL,
        10,
        TimeUnit.MINUTES
      )
      .build()

    workManager.enqueueUniquePeriodicWork(
      PERIODIC_WORK_NAME,
      ExistingPeriodicWorkPolicy.UPDATE,
      periodic
    )

    if (runImmediately) {
      val immediate = OneTimeWorkRequestBuilder<NextDnsNotificationWorker>()
        .setConstraints(constraints)
        .setBackoffCriteria(
          BackoffPolicy.EXPONENTIAL,
          10,
          TimeUnit.MINUTES
        )
        .build()

      workManager.enqueueUniqueWork(
        IMMEDIATE_WORK_NAME,
        ExistingWorkPolicy.REPLACE,
        immediate
      )
    }
  }
}
