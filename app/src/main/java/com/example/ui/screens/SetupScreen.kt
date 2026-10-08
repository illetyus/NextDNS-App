package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import com.example.data.model.DiagnosticTestResult
import com.example.data.model.NextDnsProfile
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.NextDnsViewModel

@Composable
fun SetupScreen(
  viewModel: NextDnsViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val activeProfile by viewModel.activeProfile.collectAsStateWithLifecycle()
  val testResult by viewModel.testResult.collectAsStateWithLifecycle()
  val isDiagnosticRunning by viewModel.isDiagnosticRunning.collectAsStateWithLifecycle()

  val profileSetup by viewModel.profileSetup.collectAsStateWithLifecycle()

  var selectedPlatform by remember { mutableStateOf("Android") }
  var showAdvancedIpSettings by remember { mutableStateOf(false) }

  val platforms = listOf("Android", "iOS", "Windows", "macOS", "Linux", "ChromeOS", "Tarayıcılar", "Yönlendiriciler")
  val profile = activeProfile ?: NextDnsProfile("", "Profil")
  val profileId = profile.id
  val setup = profileSetup

  // Live polling: initial check on profile change and periodic refresh while on screen
  LaunchedEffect(activeProfile?.id) {
    viewModel.runDiagnostic(showToast = false)
  }

  LaunchedEffect(Unit) {
    while (isActive) {
      delay(15_000L) // 15 seconds live polling
      viewModel.runDiagnostic(showToast = false)
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      ConnectionStatusBanner(
        activeProfile = activeProfile,
        testResult = testResult,
        isDiagnosticRunning = isDiagnosticRunning,
        onRefreshDiagnostic = { viewModel.runDiagnostic(showToast = true) },
        onOpenDnsSettings = {
          try {
            val intent = Intent(Settings.ACTION_WIRELESS_SETTINGS).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
          } catch (_: Exception) {
            try {
              val intent = Intent("android.settings.NETWORK_PROVIDER_SETTINGS").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              }
              context.startActivity(intent)
            } catch (_: Exception) {
              context.startActivity(Intent(Settings.ACTION_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
              })
            }
          }
        }
      )
    }

    item {
      EndpointsSection(
        profileId = profileId,
        context = context
      )
    }

    item {
      LinkedIpSection(
        profileId = profileId,
        setup = setup,
        testResult = testResult,
        showAdvancedIpSettings = showAdvancedIpSettings,
        onToggleAdvanced = { showAdvancedIpSettings = !showAdvancedIpSettings },
        onLinkIp = { viewModel.linkIpAddress(profileId) },
        context = context
      )
    }

    item {
      SetupGuideSection(
        profileId = profileId,
        setup = setup,
        platforms = platforms,
        selectedPlatform = selectedPlatform,
        onPlatformSelected = { selectedPlatform = it },
        context = context
      )
    }
  }
}

// =========================================================================
// Modular Sub-Composables
// =========================================================================

