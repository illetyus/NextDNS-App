package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
  val activeProfile by viewModel.activeProfile.collectAsState()
  val testResult by viewModel.testResult.collectAsState()
  val isDiagnosticRunning by viewModel.isDiagnosticRunning.collectAsState()

  var selectedPlatform by remember { mutableStateOf("Android") }
  var showAdvancedIpSettings by remember { mutableStateOf(false) }

  val platforms = listOf("Android", "iOS", "Windows", "macOS", "Linux", "ChromeOS", "Tarayıcılar", "Yönlendiriciler")
  val profile = activeProfile ?: NextDnsProfile("82a32a", "Ana Profil")
  val profileId = profile.id.ifBlank { "82a32a" }

  val isConnected = testResult.status == "using-nextdns"

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Android 16 Expressive Live Status Banner
    item {
      Surface(
        modifier = Modifier
          .fillMaxWidth()
          .border(
            1.dp,
            if (isConnected) MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.error.copy(alpha = 0.5f),
            RoundedCornerShape(18.dp)
          ),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shadowElevation = 2.dp
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.weight(1f)
          ) {
            StatusBeacon(
              color = if (isConnected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
              size = 10.dp,
              isPulsing = true
            )
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = if (isConnected) "Her şey yolunda!" else "Yapılandırılmadı",
                  style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                  ),
                  color = MaterialTheme.colorScheme.onSurface
                )
                if (isConnected) {
                  Surface(
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = "12 ms • DoH",
                      color = MaterialTheme.colorScheme.tertiary,
                      fontSize = 10.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                  }
                }
              }
              Text(
                text = if (isConnected) "Bu cihaz, NextDNS'i bu profille kullanıyor." else "Bu cihaz NextDNS üzerinden yapılandırılmamış.",
                style = MaterialTheme.typography.bodySmall.copy(
                  fontSize = 11.5.sp,
                  lineHeight = 15.sp
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }

          IconButton(
            onClick = { viewModel.runDiagnostic() },
            modifier = Modifier
              .size(36.dp)
              .bounceClick(scaleDown = 0.88f) { viewModel.runDiagnostic() }
          ) {
            Icon(
              imageVector = Icons.Default.Refresh,
              contentDescription = "Yeniden Test Et",
              tint = MaterialTheme.colorScheme.primary,
              modifier = Modifier.size(18.dp)
            )
          }
        }
      }
    }

    // 2. Uç noktalar Kartı
    item {
      NextDnsCard(
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

    // 3. Bağlı IP Kartı
    item {
      NextDnsCard(
        title = "Bağlı IP",
        subtitle = "Uygulamalarımızı, DNS-over-TLS, DNS-over-HTTPS veya IPv6 kullanarak NextDNS'i ayarlayamıyorsanız aşağıdaki DNS sunucularını kullanın ve IP'nizi bağlayın. Bu yöntem çoğunlukla ev ağlarında kullanım içindir ve mobil cihazlarda önerilmez."
      ) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(14.dp))
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
        ) {
          EndpointTableRow("DNS sunucuları", "45.90.28.234\n45.90.30.234") {
            copyToClipboard(context, "45.90.28.234", "DNS Sunucusu")
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
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(
                text = testResult.clientIp.ifBlank { "37.130.67.187" },
                color = MaterialTheme.colorScheme.onSurface,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp
              )
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Bağlı",
                tint = MaterialTheme.colorScheme.tertiary,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .bounceClick(scaleDown = 0.96f) { showAdvancedIpSettings = !showAdvancedIpSettings }
            .padding(vertical = 6.dp, horizontal = 4.dp),
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
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
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
                  .bounceClick {
                    copyToClipboard(context, "https://link-ip.nextdns.io/$profileId/update", "DDNS URL")
                  }
              )
            }
          }
        }
      }
    }

    // 4. Kurulum Rehberi Kartı
    item {
      NextDnsCard(
        title = "Kurulum rehberi",
        subtitle = "Cihazınızda, tarayıcınızda veya yönlendiricinizde NextDNS'i kurmak için aşağıdaki talimatları izleyin."
      ) {
        // Platform Seçim Sekmeleri (Android 16 Expressive pill tabs)
        val platformScroll = rememberScrollState()
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(platformScroll)
            .padding(bottom = 16.dp),
          horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          platforms.forEach { plat ->
            val isPlatSelected = selectedPlatform == plat
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .bounceClick(scaleDown = 0.93f) { selectedPlatform = plat },
              shape = RoundedCornerShape(10.dp),
              color = if (isPlatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
              border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (isPlatSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline
              )
            ) {
              Text(
                text = plat,
                color = if (isPlatSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                fontWeight = if (isPlatSelected) FontWeight.Bold else FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
              )
            }
          }
        }


        // Platforma Özgü Kurulum Detayları
        when (selectedPlatform) {
          "Android" -> AndroidSetupGuide(profileId, context)
          "Windows" -> WindowsSetupGuide(profileId, context)
          "iOS" -> IosSetupGuide(profileId, context)
          "macOS" -> MacOsSetupGuide(profileId, context)
          "Linux" -> LinuxSetupGuide(profileId, context)
          "ChromeOS" -> ChromeOsSetupGuide(profileId, context)
          "Tarayıcılar" -> BrowserSetupGuide(profileId, context)
          "Yönlendiriciler" -> RouterSetupGuide(profileId, context)
          else -> AndroidSetupGuide(profileId, context)
        }
      }
    }
  }
}

