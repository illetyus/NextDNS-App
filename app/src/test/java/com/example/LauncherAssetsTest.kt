package com.example

import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [24, 34])
class LauncherAssetsTest {
  @Test fun packagedLauncherAssetsResolveOnLegacyAndAdaptiveAndroid() {
    val context = ApplicationProvider.getApplicationContext<NextDnsApp>()
    for (resource in listOf(R.mipmap.ic_launcher, R.mipmap.ic_launcher_round)) {
      val drawable = context.getDrawable(resource)!!
      assertTrue(drawable.intrinsicWidth > 0 && drawable.intrinsicHeight > 0)
      if (Build.VERSION.SDK_INT >= 26) {
        assertTrue(drawable is AdaptiveIconDrawable)
        val adaptive = drawable as AdaptiveIconDrawable
        assertNotNull(adaptive.foreground)
        assertNotNull(adaptive.background)
        if (Build.VERSION.SDK_INT >= 33) assertNotNull(adaptive.monochrome)
      } else {
        assertTrue(drawable is BitmapDrawable)
        val bitmap = (drawable as BitmapDrawable).bitmap
        assertTrue(bitmap.width >= 48 && bitmap.height >= 48)
      }
    }
    assertNotNull(context.getDrawable(R.drawable.ic_client_network))
  }
}