@Composable
private fun ConnectionStatusBanner(
  activeProfile: NextDnsProfile?,
  testResult: DiagnosticTestResult,
  isDiagnosticRunning: Boolean,
  onRefreshDiagnostic: () -> Unit,
  onOpenDnsSettings: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isTesting = testResult.isTesting || isDiagnosticRunning
  val rawStatus = testResult.status.lowercase().trim()
  val isUsingNextDns = rawStatus == "ok" || rawStatus == "using-nextdns"
  val isOffline = rawStatus == "error" || (testResult.errorMessage != null && !isUsingNextDns)

  val borderColor = when {
    isTesting -> MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
    isUsingNextDns -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)
    isOffline -> MaterialTheme.colorScheme.outline
    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.5f)
  }

  val beaconColor = when {
    isTesting -> MaterialTheme.colorScheme.primary
    isUsingNextDns -> MaterialTheme.colorScheme.tertiary
    isOffline -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.error
  }

  val title = when {
    isTesting -> "Bağlantı kontrol ediliyor..."
    isUsingNextDns -> "Her şey yolunda!"
    isOffline -> "Bağlantı kontrol edilemedi"
    else -> "Bu cihaz NextDNS kullanmıyor"
  }

  val subtitle = when {
    isTesting -> "NextDNS test sunucuları ile bağlantı kontrol ediliyor..."
    isUsingNextDns -> "Bu cihaz, NextDNS'i bu profille kullanıyor."
    isOffline -> "NextDNS test sunucusuna ulaşılamadı. İnternet bağlantınızı kontrol edin."
    !testResult.resolver.isNullOrBlank() -> "Mevcut DNS: ${testResult.resolver}. NextDNS'i bu cihazda aktif etmek için aşağıdaki yönergeleri uygulayın."
    else -> "Bu cihaz NextDNS üzerinden yapılandırılmamış. Aşağıdaki kurulum adımlarından birini izleyin."
  }

  val badgeText = when {
    isTesting -> "Test Ediliyor"
    isUsingNextDns -> {
      val lat = if (testResult.latencyMs > 0) "${testResult.latencyMs} ms • " else ""
      val proto = testResult.protocol.takeIf { it.isNotBlank() }
      when {
        proto != null -> "$lat$proto"
        lat.isNotBlank() -> lat.removeSuffix(" • ")
        else -> "Bağlı"
      }
    }
    isOffline -> "Çevrimdışı"
    else -> "Yapılandırılmadı"
  }

  val badgeBg = when {
    isTesting -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
    isUsingNextDns -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
    isOffline -> MaterialTheme.colorScheme.surfaceVariant
    else -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
  }

  val badgeTextColor = when {
    isTesting -> MaterialTheme.colorScheme.primary
    isUsingNextDns -> MaterialTheme.colorScheme.tertiary
    isOffline -> MaterialTheme.colorScheme.onSurfaceVariant
    else -> MaterialTheme.colorScheme.error
  }

  val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotate")
  val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(1000, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    ),
    label = "rotation"
  )

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .border(1.dp, borderColor, RoundedCornerShape(18.dp)),
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surfaceVariant,
    shadowElevation = 2.dp
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(12.dp),
          modifier = Modifier.weight(1f)
        ) {
          StatusBeacon(
            color = beaconColor,
            size = 10.dp,
            isPulsing = isUsingNextDns || isTesting
          )
          Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = title,
                style = MaterialTheme.typography.titleSmall.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 14.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
              )
              Surface(
                color = badgeBg,
                shape = RoundedCornerShape(6.dp)
              ) {
                Text(
                  text = badgeText,
                  color = badgeTextColor,
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold,
                  modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
              }
            }
            Text(
              text = subtitle,
              style = MaterialTheme.typography.bodySmall.copy(
                fontSize = 11.5.sp,
                lineHeight = 15.sp
              ),
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }

        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Surface(
            color = if (isUsingNextDns) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f) else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
            shape = RoundedCornerShape(12.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(4.dp),
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .background(
                    if (isUsingNextDns) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
                    CircleShape
                  )
              )
              Text(
                text = "CANLI",
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                color = if (isUsingNextDns) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
              )
            }
          }

          IconButton(
            onClick = onRefreshDiagnostic,
            enabled = !isTesting,
            modifier = Modifier.size(48.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Yeniden Test Et",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier
                .size(18.dp)
                .then(if (isTesting) Modifier.graphicsLayer { rotationZ = rotation } else Modifier)
            )
          }
        }
      }

      if (!isUsingNextDns) {
        Spacer(modifier = Modifier.height(12.dp))
        HorizontalDivider(color = borderColor.copy(alpha = 0.3f))
        Spacer(modifier = Modifier.height(10.dp))
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
            .heightIn(min = 48.dp)
            .bounceClick(scaleDown = 0.98f, onClick = onOpenDnsSettings)
            .padding(horizontal = 12.dp, vertical = 8.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Icon(
              imageVector = Icons.Default.Settings,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(16.dp)
            )
            Text(
              text = "Android Özel DNS Ayarlarını Aç",
              color = MaterialTheme.colorScheme.primary,
              fontSize = 12.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
          Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

@Composable
private fun EndpointsSection(
  profileId: String,
  context: Context,
  modifier: Modifier = Modifier
) {
  NextDnsCard(
    modifier = modifier,
    title = "Uç noktalar",
    subtitle = "NextDNS'i bu profille kullanmak için aşağıdaki uç noktalardan birini ayarlayın."
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
        .clip(RoundedCornerShape(14.dp))
    ) {
      EndpointTableRow("ID", profileId) { copyToClipboard(context, profileId, "ID") }
      HorizontalDivider(color = MaterialTheme.colorScheme.outline)
      EndpointTableRow("DNS-over-TLS/QUIC", "$profileId.dns.nextdns.io") {
        copyToClipboard(context, "$profileId.dns.nextdns.io", "DoT Adresi")
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outline)
      EndpointTableRow("DNS-over-HTTPS", "https://dns.nextdns.io/$profileId") {
        copyToClipboard(context, "https://dns.nextdns.io/$profileId", "DoH URL")
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outline)
      EndpointTableRow("IPv6", "2a07:a8c0::$profileId\n2a07:a8c1::$profileId") {
        copyToClipboard(context, "2a07:a8c0::$profileId", "IPv6 Adresi")
      }
    }
  }
}

@Composable
private fun LinkedIpSection(
  profileId: String,
  setup: com.example.data.api.SetupDto?,
  testResult: DiagnosticTestResult,
  showAdvancedIpSettings: Boolean,
  onToggleAdvanced: () -> Unit,
  onLinkIp: () -> Unit,
  context: Context,
  modifier: Modifier = Modifier
) {
  val detectedIp = testResult.clientIp.ifBlank { "" }
  val rawStatus = testResult.status.lowercase().trim()
  val isUsingNextDns = rawStatus == "ok" || rawStatus == "using-nextdns"

  NextDnsCard(
    modifier = modifier,
    title = "Bağlı IP",
    subtitle = "DNS-over-TLS, DNS-over-HTTPS veya IPv6 kullanarak NextDNS'i ayarlayamıyorsanız aşağıdaki DNS sunucularını kullanın ve IP'nizi bağlayın. Bu yöntem çoğunlukla ev ağlarında kullanım içindir ve mobil cihazlarda önerilmez."
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
        .clip(RoundedCornerShape(14.dp))
    ) {
      val ipv4List = setup?.ipv4 ?: listOf("45.90.28.234", "45.90.30.234")
      EndpointTableRow("DNS sunucuları", ipv4List.joinToString("\n")) {
        copyToClipboard(context, ipv4List.firstOrNull() ?: "", "DNS Sunucusu")
      }
      HorizontalDivider(color = MaterialTheme.colorScheme.outline)
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Bağlı IP",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 12.5.sp,
          fontWeight = FontWeight.Medium,
          modifier = Modifier.width(110.dp)
        )
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Text(
            text = detectedIp.ifBlank { "Algılanıyor..." },
            color = MaterialTheme.colorScheme.onSurface,
            fontFamily = FontFamily.Monospace,
            fontSize = 12.sp,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .heightIn(min = 48.dp)
              .padding(horizontal = 4.dp)
              .bounceClick(scaleDown = 0.99f) {
                if (detectedIp.isNotBlank()) {
                  copyToClipboard(context, detectedIp, "IP Adresi")
                }
              }
          )
          if (isUsingNextDns && detectedIp.isNotBlank()) {
            Icon(
              imageVector = Icons.Default.CheckCircle,
              contentDescription = "Bağlı",
              tint = MaterialTheme.colorScheme.tertiary,
              modifier = Modifier.size(16.dp)
            )
          } else {
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .heightIn(min = 48.dp)
                .bounceClick(scaleDown = 0.98f, onClick = onLinkIp),
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
              shape = RoundedCornerShape(6.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Link,
                  contentDescription = "IP'yi Bağla",
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(13.dp)
                )
                Text(
                  text = "IP'yi Bağla",
                  color = MaterialTheme.colorScheme.primary,
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    Row(
      modifier = Modifier
        .clip(RoundedCornerShape(8.dp))
        .heightIn(min = 48.dp)
        .bounceClick(scaleDown = 0.98f, onClick = onToggleAdvanced)
        .padding(vertical = 6.dp, horizontal = 6.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(
        text = if (showAdvancedIpSettings) "Gelişmiş ayarları gizle" else "Gelişmiş ayarları göster",
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
      )
      Icon(
        imageVector = if (showAdvancedIpSettings) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(16.dp)
      )
    }

    if (showAdvancedIpSettings) {
      Spacer(modifier = Modifier.height(8.dp))
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Dinamik DNS (DDNS) Güncelleme URL'si:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
          Text(
            text = "https://link-ip.nextdns.io/$profileId/update",
            color = MaterialTheme.colorScheme.primary,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.5.sp,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .heightIn(min = 48.dp)
              .padding(horizontal = 4.dp)
              .bounceClick(scaleDown = 0.99f) {
                copyToClipboard(context, "https://link-ip.nextdns.io/$profileId/update", "DDNS URL")
              }
          )
        }
      }
    }
  }
}

@Composable
private fun SetupGuideSection(
  profileId: String,
  setup: com.example.data.api.SetupDto?,
  platforms: List<String>,
  selectedPlatform: String,
  onPlatformSelected: (String) -> Unit,
  context: Context,
  modifier: Modifier = Modifier
) {
  NextDnsCard(
    modifier = modifier,
    title = "Kurulum rehberi",
    subtitle = "Cihazınızda, tarayıcınızda veya yönlendiricinizde NextDNS'i kurmak için aşağıdaki talimatları izleyin."
  ) {
    val platformScroll = rememberScrollState()
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .horizontalScroll(platformScroll)
        .padding(bottom = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      platforms.forEach { plat ->
        val isPlatSelected = selectedPlatform == plat
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .heightIn(min = 48.dp)
            .bounceClick(scaleDown = 0.98f) { onPlatformSelected(plat) },
          shape = RoundedCornerShape(12.dp),
          color = if (isPlatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
          border = BorderStroke(
            1.dp,
            if (isPlatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
          )
        ) {
          Text(
            text = plat,
            color = if (isPlatSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = if (isPlatSelected) FontWeight.Bold else FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
          )
        }
      }
    }

    when (selectedPlatform) {
      "Android" -> AndroidSetupGuide(profileId, context)
      "Windows" -> WindowsSetupGuide(profileId, setup, context)
      "iOS" -> IosSetupGuide(profileId, context)
      "macOS" -> MacOsSetupGuide(profileId, context)
      "Linux" -> LinuxSetupGuide(profileId, context)
      "ChromeOS" -> ChromeOsSetupGuide(profileId, context)
      "Tarayıcılar" -> BrowserSetupGuide(profileId, context)
      "Yönlendiriciler" -> RouterSetupGuide(profileId, setup, context)
      else -> AndroidSetupGuide(profileId, context)
    }
  }
}

// =========================================================================
// Platform Setup Guides
// =========================================================================

@Composable
private fun EndpointTableRow(
  label: String,
  value: String,
  onCopy: () -> Unit
) {
  var isCopied by remember { mutableStateOf(false) }

  LaunchedEffect(isCopied) {
    if (isCopied) {
      delay(2000L)
      isCopied = false
    }
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .heightIn(min = 48.dp)
      .bounceClick(scaleDown = 0.99f) {
        onCopy()
        isCopied = true
      }
      .padding(horizontal = 14.dp, vertical = 12.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween
  ) {
    Text(
      text = label,
      color = MaterialTheme.colorScheme.onSurface,
      fontSize = 12.5.sp,
      fontWeight = FontWeight.Medium,
      modifier = Modifier.width(130.dp)
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.weight(1f)
    ) {
      Text(
        text = value,
        color = if (isCopied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        modifier = Modifier.weight(1f)
      )
      Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (isCopied) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
        border = BorderStroke(
          1.dp,
          if (isCopied) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
          else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
        )
      ) {
        Row(
          modifier = Modifier.padding(horizontal = 7.dp, vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
          Icon(
            imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
            contentDescription = "Kopyala",
            tint = if (isCopied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(12.dp)
          )
          Text(
            text = if (isCopied) "Kopyalandı" else "Kopyala",
            color = if (isCopied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
            fontSize = 10.5.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }
  }
}

@Composable
private fun CopyableValueBox(
  value: String,
  context: Context,
  modifier: Modifier = Modifier,
  label: String? = null,
  copyLabel: String = "Değer",
  description: String? = null,
  isCode: Boolean = true
) {
  var isCopied by remember { mutableStateOf(false) }

  LaunchedEffect(isCopied) {
    if (isCopied) {
      delay(2000L)
      isCopied = false
    }
  }

  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(6.dp)
  ) {
    if (!label.isNullOrBlank()) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          text = label,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 12.sp,
          fontWeight = FontWeight.SemiBold
        )
        if (!description.isNullOrBlank()) {
          Text(
            text = description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
          )
        }
      }
    }

    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(12.dp),
      border = BorderStroke(
        1.dp,
        if (isCopied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.outline
      ),
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(min = 48.dp)
        .bounceClick(scaleDown = 0.99f) {
          copyToClipboard(context, value, copyLabel)
          isCopied = true
        }
    ) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = value,
          color = if (isCopied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
          fontFamily = if (isCode) FontFamily.Monospace else FontFamily.Default,
          fontSize = 12.5.sp,
          fontWeight = FontWeight.SemiBold,
          modifier = Modifier
            .weight(1f)
            .padding(end = 10.dp)
        )

        Surface(
          shape = RoundedCornerShape(8.dp),
          color = if (isCopied) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                  else MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
          border = BorderStroke(
            1.dp,
            if (isCopied) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.3f)
            else MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)
          )
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Icon(
              imageVector = if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
              contentDescription = "Kopyala",
              tint = if (isCopied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(13.dp)
            )
            Text(
              text = if (isCopied) "Kopyalandı" else "Kopyala",
              color = if (isCopied) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }
      }
    }
  }
}

@Composable
private fun SetupStepItem(num: String, text: String, subtext: String? = null) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalAlignment = Alignment.Top,
    modifier = Modifier.fillMaxWidth()
  ) {
    Surface(
      shape = CircleShape,
      color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
      modifier = Modifier.size(24.dp)
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = num,
          color = MaterialTheme.colorScheme.primary,
          fontWeight = FontWeight.Bold,
          fontSize = 12.sp
        )
      }
    }
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
      Text(
        text = text,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.5.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 17.sp
      )
      if (!subtext.isNullOrBlank()) {
        Text(
          text = subtext,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.5.sp,
          lineHeight = 15.sp
        )
      }
    }
  }
}

