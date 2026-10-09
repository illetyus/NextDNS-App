package com.example.i18n

import com.example.R

/** Existing cache/codec values stay stable; only their presentation is translated. */
object UiLabels {
  private val labels = mapOf(
    "Tüm cihazlar" to R.string.all_devices,
    "Son 24 saat" to R.string.last_24_hours,
    "Son 7 gün" to R.string.last_7_days,
    "Son 30 gün" to R.string.last_30_days,
    "Son 90 gün" to R.string.last_90_days,
    "Son 3 ay" to R.string.last_3_months,
    "6 saat" to R.string.retention_6_hours,
    "1 gün" to R.string.retention_1_day,
    "1 hafta" to R.string.retention_1_week,
    "1 ay" to R.string.retention_1_month,
    "3 ay" to R.string.retention_3_months,
    "6 ay" to R.string.retention_6_months,
    "1 yıl" to R.string.retention_1_year,
    "2 yıl" to R.string.retention_2_years,
    "İsviçre" to R.string.storage_ch,
    "İsviçre (CH)" to R.string.storage_ch,
    "Avrupa Birliği (AB)" to R.string.storage_eu,
    "Amerika Birleşik Devletleri (ABD)" to R.string.storage_us,
    "Bilinmeyen Cihaz" to R.string.ui_571961518d,
    "Genel" to R.string.ui_0f1322006d
  )

  fun canonical(value: String): String = labels[value]?.let { AppStrings.get(it) } ?: value
}
