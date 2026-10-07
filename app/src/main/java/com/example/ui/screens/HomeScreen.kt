package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.ui.components.NextDnsButton
import com.example.ui.components.StatusBeacon
import com.example.ui.components.bounceClick
import com.example.ui.theme.*
import com.example.ui.viewmodel.NavTab
import com.example.ui.viewmodel.NextDnsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
  viewModel: NextDnsViewModel,
  modifier: Modifier = Modifier
) {
  val currentTab by viewModel.currentTab.collectAsState()
  val currentSectionSyncState by viewModel.currentSectionSyncState.collectAsState()
  val activeProfile by viewModel.activeProfile.collectAsState()
  val profiles by viewModel.profiles.collectAsState()
  val apiKey by viewModel.apiKey.collectAsState()
  val uiMessage by viewModel.uiMessage.collectAsState()
  val haptic = LocalHapticFeedback.current

  var showProfileMenu by remember { mutableStateOf(false) }
  var showAccountMenu by remember { mutableStateOf(false) }
  var showNewProfileDialog by remember { mutableStateOf(false) }
  var newProfileNameInput by remember { mutableStateOf("") }
  val snackbarHostState = remember { SnackbarHostState() }
  val lifecycleOwner = LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> viewModel.startForegroundProfileSync()
        Lifecycle.Event.ON_PAUSE -> viewModel.stopForegroundProfileSync()
        else -> Unit
      }
    }

    lifecycleOwner.lifecycle.addObserver(observer)
    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      viewModel.startForegroundProfileSync()
    }

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      viewModel.stopForegroundProfileSync()
    }
  }

  DisposableEffect(lifecycleOwner, currentTab) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> viewModel.startVisibleTabSync(currentTab)
        Lifecycle.Event.ON_PAUSE -> viewModel.stopVisibleTabSync()
        else -> Unit
      }
    }

    lifecycleOwner.lifecycle.addObserver(observer)
    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      viewModel.startVisibleTabSync(currentTab)
    }

    onDispose {
      lifecycleOwner.lifecycle.removeObserver(observer)
      viewModel.stopVisibleTabSync()
    }
  }

  LaunchedEffect(uiMessage) {
    uiMessage?.let {
      snackbarHostState.showSnackbar(
        message = it.text,
        duration = SnackbarDuration.Short
      )
      viewModel.dismissMessage()
    }
  }

  Scaffold(
    modifier = modifier.fillMaxSize(),
    containerColor = MaterialTheme.colorScheme.background,
    topBar = {
      Surface(
        color = MaterialTheme.colorScheme.background,
        modifier = Modifier.statusBarsPadding()
      ) {
        Column(modifier = Modifier.fillMaxWidth()) {
          // --- NextDNS Expressive Top Bar ---
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            // Left: NextDNS Shield Logo + Profile Selector
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
              // Animated Pulsing Shield Logo
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .bounceClick(scaleDown = 0.95f) {
                    viewModel.syncAllData()
                  }
                  .padding(vertical = 4.dp, horizontal = 2.dp)
              ) {
                Box(contentAlignment = Alignment.Center) {
                  StatusBeacon(color = MaterialTheme.colorScheme.primary, size = 12.dp, isPulsing = true)
                  Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "NextDNS Logo",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp)
                  )
                }
                Text(
                  text = "NextDNS",
                  style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    letterSpacing = (-0.4).sp
                  ),
                  color = MaterialTheme.colorScheme.onSurface
                )
              }

              // Profile Selector Dropdown Box
              Box {
                Surface(
                  modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .bounceClick(scaleDown = 0.94f) { showProfileMenu = true },
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.tertiary)
                    )
                    Text(
                      text = activeProfile?.name ?: "Profil Seç",
                      style = MaterialTheme.typography.bodySmall.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                      ),
                      color = MaterialTheme.colorScheme.onSurface,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                      modifier = Modifier.widthIn(max = 110.dp)
                    )
                    Icon(
                      imageVector = Icons.Default.ArrowDropDown,
                      contentDescription = null,
                      tint = MaterialTheme.colorScheme.onSurfaceVariant,
                      modifier = Modifier.size(16.dp)
                    )
                  }
                }

                DropdownMenu(
                  expanded = showProfileMenu,
                  onDismissRequest = { showProfileMenu = false },
                  modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
                ) {
                  profiles.forEach { prof ->
                    val isCurrent = prof.id == activeProfile?.id
                    DropdownMenuItem(
                      text = {
                        Row(
                          modifier = Modifier.fillMaxWidth(),
                          horizontalArrangement = Arrangement.SpaceBetween,
                          verticalAlignment = Alignment.CenterVertically
                        ) {
                          Column {
                            Text(
                              text = prof.name,
                              color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                              fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                              fontSize = 13.sp
                            )
                            Text(
                              text = "ID: ${prof.id}",
                              color = MaterialTheme.colorScheme.outline,
                              fontSize = 10.sp
                            )
                          }
                          if (isCurrent) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                              imageVector = Icons.Default.Check,
                              contentDescription = "Seçili",
                              tint = MaterialTheme.colorScheme.primary,
                              modifier = Modifier.size(14.dp)
                            )
                          }
                        }
                      },
                      onClick = {
                        viewModel.switchProfile(prof.id)
                        showProfileMenu = false
                      }
                    )
                  }
                  HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                  DropdownMenuItem(
                    text = {
                      Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                      ) {
                        Icon(
                          imageVector = Icons.Default.Add,
                          contentDescription = null,
                          tint = MaterialTheme.colorScheme.primary,
                          modifier = Modifier.size(14.dp)
                        )
                        Text(
                          text = "Yeni Profil Oluştur",
                          color = MaterialTheme.colorScheme.primary,
                          fontSize = 12.sp,
                          fontWeight = FontWeight.Bold
                        )
                      }
                    },
                    onClick = {
                      showProfileMenu = false
                      showNewProfileDialog = true
                    }
                  )
                }
              }
            }

            // Right: User Account Dropdown Box
            Box {
              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(10.dp))
                  .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                  .bounceClick(scaleDown = 0.94f) { showAccountMenu = true },
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                  Icon(
                    imageVector = if (apiKey.isNotBlank()) Icons.Default.AccountCircle else Icons.Default.PersonOutline,
                    contentDescription = null,
                    tint = if (apiKey.isNotBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = if (apiKey.isNotBlank()) "Hesabım" else "Misafir",
                    style = MaterialTheme.typography.bodySmall.copy(
                      fontWeight = FontWeight.Medium,
                      fontSize = 11.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                  )
                  Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                  )
                }
              }

              DropdownMenu(
                expanded = showAccountMenu,
                onDismissRequest = { showAccountMenu = false },
                modifier = Modifier
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
              ) {
                DropdownMenuItem(
                  text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Icon(Icons.Default.Sync, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                      Text("Verileri Yenile", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                  },
                  onClick = {
                    showAccountMenu = false
                    viewModel.syncAllData()
                  }
                )
                DropdownMenuItem(
                  text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Icon(Icons.Default.Speed, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(16.dp))
                      Text("Tanı Testi Çalıştır", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
                    }
                  },
                  onClick = {
                    showAccountMenu = false
                    viewModel.runDiagnostic()
                  }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.outline)
                DropdownMenuItem(
                  text = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                      Icon(Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(16.dp))
                      Text("Çıkış Yap", color = MaterialTheme.colorScheme.error, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                  },
                  onClick = {
                    showAccountMenu = false
                    viewModel.logout()
                  }
                )
              }
            }
          }

          // --- Android 16 Expressive Segmented Navigation Tabs ---
          val tabScrollState = rememberScrollState()
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .horizontalScroll(tabScrollState)
              .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
              NavTab.values().forEach { tab ->
              val isSelected = currentTab == tab
              val tabBgColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f) else Color.Transparent,
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "tabBg"
              )
              val tabBorderColor by animateColorAsState(
                targetValue = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                animationSpec = spring(stiffness = Spring.StiffnessLow),
                label = "tabBorder"
              )

              Surface(
                modifier = Modifier
                  .clip(RoundedCornerShape(12.dp))
                  .border(1.dp, tabBorderColor, RoundedCornerShape(12.dp))
                  .bounceClick(scaleDown = 0.94f) {
                    viewModel.selectTab(tab)
                  }
                  .testTag("tab_${tab.name.lowercase()}"),
                color = tabBgColor,
                shape = RoundedCornerShape(12.dp)
              ) {
                Row(
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                  Icon(
                    imageVector = getTabIcon(tab),
                    contentDescription = null,
                    tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                  )
                  Text(
                    text = tab.title,
                    style = MaterialTheme.typography.bodySmall.copy(
                      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                      fontSize = 12.sp
                    ),
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
              }
            }
          }

          currentSectionSyncState?.let { syncState ->
            val syncText = when {
              syncState.isSaving -> "NextDNS'e kaydediliyor…"
              syncState.isRefreshing -> "Sunucuyla eşitleniyor…"
              syncState.errorMessage != null -> {
                val lastOk = syncState.lastSuccessAt?.let(::formatSyncTime) ?: "yok"
                "Eşitleme başarısız • son başarılı: $lastOk"
              }
              syncState.lastSuccessAt != null -> "Sunucudan güncel • ${formatSyncTime(syncState.lastSuccessAt)}"
              else -> "Henüz sunucudan doğrulanmadı"
            }

            val syncColor = when {
              syncState.errorMessage != null -> MaterialTheme.colorScheme.error
              syncState.isSaving || syncState.isRefreshing -> MaterialTheme.colorScheme.primary
              else -> MaterialTheme.colorScheme.onSurfaceVariant
            }

            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 3.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              if (syncState.isSaving || syncState.isRefreshing) {
                CircularProgressIndicator(
                  modifier = Modifier.size(12.dp),
                  strokeWidth = 1.5.dp,
                  color = MaterialTheme.colorScheme.primary
                )
              } else {
                Icon(
                  imageVector = if (syncState.errorMessage != null) Icons.Default.Warning else Icons.Default.CloudDone,
                  contentDescription = null,
                  tint = syncColor,
                  modifier = Modifier.size(13.dp)
                )
              }
              Text(
                text = syncText,
                color = syncColor,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
            }
          }

          HorizontalDivider(color = MaterialTheme.colorScheme.outline, thickness = 1.dp)
        }
      }
    },
    snackbarHost = {
      SnackbarHost(
        hostState = snackbarHostState,
        modifier = Modifier.padding(16.dp)
      ) { data ->
        Snackbar(
          snackbarData = data,
          containerColor = MaterialTheme.colorScheme.surfaceVariant,
          contentColor = MaterialTheme.colorScheme.onSurface,
          shape = RoundedCornerShape(14.dp)
        )
      }
    }
  ) { paddingValues ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(MaterialTheme.colorScheme.background)
        .padding(paddingValues)
    ) {
      AnimatedContent(
        targetState = currentTab,
        transitionSpec = {
          if (targetState.ordinal > initialState.ordinal) {
            (slideInHorizontally { width -> width / 3 } + fadeIn(animationSpec = tween(220)))
              .togetherWith(slideOutHorizontally { width -> -width / 3 } + fadeOut(animationSpec = tween(180)))
          } else {
            (slideInHorizontally { width -> -width / 3 } + fadeIn(animationSpec = tween(220)))
              .togetherWith(slideOutHorizontally { width -> width / 3 } + fadeOut(animationSpec = tween(180)))
          }
        },
        label = "NextDnsTabContent"
      ) { tab ->
        when (tab) {
          NavTab.SETUP -> SetupScreen(viewModel)
          NavTab.SECURITY -> SecurityScreen(viewModel)
          NavTab.PRIVACY -> PrivacyScreen(viewModel)
          NavTab.PARENTAL -> ParentalScreen(viewModel)
          NavTab.DENYLIST -> DenylistScreen(viewModel)
          NavTab.ALLOWLIST -> AllowlistScreen(viewModel)
          NavTab.ANALYTICS -> AnalyticsScreen(viewModel)
          NavTab.LOGS -> LogsScreen(viewModel)
          NavTab.SETTINGS -> SettingsScreen(viewModel)
          NavTab.ACCOUNT -> AccountScreen(viewModel)
        }
      }
    }
  }

  // Dialog: Yeni Profil Oluştur
  if (showNewProfileDialog) {
    AlertDialog(
      onDismissRequest = { showNewProfileDialog = false },
      containerColor = MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(18.dp),
      title = {
        Text(
          text = "Yeni Profil Oluştur",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.onSurface
        )
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          Text(
            text = "Bu profil için bir isim girin:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
          )
          OutlinedTextField(
            value = newProfileNameInput,
            onValueChange = { newProfileNameInput = it },
            placeholder = { Text("örn. Ev Ağı, Telefonum", color = MaterialTheme.colorScheme.outline, fontSize = 13.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary,
              unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedTextColor = MaterialTheme.colorScheme.onSurface,
              unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp)
          )
        }
      },
      confirmButton = {
        NextDnsButton(
          text = "Oluştur",
          onClick = {
            if (newProfileNameInput.isNotBlank()) {
              viewModel.createProfile(newProfileNameInput.trim())
              newProfileNameInput = ""
              showNewProfileDialog = false
            }
          }
        )
      },
      dismissButton = {
        TextButton(onClick = { showNewProfileDialog = false }) {
          Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
      }
    )
  }
}

private fun formatSyncTime(epochMillis: Long): String =
  java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
    .format(java.util.Date(epochMillis))

private fun getTabIcon(tab: NavTab): ImageVector {
  return when (tab) {
    NavTab.SETUP -> Icons.Default.SettingsInputComponent
    NavTab.SECURITY -> Icons.Default.Security
    NavTab.PRIVACY -> Icons.Default.Shield
    NavTab.PARENTAL -> Icons.Default.FamilyRestroom
    NavTab.DENYLIST -> Icons.Default.Block
    NavTab.ALLOWLIST -> Icons.Default.CheckCircle
    NavTab.ANALYTICS -> Icons.Default.BarChart
    NavTab.LOGS -> Icons.Default.Article
    NavTab.SETTINGS -> Icons.Default.Settings
    NavTab.ACCOUNT -> Icons.Default.AccountCircle
  }
}

