package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.NextDnsProfile
import com.example.data.repository.ApiConnectionStatus
import com.example.ui.components.*
import com.example.ui.viewmodel.NextDnsViewModel

@Composable
fun AccountScreen(
  viewModel: NextDnsViewModel,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  val accountInfo by viewModel.accountInfo.collectAsState()
  val apiKey by viewModel.apiKey.collectAsState()
  val apiStatus by viewModel.apiStatus.collectAsState()
  val profiles by viewModel.profiles.collectAsState()
  val activeProfileId by viewModel.activeProfileId.collectAsState()
  val testResult by viewModel.testResult.collectAsState()
  val isSyncing by viewModel.isSyncing.collectAsState()
  val analytics by viewModel.analytics.collectAsState()
  val analyticsLastSuccessAt by viewModel.analyticsLastSuccessAt.collectAsState()
  val analyticsErrorMessage by viewModel.analyticsErrorMessage.collectAsState()
  val isAnalyticsLoading by viewModel.isAnalyticsLoading.collectAsState()

  var showLogoutConfirm by remember { mutableStateOf(false) }
  var showNewProfileDialog by remember { mutableStateOf(false) }
  var newProfileName by remember { mutableStateOf("") }
  var showApiKey by remember { mutableStateOf(false) }

  LaunchedEffect(activeProfileId) {
    if (activeProfileId.isNotBlank()) {
      viewModel.refreshAnalytics(device = null, time = null)
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
        title = "DNS Metrikleri",
        subtitle = "Aktif profil için NextDNS API'sinden son alınan sorgu istatistikleri."
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
                  text = "Toplam Sorgu",
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
                  text = "Engellenen",
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.sp
                )
                Text(
                  text = analyticsLastSuccessAt?.let { "%,d".format(analytics.blockedQueries) } ?: "—",
                  color = Color(0xFFEF4444),
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
                  text = "Engelleme",
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.sp
                )
                Text(
                  text = analyticsLastSuccessAt?.let { "%%%d".format(analytics.blockedPercentage.toInt()) } ?: "—",
                  color = Color(0xFF10B981),
                  fontSize = 16.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }

          if (analytics.topDevices.isNotEmpty()) {
            Text(
              text = "Bağlı Aktif Cihazlar: ${analytics.topDevices.size} cihaz (${analytics.topDevices.take(3).joinToString { it.name }})",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.5.sp
            )
          }

          if (analyticsLastSuccessAt == null) {
            Text(
              text = when {
                isAnalyticsLoading -> "NextDNS analiz verisi alınıyor…"
                !analyticsErrorMessage.isNullOrBlank() -> analyticsErrorMessage!!
                else -> "Henüz doğrulanmış analiz verisi alınmadı."
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
        title = "API Anahtarı",
        subtitle = "Bu cihazda kullanılan NextDNS kimlik doğrulama anahtarı."
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
                text = if (apiKey.isBlank()) {
                  "Anahtar girilmedi"
                } else if (showApiKey) {
                  apiKey
                } else {
                  apiKey.take(4) + "••••••••••••••••" + apiKey.takeLast(4)
                },
                fontFamily = FontFamily.Monospace,
                fontSize = 12.5.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
              )

              Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                  onClick = { showApiKey = !showApiKey },
                  modifier = Modifier.size(32.dp)
                ) {
                  Icon(
                    imageVector = if (showApiKey) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                    contentDescription = "Göster/Gizle",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                  )
                }

                if (apiKey.isNotBlank()) {
                  IconButton(
                    onClick = {
                      clipboardManager.setText(AnnotatedString(apiKey))
                      Toast.makeText(context, "API Anahtarı kopyalandı", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.size(32.dp)
                  ) {
                    Icon(
                      imageVector = Icons.Default.ContentCopy,
                      contentDescription = "Kopyala",
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
              text = if (isSyncing) "EŞİTLENİYOR..." else "TÜM VERİYİ BULUTLA EŞİTLE",
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
        title = "Profiller (${profiles.size})",
        subtitle = "Hesabınıza bağlı NextDNS profilleri."
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
                        if (isActive) Color(0xFF10B981) else MaterialTheme.colorScheme.outline,
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
                    color = Color(0xFF10B981).copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                  ) {
                    Text(
                      text = "AKTİF",
                      color = Color(0xFF10B981),
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
            text = "YENİ PROFİL OLUŞTUR",
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
        title = "Ağ Teşhisi",
        subtitle = "Cihazınızın NextDNS bağlantı durumu ve protokol parametreleri."
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
                text = if (isUsingNextDns) "Bu cihaz NextDNS kullanıyor" else "NextDNS aktif değil",
                color = if (isUsingNextDns) Color(0xFF10B981) else MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.Bold,
                fontSize = 13.5.sp
              )
              if (testResult.serverPoP.isNotBlank()) {
                Text(
                  text = "Sunucu: ${testResult.serverPoP} (${testResult.protocol}) • Gecikme: ${testResult.latencyMs} ms",
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.5.sp
                )
              }
            }

            NextDnsButton(
              text = if (testResult.isTesting) "TEST..." else "TEŞHİS ET",
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
        title = "Oturum Yönetimi",
        subtitle = "Web paneli ve oturum sonlandırma."
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
            Text("my.nextdns.io Hesabını Aç", fontSize = 12.5.sp)
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
            Text("Çıkış Yap", fontSize = 12.5.sp, fontWeight = FontWeight.Bold)
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
        Text("Çıkış Yapmak İstiyor musunuz?", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
      },
      text = {
        Text(
          "NextDNS API bağlantısı kesilecek ve kayıtlı oturum bilgileri temizlenecektir.",
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
          Text("Çıkış Yap", color = MaterialTheme.colorScheme.onError)
        }
      },
      dismissButton = {
        TextButton(onClick = { showLogoutConfirm = false }) {
          Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
        Text("Yeni Profil Oluştur", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold)
      },
      text = {
        OutlinedTextField(
          value = newProfileName,
          onValueChange = { newProfileName = it },
          label = { Text("Profil Adı") },
          placeholder = { Text("Örn: Ev Ağı, Telefon") },
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
          Text("Oluştur")
        }
      },
      dismissButton = {
        TextButton(onClick = { showNewProfileDialog = false }) {
          Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    )
  }

}