@Composable
private fun AndroidSetupGuide(profileId: String, context: Context) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          NextDnsRecommendedBadge()
          Text("Özel DNS (Private DNS / DoT)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        }
        Text(
          text = "Android 9 (Pie) ve üzeri tüm cihazlarda yerleşik olarak çalışır. Arka planda pil harcamaz ve herhangi bir harici yazılım gerektirmez.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 12.sp,
          lineHeight = 16.sp
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.6f))

        SetupStepItem("1", "Ayarlar → Ağ ve İnternet (veya Bağlantılar) menüsünü açın.")
        SetupStepItem("2", "Gelişmiş → Özel DNS (Private DNS) seçeneğine dokunun.")
        SetupStepItem("3", "Özel DNS sağlayıcı ana bilgisayar adı seçeneğini işaretleyin.")
        SetupStepItem("4", "Aşağıdaki ana bilgisayar adını yapıştırın ve Kaydet'e basın:")

        CopyableValueBox(
          label = "Standart Özel DNS Adresi",
          value = "$profileId.dns.nextdns.io",
          context = context,
          copyLabel = "Özel DNS Adresi"
        )

        // Pro tip: Cihaz adlandırma
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
          shape = RoundedCornerShape(10.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(15.dp))
              Text("İpucu: Cihazınızı Günlüklerde İsimlendirin", fontWeight = FontWeight.Bold, fontSize = 11.5.sp, color = MaterialTheme.colorScheme.primary)
            }
            Text(
              text = "Günlüklerde hangi cihazın sorgu yaptığını görmek için adresi cihaz-adiniz-$profileId.dns.nextdns.io şeklinde girebilirsiniz:",
              fontSize = 11.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              lineHeight = 15.sp
            )
            CopyableValueBox(
              value = "telefon-$profileId.dns.nextdns.io",
              context = context,
              copyLabel = "Cihaz Adlı DoT Adresi"
            )
          }
        }

        Spacer(modifier = Modifier.height(4.dp))

        NextDnsButton(
          text = "Android Özel DNS Ayarlarını Aç",
          onClick = {
            try {
              context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
            } catch (_: Exception) {
              try {
                context.startActivity(Intent("android.settings.NETWORK_PROVIDER_SETTINGS").apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
              } catch (_: Exception) {
                context.startActivity(Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) })
              }
            }
          },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
