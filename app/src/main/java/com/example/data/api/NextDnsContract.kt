package com.example.data.api

object LogRetentionCodec {
  private val labelToSeconds = linkedMapOf(
    "6 saat" to 21_600,
    "1 gün" to 86_400,
    "1 hafta" to 604_800,
    "1 ay" to 2_592_000,
    "3 ay" to 7_776_000,
    "6 ay" to 15_552_000,
    "1 yıl" to 31_536_000,
    "2 yıl" to 63_072_000
  )

  private val secondsToLabel = labelToSeconds.entries.associate { (label, seconds) -> seconds to label }

  fun toSeconds(label: String): Int? = labelToSeconds[label]

  fun toLabel(seconds: Int?): String? = seconds?.let(secondsToLabel::get)
}

fun <T> NextDnsApiResponse<T>?.hasApiErrors(): Boolean =
  this?.errors.orEmpty().isNotEmpty()

fun <T> NextDnsApiResponse<T>?.isSemanticallySuccessful(): Boolean =
  this != null && !hasApiErrors()
