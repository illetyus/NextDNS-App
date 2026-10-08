package com.example.data.legal

import android.content.Context

/**
 * Device-local acceptance of a specific Terms revision. This is not consent
 * to personal-data processing and does not transmit a receipt to a server.
 */
class LegalAcceptanceStore(
  context: Context,
  private val clock: () -> Long = { System.currentTimeMillis() }
) {
  private val preferences = context.applicationContext.getSharedPreferences(
    "legal_acceptance", Context.MODE_PRIVATE
  )

  fun isAccepted(): Boolean =
    preferences.getString(ACCEPTED_TERMS_REVISION, null) == CURRENT_TERMS_REVISION &&
      preferences.getLong(ACCEPTED_AT, 0L) > 0L

  fun acceptedAtEpochMillis(): Long? =
    if (isAccepted()) preferences.getLong(ACCEPTED_AT, 0L) else null

  /** Persist synchronously, so the account/network gate cannot open on failed storage. */
  fun acceptCurrentTerms(): Boolean {
    val acceptedAt = clock().takeIf { it > 0L } ?: return false
    return preferences.edit()
      .putString(ACCEPTED_TERMS_REVISION, CURRENT_TERMS_REVISION)
      .putLong(ACCEPTED_AT, acceptedAt)
      .commit()
  }

  companion object {
    // DRAFT ONLY: increment whenever the final legal text is materially revised.
    // Release must not use this provisional revision or unreviewed legal assets.
    const val CURRENT_TERMS_REVISION = "terms-2026-10-DRAFT-2"
    private const val ACCEPTED_TERMS_REVISION = "accepted_terms_revision"
    private const val ACCEPTED_AT = "accepted_at_epoch_millis"
  }
}
