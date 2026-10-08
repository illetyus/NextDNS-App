package com.example

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.example.data.notifications.NotificationCenter
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NativeNotificationPermissionTest {
  @Test fun android13PermissionDenialPreventsPrivateNotifications() {
    assumeTrue("This permission scenario requires Android 13+", Build.VERSION.SDK_INT >= 33)
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val context = instrumentation.targetContext
    instrumentation.uiAutomation.revokeRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
    NotificationCenter.createChannels(context)
    val manager = context.getSystemService(NotificationManager::class.java)
    manager.cancelAll()
    assertEquals(Notification.VISIBILITY_PRIVATE, manager.getNotificationChannel(NotificationCenter.CHANNEL_CONFIG_CHANGES).lockscreenVisibility)
    assertFalse(NotificationCenter.canPost(context, NotificationCenter.CHANNEL_CONFIG_CHANGES))
    assertFalse(NotificationCenter.postConfigChanged(context))
    assertTrue(manager.activeNotifications.isEmpty())
  }
}
