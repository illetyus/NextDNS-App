package com.example.data.legal

import android.content.Context

/**
 * Device-local acceptance of a specific Terms revision. This is not consent
 * to personal-data processing and does not transmit a receipt to a server.
 */
class LegalAcceptanceStore(context: Context) {
  private val preferences = context.applicationContext.getSharedPreferences(
    "legal_acceptance", Context.MODE_PRIVATE
  )

  fun isAccepted(): Boolean =
    preferences.getString(ACCEPTED_TERMS_REVISION, null) == CURRENT_TERMS_REVISION

  /** Persist synchronously, so the account/network gate cannot open on failed storage. */
  fun acceptCurrentTerms(): Boolean =
    preferences.edit()
      .putString(ACCEPTED_TERMS_REVISION, CURRENT_TERMS_REVISION)
      .commit()

  companion object {
    // DRAFT ONLY: increment whenever the final legal text is materially revised.
    // Release must not use this provisional revision or unreviewed legal assets.
    const val CURRENT_TERMS_REVISION = "terms-2026-10-DRAFT-1"
    private const val ACCEPTED_TERMS_REVISION = "accepted_terms_revision"
  }
}
