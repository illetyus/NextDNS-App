package com.example.data.model

enum class DiagnosticConnectionState {
  TESTING, ERROR, UNCONFIGURED, MATCHED_PROFILE, UNVERIFIED_PROFILE, NO_SELECTED_PROFILE
}

fun diagnosticConnectionState(
  selectedProfileId: String?,
  result: DiagnosticTestResult,
  running: Boolean = false
): DiagnosticConnectionState = when {
  running || result.isTesting -> DiagnosticConnectionState.TESTING
  result.errorMessage != null || result.status.equals("error", true) -> DiagnosticConnectionState.ERROR
  result.lastTestedTime <= 0L -> DiagnosticConnectionState.UNCONFIGURED
  result.status.lowercase().trim() !in setOf("ok", "using-nextdns") -> DiagnosticConnectionState.UNCONFIGURED
  selectedProfileId.isNullOrBlank() -> DiagnosticConnectionState.NO_SELECTED_PROFILE
  !result.profileId.trim().equals(selectedProfileId.trim(), true) -> DiagnosticConnectionState.UNVERIFIED_PROFILE
  else -> DiagnosticConnectionState.MATCHED_PROFILE
}
