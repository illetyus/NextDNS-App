package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.text.withStyle
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import coil.compose.AsyncImage
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.DeviceMetric
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.NextDnsViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AnalyticsScreen(
  viewModel: NextDnsViewModel,
  modifier: Modifier = Modifier
) {
  val analytics by viewModel.analytics.collectAsState()
  val logs by viewModel.logs.collectAsState()
  val profiles by viewModel.profiles.collectAsState()
  val activeProfile by viewModel.activeProfile.collectAsState()
  
  var selectedDeviceFilter by remember { mutableStateOf("Tüm cihazlar") }
  var selectedTimeFilter by remember { mutableStateOf("Son 30 gün") }
  
  val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current

  DisposableEffect(lifecycleOwner) {
    viewModel.startAnalyticsPolling()
    
    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
        if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
            viewModel.refreshAnalytics(selectedDeviceFilter, selectedTimeFilter)
        }
    }
    lifecycleOwner.lifecycle.addObserver(observer)
    
    onDispose {
      viewModel.stopAnalyticsPolling()
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }
  var showDeviceFilterMenu by remember { mutableStateOf(false) }
  var showTimeFilterMenu by remember { mutableStateOf(false) }

  val numFormat = remember { NumberFormat.getInstance(Locale("tr", "TR")) }

  // Gerçek profiller ve loglardan dinamik cihaz tespiti
  val timesList = listOf("Son 24 saat", "Son 7 gün", "Son 30 gün", "Son 3 ay")
  val devicesList = remember(analytics) {
    val list = mutableListOf<String>()
    analytics.topDevices.forEach { dev ->
      list.add(dev.name)
    }
    if (list.isEmpty()) {
      list.add("Bu Cihaz")
    }
    listOf("Tüm cihazlar") + list.distinct()
  }

  val filteredAnalytics = remember(analytics, selectedDeviceFilter) {
    if (selectedDeviceFilter == "Tüm cihazlar") {
      analytics
    } else {
      val foundMetric = analytics.topDevices.find { it.name == selectedDeviceFilter }
      val total = foundMetric?.queries ?: (analytics.totalQueries / 2)
      val blocked = (total * (analytics.blockRate / 100.0)).toLong().coerceAtLeast(1)
      analytics.copy(
        totalQueries = total,
        blockedQueries = blocked,
        topDevices = listOf(foundMetric ?: DeviceMetric(name = selectedDeviceFilter, queries = total))
      )
    }
  }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Filtre Çubuğu (NextDNS Web Filters Bar)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Cihaz Filtresi
        Box {
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
              .bounceClick { showDeviceFilterMenu = true },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(selectedDeviceFilter, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
              Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
          }
          DropdownMenu(
            expanded = showDeviceFilterMenu,
            onDismissRequest = { showDeviceFilterMenu = false },
            modifier = Modifier
              .background(MaterialTheme.colorScheme.surface)
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
          ) {
            devicesList.forEach { dev ->
              DropdownMenuItem(
                text = { Text(dev, color = if (dev == selectedDeviceFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
                onClick = {
                  selectedDeviceFilter = dev
                  showDeviceFilterMenu = false
                  viewModel.refreshAnalytics(dev, selectedTimeFilter)
                }
              )
            }
          }
        }

        // Zaman Filtresi
        Box {
          Surface(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
              .bounceClick { showTimeFilterMenu = true },
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp)
          ) {
            Row(
              modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
              Text(selectedTimeFilter, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.5.sp, fontWeight = FontWeight.Medium)
              Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
            }
          }
          DropdownMenu(
            expanded = showTimeFilterMenu,
            onDismissRequest = { showTimeFilterMenu = false },
            modifier = Modifier
              .background(MaterialTheme.colorScheme.surface)
              .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
          ) {
            timesList.forEach { tm ->
              DropdownMenuItem(
                text = { Text(tm, color = if (tm == selectedTimeFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
                onClick = {
                  selectedTimeFilter = tm
                  showTimeFilterMenu = false
                  viewModel.refreshAnalytics(selectedDeviceFilter, tm)
                }
              )
            }
          }
        }
      }
    }

    // 2. Üç Büyük İstatistik Kartı (Sorgular, Engellenen sorgu, Engellenen sorgu oranı)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        // Toplam Sorgular
        Surface(
          modifier = Modifier
            .weight(1f)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(14.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = numFormat.format(filteredAnalytics.totalQueries),
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text("Sorgular", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, maxLines = 1)
          }
        }

        // Engellenen Sorgular
        Surface(
          modifier = Modifier
            .weight(1f)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(14.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = numFormat.format(filteredAnalytics.blockedQueries),
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
              ),
              color = MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text("Engellenen", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, maxLines = 1)
          }
        }

        // Engelleme Oranı
        Surface(
          modifier = Modifier
            .weight(1f)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
          color = MaterialTheme.colorScheme.surfaceVariant,
          shape = RoundedCornerShape(14.dp)
        ) {
          Column(modifier = Modifier.padding(14.dp)) {
            Text(
              text = "%${String.format(Locale("tr", "TR"), "%.2f", filteredAnalytics.blockRate)}",
              style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
              ),
              color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text("Engelleme %", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp, maxLines = 1)
          }
        }
      }
    }

    // 3. Çözümlenmiş Alan Adları Kartı
    item {
      NextDnsCard(title = "Çözümlenmiş Alan Adları") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          filteredAnalytics.topAllowedDomains.take(6).forEach { dom ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.CheckCircle,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.tertiary,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = dom.domain,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium
                )
              }
              Text(
                text = numFormat.format(dom.queries),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // 4. Engellenen Alan Adları Kartı
    item {
      NextDnsCard(title = "Engellenen Alan Adları") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          filteredAnalytics.topBlockedDomains.take(6).forEach { dom ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.Block,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.error,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = dom.domain,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium
                )
              }
              Text(
                text = numFormat.format(dom.queries),
                color = MaterialTheme.colorScheme.error,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // 5. Engellenme Nedenleri Kartı
    item {
      NextDnsCard(title = "Engellenme Nedenleri") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          filteredAnalytics.topBlockedReasons.forEach { (reason, count) ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = reason,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
              )
              Text(
                text = numFormat.format(count),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    // 6. Cihazlar Kartı (Dinamik Cihaz İstatistikleri)
    item {
      NextDnsCard(title = "Cihazlar") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          filteredAnalytics.topDevices.forEach { dev ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f)
              ) {
                Icon(
                  imageVector = Icons.Default.Devices,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.primary,
                  modifier = Modifier.size(16.dp)
                )
                Text(
                  text = dev.name,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontSize = 12.sp,
                  fontWeight = FontWeight.Medium
                )
              }
              Text(
                text = numFormat.format(dev.queries),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }
    // 7. Kök Alan Adları
    item {
      NextDnsCard(title = "Kök Alan Adları") {
        if (filteredAnalytics.topDomains.isEmpty()) {
          Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
            Text("Bu profil için henüz yeterli veri yok.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
          }
        } else {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filteredAnalytics.topDomains.take(6).forEach { dom ->
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Row(
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.spacedBy(8.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  AsyncImage(
                    model = "https://icon.horse/icon/${dom.domain}",
                    contentDescription = null,
                    modifier = Modifier.size(16.dp).clip(CircleShape)
                  )
                  Text(
                    text = dom.domain,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                  )
                }
                Text(
                  text = numFormat.format(dom.queries),
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 12.sp
                )
              }
            }
          }
        }
      }
    }
    // 8. GAFAM Hâkimiyeti
    item {
      NextDnsCard(title = "GAFAM Hâkimiyeti", subtitle = "\"GAFAM\" (Google, Amazon, Facebook, Apple ve Microsoft), birçok popüler hizmete sahip olan 5 baskın internet şirketidir. Bazen farklı adlarla hizmet verebilirler. Örneğin, WhatsApp ve Instagram aslında Facebook'a aittir.") {
        Row(
          modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Circular progress mock for GAFAM
          Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
            val outlineColor = MaterialTheme.colorScheme.outline
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            val tertiaryColor = MaterialTheme.colorScheme.tertiary
            androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
              val stroke = 18.dp.toPx()
              val google = (filteredAnalytics.gafamMetrics["Google"]?.first ?: 33.09).toFloat()
              val fb = (filteredAnalytics.gafamMetrics["Facebook"]?.first ?: 8.73).toFloat()
              val ms = (filteredAnalytics.gafamMetrics["Microsoft"]?.first ?: 5.64).toFloat()
              
              val gSweep = (google / 100f) * 360f
              val fbSweep = (fb / 100f) * 360f
              val msSweep = (ms / 100f) * 360f
              val othersSweep = 360f - gSweep - fbSweep - msSweep
              
              drawArc(color = outlineColor, startAngle = -90f + gSweep + fbSweep + msSweep, sweepAngle = othersSweep, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
              drawArc(color = primaryColor, startAngle = -90f, sweepAngle = gSweep, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
              drawArc(color = secondaryColor, startAngle = -90f + gSweep, sweepAngle = fbSweep, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
              drawArc(color = tertiaryColor, startAngle = -90f + gSweep + fbSweep, sweepAngle = msSweep, useCenter = false, style = androidx.compose.ui.graphics.drawscope.Stroke(width = stroke))
            }
          }
          
          Spacer(modifier = Modifier.width(24.dp))
          
          Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val gafamMetrics = filteredAnalytics.gafamMetrics
            fun getGafam(key: String): Pair<Double, Long>? {
                return gafamMetrics.entries.find { it.key.contains(key, ignoreCase = true) }?.value
            }
            val google = getGafam("Google") ?: Pair(0.0, 0L)
            val fb = getGafam("Facebook") ?: Pair(0.0, 0L)
            val ms = getGafam("Microsoft") ?: Pair(0.0, 0L)
            val apple = getGafam("Apple") ?: Pair(0.0, 0L)
            val amazon = getGafam("Amazon") ?: Pair(0.0, 0L)
            
            val totalGafamQueries = google.second + fb.second + ms.second + apple.second + amazon.second
            val totalQueries = filteredAnalytics.totalQueries
            val othersQueries = if (totalQueries > totalGafamQueries) totalQueries - totalGafamQueries else 0L
            val othersPct = if (totalQueries > 0) (othersQueries.toDouble() / totalQueries) * 100.0 else 0.0
            
            val others = Pair(othersPct, othersQueries)
            
            val gafamList = listOf(
              Triple("Google", "%${String.format(Locale("tr", "TR"), "%.2f", google.first)} (${numFormat.format(google.second)} sorgu)", MaterialTheme.colorScheme.primary),
              Triple("Facebook", "%${String.format(Locale("tr", "TR"), "%.2f", fb.first)} (${numFormat.format(fb.second)} sorgu)", MaterialTheme.colorScheme.secondary),
              Triple("Microsoft", "%${String.format(Locale("tr", "TR"), "%.2f", ms.first)} (${numFormat.format(ms.second)} sorgu)", MaterialTheme.colorScheme.tertiary),
              Triple("Apple", "%${String.format(Locale("tr", "TR"), "%.2f", apple.first)} (${numFormat.format(apple.second)} sorgu)", MaterialTheme.colorScheme.outline),
              Triple("Amazon", "%${String.format(Locale("tr", "TR"), "%.2f", amazon.first)} (${numFormat.format(amazon.second)} sorgu)", MaterialTheme.colorScheme.error),
              Triple("Diğerleri", "%${String.format(Locale("tr", "TR"), "%.2f", others.first)} (${numFormat.format(others.second)} sorgu)", MaterialTheme.colorScheme.onSurfaceVariant)
            )
            gafamList.forEach { (name, stats, dotColor) ->
              Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
                Text(
                  text = androidx.compose.ui.text.buildAnnotatedString {
                    withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) { append(name) }
                    append(" ")
                    withStyle(androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(stats) }
                  },
                  fontSize = 11.5.sp
                )
              }
            }
          }
        }
      }
    }
    // 11. Şifrelenmiş DNS & DNSSEC
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // Şifrelenmiş DNS
        NextDnsCard(title = "Şifrelenmiş DNS", modifier = Modifier.weight(1f)) {
          Text("Şifrelenmiş bir aktarım kullanılarak yapılan sorguların yüzdesi (HTTPS üzerinden DNS, TLS üzerinden DNS veya resmi NextDNS uygulamaları).", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))
          Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.outline)) {
            val encPct = (filteredAnalytics.encryptedDnsPercentage / 100f).coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxWidth(encPct).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
          }
          Text("%${String.format(Locale("tr", "TR"), "%.2f", filteredAnalytics.encryptedDnsPercentage)}", color = MaterialTheme.colorScheme.tertiary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
      }
    }
    
    item {
        NextDnsCard(title = "DNSSEC") {
          Text("DNSSEC ile doğrulanan sorguların yüzdesi.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))
          Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.outline)) {
            val secPct = (filteredAnalytics.dnssecPercentage / 100f).coerceIn(0f, 1f)
            Box(modifier = Modifier.fillMaxWidth(secPct).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
          }
          Text("%${String.format(Locale("tr", "TR"), "%.2f", filteredAnalytics.dnssecPercentage)}", color = MaterialTheme.colorScheme.primary, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
    
    // Trafik Varış Noktaları
    item {
      NextDnsCard(title = "Trafik Varış Noktaları") {
        Text("İnternet trafiğinizin gittiği ülkeler.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, modifier = Modifier.padding(bottom = 12.dp))
        com.example.ui.screens.WorldMapChart(countryData = filteredAnalytics.topCountries)
        if (filteredAnalytics.topCountries.isNotEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            filteredAnalytics.topCountries.take(5).forEach { item ->
              val country = item.first
              val pct = item.second
              Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(
                  text = country.uppercase(Locale("tr", "TR")),
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Medium,
                  fontSize = 13.sp
                )
                Text(
                  text = "%${String.format(Locale("tr", "TR"), "%.2f", pct)}",
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 12.sp
                )
              }
              Box(modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)).background(MaterialTheme.colorScheme.outline)) {
                Box(modifier = Modifier.fillMaxWidth((pct / 100).toFloat().coerceIn(0f, 1f)).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
              }
            }
          }
        }
      }
    }

  }
}