@Composable
private fun EndpointTableRow(
  label: String,
  value: String,
  onCopy: () -> Unit
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .bounceClick(scaleDown = 0.98f) { onCopy() }
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
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        modifier = Modifier.weight(1f)
      )
      Icon(
        imageVector = Icons.Default.ContentCopy,
        contentDescription = "Kopyala",
        tint = MaterialTheme.colorScheme.primary,
        modifier = Modifier.size(15.dp)
      )
    }
  }
}

@Composable
private fun AndroidSetupGuide(profileId: String, context: android.content.Context) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    // Android Private DNS Guide
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(14.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          NextDnsRecommendedBadge()
          Text("Özel DNS (Private DNS)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        }
        Text(
          text = "Android 9 veya sonraki sürümlerde yerel olarak desteklenir. Uygulama yüklemenize gerek kalmaz.",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 11.5.sp,
          lineHeight = 15.sp
        )
        SetupStepItem("1", "Ayarlar → Ağ ve İnternet (veya Bağlantılar) bölümüne gidin.")
        SetupStepItem("2", "Gelişmiş → Özel DNS seçeneğini bulun.")
        SetupStepItem("3", "Özel DNS sağlayıcı ana bilgisayar adı seçeneğini seçin.")
        SetupStepItem("4", "Aşağıdaki ana bilgisayar adını girin ve Kaydet'e dokunun:")

        Surface(
          color = MaterialTheme.colorScheme.background,
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .bounceClick { copyToClipboard(context, "$profileId.dns.nextdns.io", "Özel DNS Adresi") }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text(
              text = "$profileId.dns.nextdns.io",
              color = MaterialTheme.colorScheme.primary,
              fontFamily = FontFamily.Monospace,
              fontWeight = FontWeight.Bold,
              fontSize = 12.5.sp
            )
            Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
          }
        }

        NextDnsButton(
          text = "Android DNS Ayarlarını Aç",
          onClick = {
            try {
              context.startActivity(Intent(Settings.ACTION_WIRELESS_SETTINGS))
            } catch (e: Exception) {
              try {
                context.startActivity(Intent(Settings.ACTION_SETTINGS))
              } catch (_: Exception) {}
            }
          },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
private fun WindowsSetupGuide(profileId: String, context: android.content.Context) {
  Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(14.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          NextDnsRecommendedBadge()
          Text("HTTPS üzerinden DNS (DoH) - Windows 11", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        }
        SetupStepItem("1", "Ayarlar uygulamasını açın → Ağ ve internet bölümüne gidin.")
        SetupStepItem("2", "Wi-Fi veya Ethernet başlığına tıklayın → Donanım özelliklerine girin.")
        SetupStepItem("3", "DNS sunucusu ataması yanındaki Düzenle düğmesine tıklayın.")
        SetupStepItem("4", "El ile girilen'i seçin ve IPv4'ü açın.")
        SetupStepItem("5", "Tercih edilen DNS'e 45.90.28.0 yazın, HTTPS üzerinden DNS şablonunu Açık yapın ve DoH şablonunu girin:")

        Surface(
          color = MaterialTheme.colorScheme.background,
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
          modifier = Modifier
            .fillMaxWidth()
            .bounceClick { copyToClipboard(context, "https://dns.nextdns.io/$profileId", "DoH Adresi") }
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("https://dns.nextdns.io/$profileId", color = MaterialTheme.colorScheme.primary, fontFamily = FontFamily.Monospace, fontSize = 11.5.sp)
            Icon(Icons.Default.ContentCopy, contentDescription = "Kopyala", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
          }
        }
      }
    }
  }
}


