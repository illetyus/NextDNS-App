package com.example.data.notifications

import android.Manifest
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
import com.example.MainActivity
import com.example.R

object NotificationCenter {
  const val CHANNEL_CONFIG_CHANGES = "nextdns_config_changes"
  const val CHANNEL_DAILY_SUMMARY = "nextdns_daily_summary"

  private const val NOTIFICATION_CONFIG_CHANGED = 2101
  private const val NOTIFICATION_DAILY_SUMMARY = 2102

  fun createChannels(context: Context) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

    val manager = context.getSystemService(NotificationManager::class.java)

    val changeChannel = NotificationChannel(
      CHANNEL_CONFIG_CHANGES,
      "Ayar değişiklikleri",
      NotificationManager.IMPORTANCE_DEFAULT
    ).apply {
      description = "NextDNS sunucu yapılandırmasında değişiklik algılandığında bildirir."
      lockscreenVisibility = Notification.VISIBILITY_PRIVATE
    }

    val summaryChannel = NotificationChannel(
      CHANNEL_DAILY_SUMMARY,
      "Günlük özet",
      NotificationManager.IMPORTANCE_LOW
    ).apply {
      description = "Son 24 saatlik DNS sorgu ve engelleme özetini gösterir."
      lockscreenVisibility = Notification.VISIBILITY_PRIVATE
    }

    manager.createNotificationChannels(listOf(changeChannel, summaryChannel))
  }

  fun canPost(context: Context, channelId: String): Boolean {
    if (
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
      ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      return false
    }

    if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
      return false
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val manager = context.getSystemService(NotificationManager::class.java)
      val channel = manager.getNotificationChannel(channelId)
      if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) {
        return false
      }
    }

    return true
  }

  fun postConfigChanged(context: Context): Boolean {
    if (!canPost(context, CHANNEL_CONFIG_CHANGES)) return false

    val notification = baseBuilder(
      context = context,
      channelId = CHANNEL_CONFIG_CHANGES,
      title = "NextDNS ayarları değişti",
      text = "Etkin profilin sunucu yapılandırmasında bir değişiklik algılandı.",
      priority = NotificationCompat.PRIORITY_DEFAULT
    ).build()

    NotificationManagerCompat.from(context)
      .notify(NOTIFICATION_CONFIG_CHANGED, notification)
    return true
  }

  fun postDailySummary(
    context: Context,
    totalQueries: Long,
    blockedQueries: Long
  ): Boolean {
    if (!canPost(context, CHANNEL_DAILY_SUMMARY)) return false

    val text = "Son 24 saatte %,d DNS sorgusu işlendi; %,d tanesi engellendi."
      .format(totalQueries, blockedQueries)

    val notification = baseBuilder(
      context = context,
      channelId = CHANNEL_DAILY_SUMMARY,
      title = "NextDNS günlük özeti",
      text = text,
      priority = NotificationCompat.PRIORITY_LOW
    ).build()

    NotificationManagerCompat.from(context)
      .notify(NOTIFICATION_DAILY_SUMMARY, notification)
    return true
  }

  private fun baseBuilder(
    context: Context,
    channelId: String,
    title: String,
    text: String,
    priority: Int
  ): NotificationCompat.Builder {
    val intent = Intent(context, MainActivity::class.java)
    val pendingIntent = PendingIntent.getActivity(
      context,
      0,
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val publicVersion = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_notification_dns)
      .setContentTitle("NextDNS bildirimi")
      .setContentText("Ayrıntılar için uygulamayı açın.")
      .setPriority(priority)
      .build()

    return NotificationCompat.Builder(context, channelId)
      .setSmallIcon(R.drawable.ic_notification_dns)
      .setContentTitle(title)
      .setContentText(text)
      .setStyle(NotificationCompat.BigTextStyle().bigText(text))
      .setContentIntent(pendingIntent)
      .setAutoCancel(true)
      .setOnlyAlertOnce(true)
      .setPriority(priority)
      .setCategory(NotificationCompat.CATEGORY_STATUS)
      .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
      .setPublicVersion(publicVersion)
  }
}
