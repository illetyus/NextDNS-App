package com.example.ui.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import com.example.data.model.AnalyticsSummary
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
  val activeProfile by viewModel.activeProfile.collectAsState()
  val analytics by viewModel.analytics.collectAsState()
  val isAnalyticsLoading by viewModel.isAnalyticsLoading.collectAsState()
  val allKnownDevices by viewModel.allKnownDevices.collectAsState()

  var selectedDeviceFilter by remember { mutableStateOf("Tüm cihazlar") }
  var selectedTimeFilter by remember { mutableStateOf("Son 30 gün") }

  val lifecycleOwner = LocalLifecycleOwner.current

  LaunchedEffect(activeProfile?.id) {
    viewModel.refreshAnalytics(selectedDeviceFilter, selectedTimeFilter)
  }

  DisposableEffect(lifecycleOwner, activeProfile?.id) {
    val observer = LifecycleEventObserver { _, event ->
      when (event) {
        Lifecycle.Event.ON_RESUME -> {
          viewModel.refreshAnalytics(selectedDeviceFilter, selectedTimeFilter)
          viewModel.startAnalyticsPolling()
        }
        Lifecycle.Event.ON_PAUSE -> viewModel.stopAnalyticsPolling()
        else -> Unit
      }
    }
    lifecycleOwner.lifecycle.addObserver(observer)

    if (lifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
      viewModel.startAnalyticsPolling()
    }

    onDispose {
      viewModel.stopAnalyticsPolling()
      lifecycleOwner.lifecycle.removeObserver(observer)
    }
  }

  var showDeviceFilterMenu by remember { mutableStateOf(false) }
  var showTimeFilterMenu by remember { mutableStateOf(false) }

  val numFormat = remember { NumberFormat.getInstance(Locale("tr", "TR")) }
  val timesList = listOf("Son 24 saat", "Son 7 gün", "Son 30 gün", "Son 3 ay")

  val devicesList = remember(allKnownDevices, analytics.topDevices) {
    val list = mutableListOf<String>()
    analytics.topDevices.forEach { dev ->
      if (dev.name.isNotBlank() && dev.name != "Bilinmeyen Cihaz" && dev.name != "Cihaz") {
        list.add(dev.name)
      }
    }
    allKnownDevices.forEach { dev ->
      if (dev.isNotBlank() && dev != "Bilinmeyen Cihaz" && dev != "Cihaz") {
        list.add(dev)
      }
    }
    listOf("Tüm cihazlar") + list.distinct()
  }

  val contentAlpha by animateFloatAsState(
    targetValue = if (isAnalyticsLoading) 0.65f else 1f,
    label = "analytics_loading_alpha"
  )

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      AnalyticsFilterBar(
        selectedDeviceFilter = selectedDeviceFilter,
        devicesList = devicesList,
        showDeviceFilterMenu = showDeviceFilterMenu,
        onToggleDeviceFilterMenu = { showDeviceFilterMenu = it },
        onSelectDevice = {
          selectedDeviceFilter = it
          viewModel.refreshAnalytics(it, selectedTimeFilter)
        },
        selectedTimeFilter = selectedTimeFilter,
        timesList = timesList,
        showTimeFilterMenu = showTimeFilterMenu,
        onToggleTimeFilterMenu = { showTimeFilterMenu = it },
        onSelectTime = {
          selectedTimeFilter = it
          viewModel.refreshAnalytics(selectedDeviceFilter, it)
        },
        isLoading = isAnalyticsLoading
      )
    }

    item {
      Box(modifier = Modifier.alpha(contentAlpha)) {
        AnalyticsOverviewCards(
          analytics = analytics,
          numFormat = numFormat
        )
      }
    }

    item {
      Box(modifier = Modifier.alpha(contentAlpha)) {
        ResolvedDomainsCard(
          analytics = analytics,
          numFormat = numFormat
        )
      }
    }

    item {
      Box(modifier = Modifier.alpha(contentAlpha)) {
        BlockedDomainsCard(
          analytics = analytics,
          numFormat = numFormat
        )
      }
    }

    item {
      Box(modifier = Modifier.alpha(contentAlpha)) {
        BlockedReasonsCard(
          analytics = analytics,
          numFormat = numFormat
        )
      }
    }

    item {
      Box(modifier = Modifier.alpha(contentAlpha)) {
        DevicesAnalyticsCard(
          analytics = analytics,
          numFormat = numFormat
        )
      }
    }

    item {
      Box(modifier = Modifier.alpha(contentAlpha)) {
        RootDomainsCard(
          analytics = analytics,
          numFormat = numFormat
        )
      }
    }

    item {
      Box(modifier = Modifier.alpha(contentAlpha)) {
        GafamDominanceCard(
          analytics = analytics,
          numFormat = numFormat
        )
      }
    }

    item {
      Box(modifier = Modifier.alpha(contentAlpha)) {
        EncryptedDnsAndDnssecCard(
          analytics = analytics
        )
      }
    }

    item {
      Box(modifier = Modifier.alpha(contentAlpha)) {
        TrafficDestinationsCard(
          analytics = analytics
        )
      }
    }
  }
}

