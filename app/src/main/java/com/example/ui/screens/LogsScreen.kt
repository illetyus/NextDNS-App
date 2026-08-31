package com.example.ui.screens

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.Duration

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DnsLogEntry
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.NextDnsViewModel

@Composable

fun formatRelativeTime(timestampStr: String): String {
    try {
        if (timestampStr.isBlank()) return "şimdi"
        // Try parsing as ISO
        val time = if (timestampStr.contains("T")) {
            Instant.parse(timestampStr).toEpochMilli()
        } else {
            // Check if it's a number (seconds or millis)
            val d = timestampStr.toDoubleOrNull()
            if (d != null) {
                if (d < 100000000000L) (d * 1000).toLong() else d.toLong()
            } else {
                return "şimdi"
            }
        }
        val now = System.currentTimeMillis()
        val diffSeconds = (now - time) / 1000
        return when {
            diffSeconds < 5 -> "şimdi"
            diffSeconds < 60 -> "$diffSeconds saniye önce"
            diffSeconds < 3600 -> "${diffSeconds / 60} dakika önce"
            diffSeconds < 86400 -> "${diffSeconds / 3600} saat önce"
            else -> "${diffSeconds / 86400} gün önce"
        }
    } catch(e: Exception) {
        return "şimdi"
    }
}

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LogsScreen(
  viewModel: NextDnsViewModel,
  modifier: Modifier = Modifier
) {
  val logs by viewModel.logs.collectAsState()
  var currentTick by remember { mutableStateOf(0L) }
  
  LaunchedEffect(Unit) {
      while(true) {
          delay(5000)
          currentTick++
      }
  }
  
  DisposableEffect(Unit) {
      viewModel.startLogsStream()
      onDispose {
          viewModel.stopLogsStream()
      }
  }

  val analytics by viewModel.analytics.collectAsState()
  val isLiveStreaming by viewModel.isLiveStreaming.collectAsState()
  val isSyncing by viewModel.isSyncing.collectAsState()
  val profiles by viewModel.profiles.collectAsState()
  val activeProfile by viewModel.activeProfile.collectAsState()

  var searchQuery by remember { mutableStateOf("") }
  var selectedDeviceFilter by remember { mutableStateOf("Tüm cihazlar") }
  var showDeviceMenu by remember { mutableStateOf(false) }
  var expandedLogId by remember { mutableStateOf<String?>(null) }
  var showLiveStreamInfo by remember { mutableStateOf(false) }

  // Dinamik cihaz listesi (Kayıtlı loglardaki ve profillerdeki gerçek cihazlar)
  val detectedDevices = remember(analytics) {
    val list = mutableListOf<String>()
    analytics.topDevices.forEach { dev ->
      list.add(dev.name)
    }
    if (list.isEmpty()) {
      list.add("Bu Cihaz")
    }
    listOf("Tüm cihazlar") + list.distinct()
  }

  val filteredLogs = remember(logs, searchQuery, selectedDeviceFilter) {
    logs.filter { log ->
      val matchesSearch = searchQuery.isBlank() ||
        log.domain.contains(searchQuery.trim(), ignoreCase = true) ||
        (log.deviceName?.contains(searchQuery.trim(), ignoreCase = true) == true) ||
        (log.blockReason?.contains(searchQuery.trim(), ignoreCase = true) == true)

      val matchesDevice = selectedDeviceFilter == "Tüm cihazlar" ||
        log.deviceName?.equals(selectedDeviceFilter, ignoreCase = true) == true

      matchesSearch && matchesDevice
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp)
  ) {
    // 1. Üst Filtre ve Arama Alanı (NextDNS Web Logs Header)
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 12.dp, bottom = 10.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
// Cihaz Seçimi Dropdown
          Box {
            Surface(
              modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                .bounceClick { showDeviceMenu = true },
              color = MaterialTheme.colorScheme.surface,
              shape = RoundedCornerShape(10.dp)
            ) {
              Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
              ) {
                Text(
                  text = selectedDeviceFilter,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 12.5.sp,
                  fontWeight = FontWeight.Medium
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
              expanded = showDeviceMenu,
              onDismissRequest = { showDeviceMenu = false },
              modifier = Modifier
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
            ) {
              detectedDevices.forEach { dev ->
                DropdownMenuItem(
                  text = {
                    Text(
                      text = dev,
                      color = if (dev == selectedDeviceFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                      fontSize = 12.sp
                    )
                  },
                  onClick = {
                    selectedDeviceFilter = dev
                    showDeviceMenu = false
                  }
                )
              }
            }
          }

        // Canlı Akış Butonu
        Surface(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .border(
              1.dp,
              if (isLiveStreaming) MaterialTheme.colorScheme.primary.copy(alpha = 0.6f) else MaterialTheme.colorScheme.outline,
              RoundedCornerShape(10.dp)
            )
            .bounceClick { viewModel.toggleLiveStream() },
          color = if (isLiveStreaming) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surface,
          shape = RoundedCornerShape(10.dp)
        ) {
          Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Box(
              modifier = Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (isLiveStreaming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline)
            )
            Text(
              text = if (isLiveStreaming) "Canlı Akış: Açık" else "Canlı Akış: Kapalı",
              color = if (isLiveStreaming) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.5.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        // Canlı Akış Bilgi İkonu
        IconButton(
          onClick = { showLiveStreamInfo = !showLiveStreamInfo },
          modifier = Modifier.size(34.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Info,
            contentDescription = "Canlı Yayın Açıklaması",
            tint = if (showLiveStreamInfo) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(18.dp)
          )
        }

        Spacer(modifier = Modifier.weight(1f))

        // Manuel Yenile Butonu
        IconButton(
          onClick = { viewModel.refreshLogs() },
          modifier = Modifier
            .size(38.dp)
            .bounceClick { viewModel.refreshLogs() }
        ) {
          Icon(
            imageVector = Icons.Default.Refresh,
            contentDescription = "Yenile",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // Canlı Akış Bilgilendirme Kartı
      AnimatedVisibility(visible = showLiveStreamInfo) {
        Surface(
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(10.dp),
          border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)),
          modifier = Modifier.fillMaxWidth()
        ) {
          Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
              Icon(Icons.Default.Sensors, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
              Text("Canlı Günlük Akışı Nedir?", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
            Text(
              text = "• AÇIK olduğunda: Cihazlarınızdan gelen yeni DNS sorguları her 2.5 saniyede bir otomatik olarak ekranınıza gerçek zamanlı akar.\n• KAPALI olduğunda: Günlükler sabit kalır, yalnızca 'Yenile' butonuna bastığınızda güncellenir.",
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = 11.sp,
              lineHeight = 15.sp
            )
          }
        }
      }

      // Arama Input Kutusu
      OutlinedTextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = { Text("Bir alan adını veya cihazı filtrele...", color = MaterialTheme.colorScheme.outline, fontSize = 12.5.sp) },
        leadingIcon = {
          Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(18.dp))
        },
        trailingIcon = {
          if (searchQuery.isNotBlank()) {
            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(22.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Temizle", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
            }
          }
        },
        singleLine = true,
        modifier = Modifier
          .fillMaxWidth(),
        colors = OutlinedTextFieldDefaults.colors(
          focusedBorderColor = MaterialTheme.colorScheme.primary,
          unfocusedBorderColor = MaterialTheme.colorScheme.outline,
          focusedTextColor = MaterialTheme.colorScheme.onSurface,
          unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
          focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
          unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
      )
    }

    // 2. Log Listesi (NextDNS Web Log Stream)
    if (filteredLogs.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.outline, modifier = Modifier.size(36.dp))
          Text(
            text = if (searchQuery.isNotBlank()) "Aramanızla eşleşen log kaydı bulunamadı." else "Henüz günlük kaydı yok.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp
          )
        }
      }
    } else {
      val listState = rememberLazyListState()

      LaunchedEffect(filteredLogs.size) {
        if (filteredLogs.isNotEmpty()) {
          listState.animateScrollToItem(0)
        }
      }

      LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        items(filteredLogs, key = { it.id }) { log ->
          val isExpanded = expandedLogId == log.id
          LogItemRow(
            log = log,
            currentTick = currentTick,
            isExpanded = isExpanded,
            onToggleExpand = {
              expandedLogId = if (isExpanded) null else log.id
            },
            onAddToAllowlist = {
              viewModel.addToAllowlist(log.domain)
            },
            onAddToDenylist = {
              viewModel.addToDenylist(log.domain)
            },
            modifier = Modifier.animateItemPlacement()
          )
        }
      }
    }
  }
}

