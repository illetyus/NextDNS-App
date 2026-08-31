package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BlocklistEntry
import com.example.data.model.NativeTrackingEntry
import com.example.data.model.PrivacySettings
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.NextDnsViewModel

@Composable
fun PrivacyScreen(
  viewModel: NextDnsViewModel,
  modifier: Modifier = Modifier
) {
  val settings by viewModel.privacySettings.collectAsState()
  var showAddBlocklistDialog by remember { mutableStateOf(false) }
  var showAddNativeDialog by remember { mutableStateOf(false) }

  val activeBlocklists = settings.blocklists.filter { it.active }
  val activeNatives = settings.nativeTracking.filter { it.active }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    item {
      BlocklistsSection(
        activeBlocklists = activeBlocklists,
        onToggleBlocklist = { viewModel.toggleBlocklist(it) },
        onOpenAddDialog = { showAddBlocklistDialog = true }
      )
    }

    item {
      NativeTrackingSection(
        activeNatives = activeNatives,
        onToggleNative = { viewModel.toggleNativeTracking(it) },
        onOpenAddDialog = { showAddNativeDialog = true }
      )
    }

    item {
      DisguisedTrackersSection(
        disguisedTrackers = settings.disguisedTrackers,
        onToggleDisguisedTrackers = { viewModel.toggleDisguisedTrackers(it) }
      )
    }

    item {
      AffiliatesSection(
        allowAffiliates = settings.allowAffiliates,
        onToggleAllowAffiliates = { viewModel.toggleAllowAffiliates(it) }
      )
    }
  }

  if (showAddBlocklistDialog) {
    AddBlocklistCatalogDialog(
      blocklists = settings.blocklists,
      onToggleBlocklist = { viewModel.toggleBlocklist(it) },
      onDismiss = { showAddBlocklistDialog = false }
    )
  }

  if (showAddNativeDialog) {
    AddNativeTrackingDialog(
      nativeTrackings = settings.nativeTracking,
      onToggleNative = { viewModel.toggleNativeTracking(it) },
      onDismiss = { showAddNativeDialog = false }
    )
  }
}

// =========================================================================
// Modular Screen Sections
// =========================================================================

@Composable
private fun BlocklistsSection(
  activeBlocklists: List<BlocklistEntry>,
  onToggleBlocklist: (String) -> Unit,
  onOpenAddDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  NextDnsCard(
    modifier = modifier,
    title = "Engelleme Listeleri",
    subtitle = "Tümü gerçek zamanlı olarak güncellenen mevcut en popüler engelleme listelerini kullanarak reklamları ve izleyicileri engelleyin."
  ) {
    if (activeBlocklists.isNotEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        activeBlocklists.forEach { item ->
          BlocklistItemCard(item = item, onRemove = { onToggleBlocklist(item.id) })
        }
      }
    } else {
      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp)
      ) {
        Text(
          text = "Henüz aktif engelleme listesi eklenmedi. 'Engelleme Listesi Ekle' butonuna basarak popüler listeleri etkinleştirebilirsiniz.",
          color = MaterialTheme.colorScheme.outline,
          fontSize = 12.sp,
          modifier = Modifier.padding(12.dp)
        )
      }
    }

    NextDnsButton(
      text = "ENGELLEME LİSTESİ EKLE / YÖNET",
      onClick = onOpenAddDialog,
      icon = Icons.Default.Add
    )
  }
}