private fun WindowsSetupGuide(profileId: String, setup: com.example.data.api.SetupDto?, context: Context) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          NextDnsRecommendedBadge()
          Text("Windows Yerel DoH (DNS-over-HTTPS)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        }
        SetupStepItem("1", "Ayarlar → Ağ ve İnternet → Wi-Fi veya Ethernet'i seçin.")
        SetupStepItem("2", "Donanım özellikleri → DNS sunucusu ataması yanındaki 'Düzenle'ye tıklayın.")
        SetupStepItem("3", "'El ile girilen'i seçin ve IPv4'ü açın.")
        SetupStepItem("4", "Tercih edilen DNS alanına aşağıdaki IP'yi girin:")

        val ipv4First = setup?.ipv4?.firstOrNull() ?: "45.90.28.0"
        CopyableValueBox(
          label = "Tercih Edilen DNS (IPv4)",
          value = ipv4First,
          context = context,
          copyLabel = "Tercih Edilen DNS"
        )

        SetupStepItem("5", "'HTTPS üzerinden DNS şifreleme' seçeneğini 'Yalnızca şifrelenmiş (HTTPS üzerinden DNS)' yapın.")
        SetupStepItem("6", "DNS-over-HTTPS şablonu alanına aşağıdaki URL'yi yapıştırın:")

        CopyableValueBox(
          label = "DoH Şablonu",
          value = "https://dns.nextdns.io/$profileId",
          context = context,
          copyLabel = "DoH Şablonu URL"
        )
      }
    }
  }
}

