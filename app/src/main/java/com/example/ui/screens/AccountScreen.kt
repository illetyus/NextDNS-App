package com.example.ui.screens

import com.example.R
import com.example.i18n.AppStrings

import android.content.ClipData
import android.content.ClipboardManager
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PersistableBundle
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.NextDnsProfile
import com.example.data.repository.ApiConnectionStatus
import com.example.ui.components.*
import com.example.ui.viewmodel.NextDnsViewModel
import kotlinx.coroutines.delay

@Composable
fun AccountScreen(
  viewModel: NextDnsViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activity = remember(context) { context.findActivity() }
  val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
  val hasApiKey by viewModel.hasApiKey.collectAsStateWithLifecycle()
  val apiStatus by viewModel.apiStatus.collectAsStateWithLifecycle()
  val profiles by viewModel.profiles.collectAsStateWithLifecycle()
  val activeProfileId by viewModel.activeProfileId.collectAsStateWithLifecycle()
  val testResult by viewModel.testResult.collectAsStateWithLifecycle()
  val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
  val analytics by viewModel.analytics.collectAsStateWithLifecycle()
  val analyticsLastSuccessAt by viewModel.analyticsLastSuccessAt.collectAsStateWithLifecycle()
  val analyticsErrorMessage by viewModel.analyticsErrorMessage.collectAsStateWithLifecycle()
  val isAnalyticsLoading by viewModel.isAnalyticsLoading.collectAsStateWithLifecycle()

  var showLogoutConfirm by remember { mutableStateOf(false) }
  var showNewProfileDialog by remember { mutableStateOf(false) }
  var newProfileName by remember { mutableStateOf("") }
  var showApiKey by remember { mutableStateOf(false) }

  LaunchedEffect(activeProfileId) {
    if (activeProfileId.isNotBlank()) {
      viewModel.refreshAnalytics(device = null, time = null)
    }
  }

  LaunchedEffect(showApiKey) {
    if (showApiKey) {
      delay(10_000L)
      showApiKey = false
    }
  }

  DisposableEffect(showApiKey, activity) {
    if (showApiKey) {
      activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
    }

    onDispose {
      if (showApiKey) {
        activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
      }
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
    contentPadding = PaddingValues(vertical = 16.dp)
  ) {
    // 2. Real Live Usage / Query Metrics Card
    item {
      NextDnsCard(
        title = AppStrings.get(R.string.ui_c636e5398b),
        subtitle = AppStrings.get(R.string.ui_de9c0c50cb)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            Surface(
              modifier = Modifier.weight(1f),
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = AppStrings.get(R.string.ui_c8fbd2d7db),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.sp
                )
                Text(
                  text = analyticsLastSuccessAt?.let { "%,d".format(analytics.totalQueries) } ?: "—",
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Surface(
              modifier = Modifier.weight(1f),
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = AppStrings.get(R.string.ui_8757f8e456),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.sp
                )
                Text(
                  text = analyticsLastSuccessAt?.let { "%,d".format(analytics.blockedQueries) } ?: "—",
                  color = MaterialTheme.colorScheme.error,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }

            Surface(
              modifier = Modifier.weight(1f),
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
            ) {
              Column(modifier = Modifier.padding(12.dp)) {
                Text(
                  text = AppStrings.get(R.string.ui_0f7cd99d61),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.sp
                )
                Text(
                  text = analyticsLastSuccessAt?.let { "%%%d".format(analytics.blockedPercentage.toInt()) } ?: "—",
                  color = MaterialTheme.colorScheme.tertiary,
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          if (analytics.topDevices.isNotEmpty()) {
            Text(
              text = AppStrings.get(R.string.active_devices, analytics.topDevices.size, analytics.topDevices.take(3).joinToString { it.name }),
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.5.sp
            )
          }

          if (analyticsLastSuccessAt == null) {
            Text(
              text = when {
                isAnalyticsLoading -> AppStrings.get(R.string.ui_f227ea13fc)
                !analyticsErrorMessage.isNullOrBlank() -> analyticsErrorMessage!!
                else -> AppStrings.get(R.string.ui_fa93b503d4)
              },
              color = if (!analyticsErrorMessage.isNullOrBlank() && !isAnalyticsLoading) {
                MaterialTheme.colorScheme.error
              } else {
                MaterialTheme.colorScheme.onSurfaceVariant
              },
              fontSize = 11.5.sp
            )
          }
        }
      }
    }

    // 2. API Key Card
    item {
      NextDnsCard(
        title = AppStrings.get(R.string.ui_f9b1cde612),
        subtitle = AppStrings.get(R.string.ui_4400dad370)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Text(
                text = if (!hasApiKey) {
                  AppStrings.get(R.string.ui_ebf30ef1ee)
                } else if (showApiKey) {
                  viewModel.currentApiKeyForSensitiveUse()
                } else {
                  viewModel.maskedApiKey()
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
              )

              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                  onClick = { showApiKey = !showApiKey },
                  modifier = Modifier.size(48.dp)
                ) {
                  Icon(
                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = AppStrings.get(R.string.toggle_visibility),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                  )
                }

                if (hasApiKey) {
                  IconButton(
                    onClick = {
                      val apiKey = viewModel.currentApiKeyForSensitiveUse()
                      copySensitiveApiKey(context, apiKey)
                      scheduleApiKeyClipboardClear(context, apiKey)
                    },
                    modifier = Modifier.size(48.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.ContentCopy,
                      contentDescription = AppStrings.get(R.string.ui_a8bcca42d9),
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(18.dp)
                    )
                  }
                }
              }
            }
          }

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            NextDnsButton(
              text = if (isSyncing) AppStrings.get(R.string.ui_d5ca89c99b) else AppStrings.get(R.string.ui_8b96719687),
              onClick = { viewModel.syncAllData() },
              icon = Icons.Default.Sync,
              modifier = Modifier.weight(1f)
            )
          }
        }
      }
    }

    // 3. Profiles Overview Card
    item {
      NextDnsCard(
        title = AppStrings.get(R.string.profiles_count, profiles.size),
        subtitle = AppStrings.get(R.string.ui_473fdfc82e)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          profiles.forEach { profile ->
            val isActive = profile.id == activeProfileId
            Surface(
              color = if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(
                1.dp,
                if (isActive) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
              ),
              modifier = Modifier
                .fillMaxWidth()
                .clickable {
                  if (!isActive) viewModel.switchProfile(profile.id)
                }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                  Box(
                    modifier = Modifier
                      .size(8.dp)
                      .background(
                        if (isActive) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline,
                        CircleShape
                      )
                  )
                  Column {
                    Text(
                      text = profile.name,
                      color = MaterialTheme.colorScheme.onSurface,
                      fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                      fontSize = 13.5.sp
                    )
                    Text(
                      text = "ID: ${profile.id}",
                      fontFamily = FontFamily.Monospace,
                      color = MaterialTheme.colorScheme.onSurfaceVariant,
                      fontSize = 11.5.sp
                    )
                  }
                }

                if (isActive) {
                  Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = AppStrings.get(R.string.ui_66100986ec),
                      color = MaterialTheme.colorScheme.tertiary,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }
            }
          }

          NextDnsButton(
            text = AppStrings.get(R.string.ui_ae56761330),
            onClick = { showNewProfileDialog = true },
            icon = Icons.Default.Add,
            modifier = Modifier.fillMaxWidth()
          )
        }
      }
    }

    // 4. Live DNS Diagnostics
    item {
      NextDnsCard(
        title = AppStrings.get(R.string.ui_33cbc60e2e),
        subtitle = AppStrings.get(R.string.ui_ed7561918d)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          val rawStatus = testResult.status.lowercase().trim()
          val isUsingNextDns = rawStatus == "ok" || rawStatus == "using_nextdns" || rawStatus == "configured" || testResult.serverPoP.isNotBlank()

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column {
              Text(
                text = if (isUsingNextDns) AppStrings.get(R.string.ui_8c25d923c8) else AppStrings.get(R.string.ui_a13d9a7458),
                color = if (isUsingNextDns) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
              )
              if (testResult.serverPoP.isNotBlank()) {
                Text(
                  text = AppStrings.get(R.string.server_latency, testResult.serverPoP, testResult.protocol, testResult.latencyMs),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.5.sp
                )
              }
            }

            NextDnsButton(
              text = if (testResult.isTesting) "TEST..." else AppStrings.get(R.string.ui_e4f71092ee),
              onClick = { viewModel.runDiagnostic(showToast = true) },
              icon = Icons.Default.Refresh
            )
          }
        }
      }
    }

    // 5. Account Management / Web Link & Logout
    item {
      NextDnsCard(
        title = AppStrings.get(R.string.ui_c917752692),
        subtitle = AppStrings.get(R.string.ui_0ff43760d0)
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedButton(
            onClick = {
              val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://my.nextdns.io/account"))
              context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.outlinedButtonColors(
              contentColor = MaterialTheme.colorScheme.onSurface
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
          ) {
            Icon(
              imageVector = Icons.Default.OpenInNew,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(AppStrings.get(R.string.ui_0a2d9890c6), fontSize = 12.5.sp)
          }

          Button(
            onClick = { showLogoutConfirm = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError
            )
          ) {
            Icon(
              imageVector = Icons.Default.Logout,
              contentDescription = null,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(AppStrings.get(R.string.ui_283092d12a), fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }

  // Logout Dialog
  if (showLogoutConfirm) {
    AlertDialog(
      onDismissRequest = { showLogoutConfirm = false },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(16.dp),
      title = {
        Text(AppStrings.get(R.string.ui_984bb24efa), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          AppStrings.get(R.string.ui_4448d81aad),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 13.sp
        )
      },
      confirmButton = {
        Button(
          onClick = {
            showLogoutConfirm = false
            viewModel.logout()
          },
          colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
        ) {
          Text(AppStrings.get(R.string.ui_283092d12a), color = MaterialTheme.colorScheme.onError)
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutConfirm = false }) {
          Text(AppStrings.get(R.string.ui_7227874813), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    )
  }

  // New Profile Dialog
  if (showNewProfileDialog) {
    AlertDialog(
      onDismissRequest = { showNewProfileDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(16.dp),
      title = {
        Text(AppStrings.get(R.string.ui_cff3a955d7), color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
      },
      text = {
        OutlinedTextField(
          value = newProfileName,
          onValueChange = { newProfileName = it },
          label = { Text(AppStrings.get(R.string.ui_249cb2491d)) },
          placeholder = { Text(AppStrings.get(R.string.ui_b75d4ec373)) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        Button(
          onClick = {
            if (newProfileName.isNotBlank()) {
              viewModel.createProfile(newProfileName.trim())
              newProfileName = ""
              showNewProfileDialog = false
            }
          },
          enabled = newProfileName.isNotBlank()
        ) {
          Text(AppStrings.get(R.string.ui_89c75b03b9))
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewProfileDialog = false }) {
          Text(AppStrings.get(R.string.ui_7227874813), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    )
  }

}


private fun copySensitiveApiKey(context: Context, apiKey: String) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val clip = ClipData.newPlainText(AppStrings.get(R.string.ui_92e563511e), apiKey)

  clip.description.extras = PersistableBundle().apply {
    putBoolean("android.content.extra.IS_SENSITIVE", true)
  }

  clipboard.setPrimaryClip(clip)

  if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2) {
    Toast.makeText(context, AppStrings.get(R.string.ui_41809d48c3), Toast.LENGTH_SHORT).show()
  }
}

private fun scheduleApiKeyClipboardClear(
  context: Context,
  expectedApiKey: String
) {
  val appContext = context.applicationContext
  Handler(Looper.getMainLooper()).postDelayed(
    { clearApiKeyClipboardIfUnchanged(appContext, expectedApiKey) },
    30_000L
  )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
  is Activity -> this
  is ContextWrapper -> baseContext.findActivity()
  else -> null
}

private fun clearApiKeyClipboardIfUnchanged(
  context: Context,
  expectedApiKey: String
) {
  val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
  val current = clipboard.primaryClip
    ?.takeIf { it.itemCount > 0 }
    ?.getItemAt(0)
    ?.coerceToText(context)
    ?.toString()

  if (current != expectedApiKey) return

  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
    clipboard.clearPrimaryClip()
  } else {
    clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
  }
}