@Composable
private fun IosSetupGuide(profileId: String, context: android.content.Context) {
  Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
    Surface(
      color = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(14.dp),
      border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
          NextDnsRecommendedBadge()
          Text("Apple Yapılandırma Profili (iOS 14+)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
        }
        SetupStepItem("1", "Safari tarayıcınızda apple.nextdns.io adresini açın.")
        SetupStepItem("2", "Profil Kimliği olarak $profileId girin ve profili indirin.")
        SetupStepItem("3", "Ayarlar → Profil İndirildi bölümünden profili yükleyin.")

        NextDnsButton(
          text = "Safari'de apple.nextdns.io Aç",
          onClick = {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://apple.nextdns.io/$profileId"))
            context.startActivity(intent)
          },
          modifier = Modifier.fillMaxWidth()
        )
      }
    }
  }
}

@Composable
private fun MacOsSetupGuide(profileId: String, context: android.content.Context) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("macOS için NextDNS", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
      SetupStepItem("1", "Mac App Store'dan resmi NextDNS uygulamasını indirin veya apple.nextdns.io profilini yükleyin.")
      SetupStepItem("2", "Yapılandırma Kimliği (Configuration ID) olarak $profileId girin.")
    }
  }
}

@Composable
private fun LinuxSetupGuide(profileId: String, context: android.content.Context) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Linux CLI Kurulumu", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
      Text("Terminalinizde şu komutu çalıştırın:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
      Surface(
        color = MaterialTheme.colorScheme.background,
        shape = RoundedCornerShape(10.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier
          .fillMaxWidth()
          .bounceClick { copyToClipboard(context, "sh -c 'sh -c \"$(curl -sL https://nextdns.io/install)\"'", "Linux Komutu") }
      ) {
        Text(
          text = "sh -c 'sh -c \"$(curl -sL https://nextdns.io/install)\"'",
          color = MaterialTheme.colorScheme.primary,
          fontFamily = FontFamily.Monospace,
          fontSize = 11.5.sp,
          modifier = Modifier.padding(12.dp)
        )
      }
    }
  }
}

@Composable
private fun ChromeOsSetupGuide(profileId: String, context: android.content.Context) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("ChromeOS Güvenli DNS", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
      SetupStepItem("1", "Ayarlar → Güvenlik ve Gizlilik → Güvenli DNS'e gidin.")
      SetupStepItem("2", "Özel seçeneğini seçin ve https://dns.nextdns.io/$profileId adresini girin.")
    }
  }
}

@Composable
private fun BrowserSetupGuide(profileId: String, context: android.content.Context) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Tarayıcı Güvenli DNS (DoH)", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
      SetupStepItem("• Chrome", "Ayarlar → Gizlilik ve güvenlik → Güvenlik → Güvenli DNS kullan → Özel: https://dns.nextdns.io/$profileId")
      SetupStepItem("• Firefox", "Ayarlar → Gizlilik ve Güvenlik → DNS over HTTPS → Maksimum Koruma → Özel: https://dns.nextdns.io/$profileId")
    }
  }
}

@Composable
private fun RouterSetupGuide(profileId: String, context: android.content.Context) {
  Surface(
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(14.dp),
    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = Modifier.fillMaxWidth()
  ) {
    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Text("Yönlendirici (Router) Kurulumu", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
      Text("Router'ınızın WAN/DNS ayarlarında DNS sunucularını güncelleyin:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
      SetupStepItem("• DNS 1", "45.90.28.234")
      SetupStepItem("• DNS 2", "45.90.30.234")
      SetupStepItem("• IPv6 1", "2a07:a8c0::$profileId")
      SetupStepItem("• IPv6 2", "2a07:a8c1::$profileId")
    }
  }
}

@Composable
private fun SetupStepItem(num: String, text: String) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.Top
  ) {
    Text(
      text = num,
      color = MaterialTheme.colorScheme.primary,
      fontWeight = FontWeight.Bold,
      fontSize = 12.5.sp,
      
    )
    Text(
      text = text,
      color = MaterialTheme.colorScheme.onSurface,
      fontSize = 12.5.sp,
      lineHeight = 16.sp
    )
  }
}