@Composable
private fun IosSetupGuide(profileId: String, context: Context) {
  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(14.dp),
      border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          NextDnsRecommendedBadge()
          Text("Apple Yapılandırma Profili (iOS 14+)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        }
        Text(
          text = "iOS 14 ve iPadOS 14'ten itibaren Apple sistem düzeyinde şifreli DNS'i yerel olarak destekler. Herhangi bir pil tüketimi oluşturmaz.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 12.sp,
          lineHeight = 16.sp
        )

        SetupStepItem("1", "Safari tarayıcınızda apple.nextdns.io adresini açın:")

        CopyableValueBox(
          label = "Profil İndirme Bağlantısı",
          value = "https://apple.nextdns.io/$profileId",
          context = context,
          copyLabel = "Apple Profil URL"
        )

        SetupStepItem("2", "İndirilen profili onaylayın. Ayarlar → Profil İndirildi menüsünden 'Yükle'ye dokunun.")
        SetupStepItem("3", "İstenirse cihaz parolanızı girip kurulumu tamamlayın.")

        NextDnsButton(
          text = "Safari'de apple.nextdns.io Aç",
          onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://apple.nextdns.io/$profileId")).apply {
              addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
          },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
private fun MacOsSetupGuide(profileId: String, context: Context) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("macOS için NextDNS", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
      Text(
        text = "macOS Big Sur (11.0) veya üzeri için sistem yapılandırma profilini Safari ile indirip kolayca kurabilirsiniz.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )

      SetupStepItem("1", "Safari'de apple.nextdns.io adresini açıp profili indirin:")

      CopyableValueBox(
        label = "macOS Yapılandırma Profili URL",
        value = "https://apple.nextdns.io/$profileId",
        context = context,
        copyLabel = "Apple Profil URL"
      )

      SetupStepItem("2", "Sistem Ayarları → Gizlilik ve Güvenlik → Profiller bölümünden profili yükleyin.")

      SetupStepItem("3", "Veya Homebrew ile NextDNS CLI kurun:")

      CopyableValueBox(
        label = "Homebrew Komutu",
        value = "brew install nextdns/tap/nextdns && sudo nextdns install -config $profileId -auto-activate",
        context = context,
        copyLabel = "Homebrew Komutu"
      )
    }
  }
}

@Composable
private fun LinuxSetupGuide(profileId: String, context: Context) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("Linux CLI Kurulumu", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
      Text(
        text = "Resmi NextDNS Linux istemcisini tek komutla kurup arka plan servisi olarak çalıştırabilirsiniz:",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )

      SetupStepItem("1", "Terminalinizi açın ve kurulum betiğini çalıştırın:")

      CopyableValueBox(
        label = "Otomatik Kurulum Komutu",
        value = "sh -c 'sh -c \"$(curl -sL https://nextdns.io/install)\"'",
        context = context,
        copyLabel = "Linux Kurulum Komutu"
      )

      SetupStepItem("2", "Kurulum sırasında sorulduğunda Profil Kimliğinizi girin:")

      CopyableValueBox(
        label = "Profil Kimliği (Configuration ID)",
        value = profileId,
        context = context,
        copyLabel = "Profil Kimliği"
      )

      SetupStepItem("3", "Kurulum tamamlandıktan sonra servisi başlatın:")

      CopyableValueBox(
        label = "Servis Başlatma Komutu",
        value = "sudo nextdns start",
        context = context,
        copyLabel = "Servis Başlatma Komutu"
      )
    }
  }
}

