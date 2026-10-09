package com.example

import android.app.Application
import com.example.i18n.LocalePreferences
import com.example.data.local.NextDnsPreferences
import com.example.data.notifications.NotificationCenter
import com.example.data.notifications.NotificationWorkScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NextDnsApp : Application() {
  private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

  override fun onCreate() {
    super.onCreate()
    instance = this
    LocalePreferences.migrateToPlatform(this)
    preferences = NextDnsPreferences.getInstance(this)

    NotificationCenter.createChannels(this)
    appScope.launch {
      runCatching {
        NotificationWorkScheduler.reconcile(this@NextDnsApp)
      }
    }
  }

  companion object {
    lateinit var instance: NextDnsApp
      private set
    lateinit var preferences: NextDnsPreferences
      private set
  }
}
