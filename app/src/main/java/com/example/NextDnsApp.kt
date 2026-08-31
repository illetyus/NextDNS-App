package com.example

import android.app.Application
import com.example.data.local.NextDnsPreferences

class NextDnsApp : Application() {
  override fun onCreate() {
    super.onCreate()
    instance = this
    preferences = NextDnsPreferences.getInstance(this)
  }

  companion object {
    lateinit var instance: NextDnsApp
      private set
    lateinit var preferences: NextDnsPreferences
      private set
  }
}