@Composable
private fun ChromeOsSetupGuide(profileId: String, context: Context) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
      Text("ChromeOS Güvenli DNS", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
      SetupStepItem("1", "Ayarlar → Güvenlik ve Gizlilik → Güvenli DNS bölümüne gidin.")
      SetupStepItem("2", "Özel seçeneğini belirleyin ve aşağıdaki DoH adresini yapıştırın:")

      CopyableValueBox(
        label = "Güvenli DNS (DoH) Adresi",
        value = "https://dns.nextdns.io/$profileId",
        context = context,
        copyLabel = "ChromeOS DoH Adresi"
      )
    }
  }
}

@Composable
private fun BrowserSetupGuide(profileId: String, context: Context) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Text("Tarayıcı Güvenli DNS (DoH)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
      Text(
        text = "Yalnızca belirli bir tarayıcıda NextDNS kullanmak istiyorsanız şifreli DNS (DoH) URL'sini tarayıcı ayarlarına ekleyebilirsiniz.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )

      // Chrome / Brave / Edge
      Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Google Chrome / Brave / Edge", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
          Text(
            text = "Ayarlar → Gizlilik ve güvenlik → Güvenlik → Güvenli DNS kullan → 'Özel' seçin ve aşağıdaki URL'yi yapıştırın:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            lineHeight = 15.sp
          )
          CopyableValueBox(
            value = "https://dns.nextdns.io/$profileId",
            context = context,
            copyLabel = "Chrome DoH Adresi"
          )
        }
      }

      // Firefox
      Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("Mozilla Firefox", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.5.sp)
          Text(
            text = "Ayarlar → Gizlilik ve Güvenlik → DNS over HTTPS → Maksimum Koruma → 'Özel' seçin ve aşağıdaki URL'yi yapıştırın:",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            lineHeight = 15.sp
          )
          CopyableValueBox(
            value = "https://dns.nextdns.io/$profileId",
            context = context,
            copyLabel = "Firefox DoH Adresi"
          )
        }
      }
    }
  }
}

