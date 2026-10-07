package com.example.data.repository

enum class SyncSection {
  SETUP,
  SECURITY,
  PRIVACY,
  PARENTAL,
  DENYLIST,
  ALLOWLIST,
  SETTINGS,
  ACCOUNT
}

data class SectionSyncState(
  val isRefreshing: Boolean = false,
  val lastAttemptAt: Long? = null,
  val lastSuccessAt: Long? = null,
  val errorMessage: String? = null
)