@Composable
private fun LogItemRow(
  log: DnsLogEntry,
  currentTick: Long,
  isExpanded: Boolean,
  onToggleExpand: () -> Unit,
  onAddToAllowlist: () -> Unit,
  onAddToDenylist: () -> Unit,
  modifier: Modifier = Modifier
) {
  val isBlocked = log.blocked
  val indicatorColor = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary

  Surface(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .border(1.dp, if (isExpanded) MaterialTheme.colorScheme.primary.copy(alpha = 0.5f) else MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
      .bounceClick { onToggleExpand() },
    color = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(12.dp)
  ) {
    Column(modifier = Modifier.fillMaxWidth().padding(14.dp)) {
      // Top: Domain (Code box)
      androidx.compose.foundation.text.selection.SelectionContainer {
        Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(6.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(indicatorColor)
                )
                Spacer(modifier = Modifier.width(8.dp))
                FaviconImage(domain = log.domain, modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp)))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = log.domain,
                    color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    maxLines = 1
                )
            }
        }
      }
      
      Spacer(modifier = Modifier.height(8.dp))

      // Bottom: Device - Time
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        if (!log.deviceName.isNullOrBlank()) {
          Text(
            text = log.deviceName,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium
          )
        }
        
        Text(
          text = formatRelativeTime(log.timestamp),
          color = MaterialTheme.colorScheme.outline,
          fontSize = 10.sp
        )
      }
      
      if (isBlocked && !log.blockReason.isNullOrBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = log.blockReason,
          color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
          fontSize = 10.5.sp,
          fontWeight = FontWeight.Medium
        )
      }

      // Detay Bölümü (Tıklandığında açılır)
      AnimatedVisibility(visible = isExpanded) {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .border(
              width = 1.dp,
              color = MaterialTheme.colorScheme.outline,
              shape = RoundedCornerShape(bottomStart = 12.dp, bottomEnd = 12.dp)
            )
            .padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          HorizontalDivider(color = MaterialTheme.colorScheme.outline)

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Protokol: DNS-over-HTTPS (DoH)", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
              Text("İstemci IP: ${log.clientIp ?: "37.130.67.187"}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Yanıt Süresi: ${log.responseTimeMs ?: 14} ms", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.5.sp)
              Text("Durum: ${if (isBlocked) "Engellendi" else "İzin Verildi"}", color = if (isBlocked) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary, fontSize = 11.5.sp, fontWeight = FontWeight.Bold)
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            NextDnsOutlineButton(
              text = "İzin Verilenlere Ekle",
              onClick = onAddToAllowlist,
              borderColor = MaterialTheme.colorScheme.tertiary,
              contentColor = MaterialTheme.colorScheme.tertiary,
              icon = Icons.Default.Check
            )
            NextDnsOutlineButton(
              text = "Engellenenlere Ekle",
              onClick = onAddToDenylist,
              borderColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.error,
              icon = Icons.Default.Block
            )
          }
        }
      }
    }
  }
}