@Composable
private fun RouterSetupGuide(profileId: String, setup: com.example.data.api.SetupDto?, context: Context) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
      Text("Yönlendirici (Router) Kurulumu", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
      Text(
        text = "Evinizdeki tüm cihazları (akıllı TV, oyun konsolu vb.) tek merkezden korumak için modem/router'ınızın DNS ayarlarını yapılandırın:",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp,
        lineHeight = 16.sp
      )

      Text("1. Standart IPv4 DNS Sunucuları", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)

      val ipv4First = setup?.ipv4?.getOrNull(0) ?: "45.90.28.234"
      CopyableValueBox(
        label = "Birincil DNS (IPv4)",
        value = ipv4First,
        context = context,
        copyLabel = "Birincil DNS"
      )

      val ipv4Second = setup?.ipv4?.getOrNull(1) ?: "45.90.30.234"
      CopyableValueBox(
        label = "İkincil DNS (IPv4)",
        value = ipv4Second,
        context = context,
        copyLabel = "İkincil DNS"
      )

      Text("2. IPv6 DNS Sunucuları", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)

      val ipv6First = setup?.ipv6?.getOrNull(0) ?: "2a07:a8c0::$profileId"
      CopyableValueBox(
        label = "Birincil DNS (IPv6)",
        value = ipv6First,
        context = context,
        copyLabel = "Birincil IPv6"
      )

      val ipv6Second = setup?.ipv6?.getOrNull(1) ?: "2a07:a8c1::$profileId"
      CopyableValueBox(
        label = "İkincil DNS (IPv6)",
        value = ipv6Second,
        context = context,
        copyLabel = "İkincil IPv6"
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outline)

      Text("3. Gelişmiş Router'lar (Asuswrt-Merlin, OpenWrt, DD-WRT, pfSense)", fontWeight = FontWeight.Bold, fontSize = 12.5.sp, color = MaterialTheme.colorScheme.onSurface)
      Text(
        text = "NextDNS CLI istemcisini doğrudan router üzerinde kurarak DoH veya DoT şifrelemesi kullanabilirsiniz:",
        fontSize = 11.5.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        lineHeight = 15.sp
      )

      CopyableValueBox(
        label = "Router CLI Kurulum Komutu",
        value = "sh -c 'sh -c \"$(curl -sL https://nextdns.io/install)\"'",
        context = context,
        copyLabel = "Router CLI Komutu"
      )
    }
  }
}