@Composable
private fun BlocklistItemCard(
  item: BlocklistEntry,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier
) {
  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        modifier = Modifier.weight(1f),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        Box(
          modifier = Modifier
            .width(3.5.dp)
            .height(42.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            Text(
              text = item.name,
              color = MaterialTheme.colorScheme.onSurface,
              fontWeight = FontWeight.Bold,
              fontSize = 13.5.sp
            )
            Surface(
              color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
              shape = RoundedCornerShape(4.dp)
            ) {
              Text(
                text = item.category,
                color = MaterialTheme.colorScheme.primary,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
              )
            }
          }
          Text(
            text = item.description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.5.sp,
            lineHeight = 15.sp
          )
          if (!item.website.isNullOrBlank()) {
            Text(
              text = item.website,
              color = MaterialTheme.colorScheme.primary,
              fontSize = 11.sp
            )
          }
          Text(
            text = "${item.entriesCount} kural • Gerçek zamanlı senkronize",
            color = MaterialTheme.colorScheme.outline,
            fontSize = 10.5.sp
          )
        }
      }

      IconButton(
        onClick = onRemove,
        modifier = Modifier.size(32.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Kaldır",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

@Composable
private fun NativeTrackingSection(
  activeNatives: List<NativeTrackingEntry>,
  onToggleNative: (String) -> Unit,
  onOpenAddDialog: () -> Unit,
  modifier: Modifier = Modifier
) {
  NextDnsCard(
    modifier = modifier,
    title = "Yerel İzleme Koruması",
    subtitle = "Bir cihazdaki etkinliğinizi izleyen, genellikle işletim sistemi düzeyinde çalışan geniş spektrumlu izleyicileri engelleyin. Bu etkinlikler ziyaret ettiğiniz siteleri, telemetriyi veya arka plan raporlarını içerebilir.",
    isBeta = true
  ) {
    if (activeNatives.isNotEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        activeNatives.forEach { nat ->
          NativeTrackingItemCard(nat = nat, onRemove = { onToggleNative(nat.id) })
        }
      }
    }

    NextDnsButton(
      text = "YEREL İZLEME KORUMASI EKLE",
      onClick = onOpenAddDialog,
      icon = Icons.Default.Add
    )
  }
}

@Composable
private fun NativeTrackingItemCard(
  nat: NativeTrackingEntry,
  onRemove: () -> Unit,
  modifier: Modifier = Modifier
) {
  val domain = when (nat.id) {
    "windows" -> "microsoft.com"
    "apple" -> "apple.com"
    "samsung" -> "samsung.com"
    "xiaomi" -> "mi.com"
    "huawei" -> "huawei.com"
    "roku" -> "roku.com"
    "sonos" -> "sonos.com"
    else -> "nextdns.io"
  }

  Surface(
    color = MaterialTheme.colorScheme.surfaceVariant,
    shape = RoundedCornerShape(12.dp),
    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
    modifier = modifier.fillMaxWidth()
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(14.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        Box(
          modifier = Modifier
            .width(3.5.dp)
            .height(32.dp)
            .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(2.dp))
        )
        FaviconImage(
          domain = domain,
          modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
        )
        Column {
          Text(
            text = nat.name,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
          )
          Text(
            text = nat.description,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 11.sp
          )
        }
      }

      IconButton(
        onClick = onRemove,
        modifier = Modifier.size(32.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "Kaldır",
          tint = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

@Composable
private fun DisguisedTrackersSection(
  disguisedTrackers: Boolean,
  onToggleDisguisedTrackers: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  NextDnsSettingToggle(
    modifier = modifier,
    title = "Gizlenmiş Üçüncü Taraf İzleyicileri Engelle",
    subtitle = "Güncel tarayıcıların gizlilik korumalarını atlatmak için CNAME veya birinci taraf olarak gizlenen izleyicileri otomatik olarak algılayın ve engelleyin.",
    checked = disguisedTrackers,
    onCheckedChange = onToggleDisguisedTrackers
  )
}

@Composable
private fun AffiliatesSection(
  allowAffiliates: Boolean,
  onToggleAllowAffiliates: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  NextDnsCard(
    modifier = modifier,
    title = "Ortaklık ve İzleme Bağlantılarına İzin Ver",
    subtitle = "Fırsat web sitelerinde, e-postalarda veya arama sonuçlarında yaygın olan satış ortağı ve izleme alanlarına izin verin. Bunlar genellikle yalnızca bir bağlantıya elle tıklandıktan sonra aranır."
  ) {
    Surface(
      color = Color(0xFF2E1065).copy(alpha = 0.4f),
      shape = RoundedCornerShape(8.dp),
      border = BorderStroke(1.dp, Color(0xFFA855F7).copy(alpha = 0.3f)),
      modifier = Modifier
        .fillMaxWidth()
        .padding(bottom = 12.dp)
    ) {
      Row(
        modifier = Modifier.padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        Icon(
          imageVector = Icons.Default.Lock,
          contentDescription = null,
          tint = Color(0xFFA855F7),
          modifier = Modifier.size(16.dp)
        )
        Text(
          text = "IP adresiniz, gizliliğinizi korumak için bu web sitelerinden otomatik olarak gizlenecektir.",
          color = Color(0xFFE9D5FF),
          fontSize = 11.5.sp,
          fontWeight = FontWeight.Medium
        )
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .bounceClick { onToggleAllowAffiliates(!allowAffiliates) }
        .padding(vertical = 4.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(
        text = if (allowAffiliates) "Ortaklık bağlantılarına izin veriliyor" else "Ortaklık bağlantılarına izin verilmiyor",
        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 13.sp),
        color = if (allowAffiliates) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
      )
      NextDnsSwitch(
        checked = allowAffiliates,
        onCheckedChange = onToggleAllowAffiliates
      )
    }
  }
}

// =========================================================================
// Dialogs
// =========================================================================

@Composable
private fun AddBlocklistCatalogDialog(
  blocklists: List<BlocklistEntry>,
  onToggleBlocklist: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var searchQuery by remember { mutableStateOf("") }
  var selectedCategory by remember { mutableStateOf("Tümü") }
  val categories = listOf("Tümü", "Genel", "Güvenlik", "Gizlilik", "Bölgesel")

  val filteredList = blocklists.filter { entry ->
    val matchesCat = selectedCategory == "Tümü" || (entry.category ?: "Genel").equals(selectedCategory, ignoreCase = true)
    val matchesQuery = searchQuery.isBlank() ||
      entry.name.contains(searchQuery, ignoreCase = true) ||
      entry.description.contains(searchQuery, ignoreCase = true)
    matchesCat && matchesQuery
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(20.dp),
    title = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Engelleme Listesi Kataloğu", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Filtre veya liste ara...", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp) },
          singleLine = true,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
          ),
          shape = RoundedCornerShape(10.dp)
        )

        LazyRow(
          horizontalArrangement = Arrangement.spacedBy(6.dp),
          modifier = Modifier.fillMaxWidth()
        ) {
          items(categories) { cat ->
            val isSelected = selectedCategory == cat
            Surface(
              color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(8.dp),
              modifier = Modifier.clickable { selectedCategory = cat }
            ) {
              Text(
                text = cat,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.5.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
              )
            }
          }
        }
      }
    },
    text = {
      LazyColumn(
        modifier = Modifier
          .fillMaxWidth()
          .heightIn(max = 380.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(filteredList, key = { it.id }) { catItem ->
          val isAdded = catItem.active
          Surface(
            color = if (isAdded) MaterialTheme.colorScheme.primary.copy(alpha = 0.08f) else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            border = BorderStroke(
              1.dp,
              if (isAdded) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline
            ),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Row(
                  verticalAlignment = Alignment.Top,
                  horizontalArrangement = Arrangement.SpaceBetween,
                  modifier = Modifier.fillMaxWidth()
                ) {
                  Text(
                    text = catItem.name,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    modifier = Modifier.weight(1f, fill = false).padding(end = 6.dp),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                  )
                  Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(4.dp)
                  ) {
                    Text(
                      text = catItem.category,
                      color = MaterialTheme.colorScheme.primary,
                      fontSize = 9.sp,
                      fontWeight = FontWeight.Bold,
                      modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                  }
                }
                Text(
                  text = catItem.description,
                  color = MaterialTheme.colorScheme.onSurfaceVariant,
                  fontSize = 11.sp,
                  maxLines = 2,
                  modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                  Text(
                    text = "${catItem.entriesCount} girdi",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp
                  )
                  Text(
                    text = " • Son güncellenme: ${catItem.updatedTime}",
                    color = MaterialTheme.colorScheme.outline,
                    fontSize = 10.sp
                  )
                }
              }

              Spacer(modifier = Modifier.width(8.dp))

              Button(
                onClick = { onToggleBlocklist(catItem.id) },
                colors = ButtonDefaults.buttonColors(
                  containerColor = if (isAdded) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else MaterialTheme.colorScheme.primary,
                  contentColor = if (isAdded) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onPrimary
                ),
                shape = RoundedCornerShape(8.dp),
                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                modifier = Modifier.height(32.dp)
              ) {
                Text(
                  text = if (isAdded) "Kaldır" else "Ekle",
                  fontSize = 11.5.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
          }
        }
      }
    },
    confirmButton = {
      TextButton(onClick = onDismiss) {
        Text("Tamam", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {}
  )
}

@Composable
private fun AddNativeTrackingDialog(
  nativeTrackings: List<NativeTrackingEntry>,
  onToggleNative: (String) -> Unit,
  onDismiss: () -> Unit
) {
  val inactiveNatives = nativeTrackings.filter { !it.active }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(18.dp),
    title = {
      Text("Yerel İzleme Koruması Ekle", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    },
    text = {
      if (inactiveNatives.isEmpty()) {
        Text("Tüm yerel izleme korumaları zaten aktif.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
      } else {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          inactiveNatives.forEach { nat ->
            val domain = when (nat.id) {
              "windows" -> "microsoft.com"
              "apple" -> "apple.com"
              "samsung" -> "samsung.com"
              "xiaomi" -> "mi.com"
              "huawei" -> "huawei.com"
              "roku" -> "roku.com"
              "sonos" -> "sonos.com"
              else -> "nextdns.io"
            }

            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier
                .fillMaxWidth()
                .bounceClick {
                  onToggleNative(nat.id)
                  onDismiss()
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
                  horizontalArrangement = Arrangement.spacedBy(10.dp),
                  modifier = Modifier.weight(1f)
                ) {
                  FaviconImage(
                    domain = domain,
                    modifier = Modifier.size(24.dp).clip(RoundedCornerShape(4.dp))
                  )
                  Column {
                    Text(nat.name, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text(nat.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                  }
                }
                Icon(Icons.Default.Add, contentDescription = "Ekle", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      }
    },
    confirmButton = {},
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("Kapat", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  )
}