// =========================================================================
// Modular Composable Sections
// =========================================================================

@Composable
private fun AnalyticsFilterBar(
  selectedDeviceFilter: String,
  devicesList: List<String>,
  showDeviceFilterMenu: Boolean,
  onToggleDeviceFilterMenu: (Boolean) -> Unit,
  onSelectDevice: (String) -> Unit,
  selectedTimeFilter: String,
  timesList: List<String>,
  showTimeFilterMenu: Boolean,
  onToggleTimeFilterMenu: (Boolean) -> Unit,
  onSelectTime: (String) -> Unit,
  isLoading: Boolean = false,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    Box {
      Surface(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
          .bounceClick { onToggleDeviceFilterMenu(true) },
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
        onDismissRequest = { onToggleDeviceFilterMenu(false) },
        modifier = Modifier
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
      ) {
        devicesList.forEach { dev ->
          DropdownMenuItem(
            text = { Text(dev, color = if (dev == selectedDeviceFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
            onClick = {
              onSelectDevice(dev)
              onToggleDeviceFilterMenu(false)
            }
          )
        }
      }
    }

    Box {
      Surface(
        modifier = Modifier
          .clip(RoundedCornerShape(10.dp))
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
          .bounceClick { onToggleTimeFilterMenu(true) },
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
        onDismissRequest = { onToggleTimeFilterMenu(false) },
        modifier = Modifier
          .background(MaterialTheme.colorScheme.surface)
          .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
      ) {
        timesList.forEach { tm ->
          DropdownMenuItem(
            text = { Text(tm, color = if (tm == selectedTimeFilter) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
            onClick = {
              onSelectTime(tm)
              onToggleTimeFilterMenu(false)
            }
          )
        }
      }
    }

    if (isLoading) {
      CircularProgressIndicator(
        modifier = Modifier.size(16.dp),
        strokeWidth = 2.dp,
        color = MaterialTheme.colorScheme.primary
      )
    }
  }
}

@Composable
private fun AnalyticsOverviewCards(
  analytics: AnalyticsSummary,
  numFormat: NumberFormat,
  modifier: Modifier = Modifier
) {
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(10.dp)
  ) {
    Surface(
      modifier = Modifier
        .weight(1f)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(14.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = numFormat.format(analytics.totalQueries),
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

    Surface(
      modifier = Modifier
        .weight(1f)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(14.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = numFormat.format(analytics.blockedQueries),
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

    Surface(
      modifier = Modifier
        .weight(1f)
        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(14.dp)),
      color = MaterialTheme.colorScheme.surfaceVariant,
      shape = RoundedCornerShape(14.dp)
    ) {
      Column(modifier = Modifier.padding(14.dp)) {
        Text(
          text = "%${String.format(Locale("tr", "TR"), "%.2f", analytics.blockRate.coerceIn(0.0, 100.0))}",
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

@Composable
private fun ResolvedDomainsCard(
  analytics: AnalyticsSummary,
  numFormat: NumberFormat,
  modifier: Modifier = Modifier
) {
  NextDnsCard(title = "Çözümlenmiş Alan Adları", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      analytics.topAllowedDomains.take(6).forEach { dom ->
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

@Composable
private fun BlockedDomainsCard(
  analytics: AnalyticsSummary,
  numFormat: NumberFormat,
  modifier: Modifier = Modifier
) {
  NextDnsCard(title = "Engellenen Alan Adları", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      analytics.topBlockedDomains.take(6).forEach { dom ->
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

@Composable
private fun BlockedReasonsCard(
  analytics: AnalyticsSummary,
  numFormat: NumberFormat,
  modifier: Modifier = Modifier
) {
  NextDnsCard(title = "Engellenme Nedenleri", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      analytics.topBlockedReasons.forEach { (reason, count) ->
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

@Composable
private fun DevicesAnalyticsCard(
  analytics: AnalyticsSummary,
  numFormat: NumberFormat,
  modifier: Modifier = Modifier
) {
  NextDnsCard(title = "Cihazlar", modifier = modifier) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      analytics.topDevices.forEach { dev ->
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

@Composable
private fun RootDomainsCard(
  analytics: AnalyticsSummary,
  numFormat: NumberFormat,
  modifier: Modifier = Modifier
) {
  NextDnsCard(title = "Kök Alan Adları", modifier = modifier) {
    if (analytics.topDomains.isEmpty()) {
      Box(modifier = Modifier.fillMaxWidth().height(100.dp), contentAlignment = Alignment.Center) {
        Text("Bu profil için henüz yeterli veri yok.", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp)
      }
    } else {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        analytics.topDomains.take(6).forEach { dom ->
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
                overflow = TextOverflow.Ellipsis
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

@Composable
private fun GafamDominanceCard(
  analytics: AnalyticsSummary,
  numFormat: NumberFormat,
  modifier: Modifier = Modifier
) {
  NextDnsCard(
    title = "GAFAM Hâkimiyeti",
    subtitle = "\"GAFAM\" (Google, Amazon, Facebook, Apple ve Microsoft), birçok popüler hizmete sahip olan 5 baskın internet şirketidir. Bazen farklı adlarla hizmet verebilirler. Örneğin, WhatsApp ve Instagram aslında Facebook'a aittir.",
    modifier = modifier
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Box(modifier = Modifier.size(100.dp), contentAlignment = Alignment.Center) {
        val outlineColor = MaterialTheme.colorScheme.outline
        val primaryColor = MaterialTheme.colorScheme.primary
        val secondaryColor = MaterialTheme.colorScheme.secondary
        val tertiaryColor = MaterialTheme.colorScheme.tertiary

        Canvas(modifier = Modifier.fillMaxSize()) {
          val stroke = 18.dp.toPx()
          val google = (analytics.gafamMetrics["Google"]?.first ?: 0.0).toFloat().coerceIn(0f, 100f)
          val fb = (analytics.gafamMetrics["Facebook"]?.first ?: 0.0).toFloat().coerceIn(0f, 100f)
          val ms = (analytics.gafamMetrics["Microsoft"]?.first ?: 0.0).toFloat().coerceIn(0f, 100f)

          val gSweep = ((google / 100f) * 360f).coerceIn(0f, 360f)
          val fbSweep = ((fb / 100f) * 360f).coerceIn(0f, (360f - gSweep).coerceAtLeast(0f))
          val msSweep = ((ms / 100f) * 360f).coerceIn(0f, (360f - gSweep - fbSweep).coerceAtLeast(0f))
          val usedSweep = (gSweep + fbSweep + msSweep).coerceIn(0f, 360f)
          val othersSweep = (360f - usedSweep).coerceIn(0f, 360f)

          drawArc(color = outlineColor, startAngle = -90f + usedSweep, sweepAngle = othersSweep, useCenter = false, style = Stroke(width = stroke))
          if (gSweep > 0f) drawArc(color = primaryColor, startAngle = -90f, sweepAngle = gSweep, useCenter = false, style = Stroke(width = stroke))
          if (fbSweep > 0f) drawArc(color = secondaryColor, startAngle = -90f + gSweep, sweepAngle = fbSweep, useCenter = false, style = Stroke(width = stroke))
          if (msSweep > 0f) drawArc(color = tertiaryColor, startAngle = -90f + gSweep + fbSweep, sweepAngle = msSweep, useCenter = false, style = Stroke(width = stroke))
        }
      }

      Spacer(modifier = Modifier.width(24.dp))

      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        val gafamMetrics = analytics.gafamMetrics
        fun getGafam(key: String): Pair<Double, Long>? {
          return gafamMetrics.entries.find { it.key.contains(key, ignoreCase = true) }?.value
        }
        val google = getGafam("Google") ?: Pair(0.0, 0L)
        val fb = getGafam("Facebook") ?: Pair(0.0, 0L)
        val ms = getGafam("Microsoft") ?: Pair(0.0, 0L)
        val apple = getGafam("Apple") ?: Pair(0.0, 0L)
        val amazon = getGafam("Amazon") ?: Pair(0.0, 0L)

        val totalGafamQueries = google.second + fb.second + ms.second + apple.second + amazon.second
        val totalQueries = analytics.totalQueries.takeIf { it > 0 } ?: 1L
        val othersQueries = (totalQueries - totalGafamQueries).coerceAtLeast(0L)
        val othersPct = ((othersQueries.toDouble() / totalQueries) * 100.0).coerceIn(0.0, 100.0)

        val others = Pair(othersPct, othersQueries)

        val gafamList = listOf(
          Triple("Google", "%${String.format(Locale("tr", "TR"), "%.2f", google.first.coerceIn(0.0, 100.0))} (${numFormat.format(google.second)} sorgu)", MaterialTheme.colorScheme.primary),
          Triple("Facebook", "%${String.format(Locale("tr", "TR"), "%.2f", fb.first.coerceIn(0.0, 100.0))} (${numFormat.format(fb.second)} sorgu)", MaterialTheme.colorScheme.secondary),
          Triple("Microsoft", "%${String.format(Locale("tr", "TR"), "%.2f", ms.first.coerceIn(0.0, 100.0))} (${numFormat.format(ms.second)} sorgu)", MaterialTheme.colorScheme.tertiary),
          Triple("Apple", "%${String.format(Locale("tr", "TR"), "%.2f", apple.first.coerceIn(0.0, 100.0))} (${numFormat.format(apple.second)} sorgu)", MaterialTheme.colorScheme.outline),
          Triple("Amazon", "%${String.format(Locale("tr", "TR"), "%.2f", amazon.first.coerceIn(0.0, 100.0))} (${numFormat.format(amazon.second)} sorgu)", MaterialTheme.colorScheme.error),
          Triple("Diğerleri", "%${String.format(Locale("tr", "TR"), "%.2f", others.first.coerceIn(0.0, 100.0))} (${numFormat.format(others.second)} sorgu)", MaterialTheme.colorScheme.onSurfaceVariant)
        )
        gafamList.forEach { (name, stats, dotColor) ->
          Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
            Text(
              text = buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)) { append(name) }
                append(" ")
                withStyle(SpanStyle(color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(stats) }
              },
              fontSize = 11.5.sp
            )
          }
        }
      }
    }
  }
}

@Composable
private fun EncryptedDnsAndDnssecCard(
  analytics: AnalyticsSummary,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    NextDnsCard(title = "Şifrelenmiş DNS") {
      Text(
        text = "Şifrelenmiş bir aktarım kullanılarak yapılan sorguların yüzdesi (HTTPS üzerinden DNS, TLS üzerinden DNS veya resmi NextDNS uygulamaları).",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        modifier = Modifier.padding(bottom = 12.dp)
      )
      Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.outline)) {
        val encPct = (analytics.encryptedDnsPercentage / 100f).coerceIn(0f, 1f)
        Box(modifier = Modifier.fillMaxWidth(encPct).fillMaxHeight().background(MaterialTheme.colorScheme.tertiary))
      }
      Text(
        text = "%${String.format(Locale("tr", "TR"), "%.2f", analytics.encryptedDnsPercentage)}",
        color = MaterialTheme.colorScheme.tertiary,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp)
      )
    }

    NextDnsCard(title = "DNSSEC") {
      Text(
        text = "DNSSEC ile doğrulanan sorguların yüzdesi.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        modifier = Modifier.padding(bottom = 12.dp)
      )
      Box(modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.outline)) {
        val secPct = (analytics.dnssecPercentage / 100f).coerceIn(0f, 1f)
        Box(modifier = Modifier.fillMaxWidth(secPct).fillMaxHeight().background(MaterialTheme.colorScheme.primary))
      }
      Text(
        text = "%${String.format(Locale("tr", "TR"), "%.2f", analytics.dnssecPercentage)}",
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        modifier = Modifier.padding(top = 4.dp)
      )
    }
  }
}

@Composable
private fun TrafficDestinationsCard(
  analytics: AnalyticsSummary,
  modifier: Modifier = Modifier
) {
  NextDnsCard(title = "Trafik Varış Noktaları", modifier = modifier) {
    Text(
      text = "İnternet trafiğinizin gittiği ülkeler.",
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 11.sp,
      modifier = Modifier.padding(bottom = 12.dp)
    )
    WorldMapChart(countryData = analytics.topCountries)
    if (analytics.topCountries.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        analytics.topCountries.take(5).forEach { item ->
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
