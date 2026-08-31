package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.NextDnsViewModel

@Composable
fun ParentalScreen(
  viewModel: NextDnsViewModel,
  modifier: Modifier = Modifier
) {
  val settings by viewModel.parentalControlSettings.collectAsState()
  var showAddServiceDialog by remember { mutableStateOf(false) }
  var showAddCategoryDialog by remember { mutableStateOf(false) }
  var showRecreationDialog by remember { mutableStateOf(false) }

  val activeServices = settings.services.filter { it.active }
  val activeCategories = settings.categories.filter { it.active }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 1. Web Siteleri, Uygulamalar ve Oyunlar Kartı
    item {
      NextDnsCard(
        title = "Web Siteleri, Uygulamalar ve Oyunlar",
        subtitle = "Belirli web sitelerine, uygulamalara ve oyunlara erişimi kısıtlayın."
      ) {
        if (activeServices.isNotEmpty()) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            activeServices.forEach { service ->
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                  verticalAlignment = Alignment.CenterVertically,
                  horizontalArrangement = Arrangement.SpaceBetween
                ) {
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                  ) {
                    Box(
                      modifier = Modifier
                        .width(3.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(2.dp))
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                      FaviconImage(
                        domain = if (service.id.lowercase() == "steam") "steampowered.com" else "${service.id.lowercase()}.com",
                        modifier = Modifier.size(18.dp).clip(RoundedCornerShape(4.dp))
                      )
                      Text(
                        text = service.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                      )
                    }
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    NextDnsSwitch(
                      checked = service.active,
                      onCheckedChange = { viewModel.toggleParentalService(service.id) }
                    )
                    IconButton(
                      onClick = { viewModel.toggleParentalService(service.id) },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kaldır",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }

        NextDnsButton(
          text = "WEB SİTESİ, UYGULAMA VEYA OYUN EKLE",
          onClick = { showAddServiceDialog = true },
          icon = Icons.Default.Add
        )
      }
    }

    // 2. Kategoriler Kartı
    item {
      NextDnsCard(
        title = "Kategoriler",
        subtitle = "Belirli web siteleri ve uygulama kategorilerine erişimi kısıtlayın."
      ) {
        if (activeCategories.isNotEmpty()) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            activeCategories.forEach { category ->
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
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
                        .width(3.dp)
                        .height(28.dp)
                        .background(MaterialTheme.colorScheme.error, RoundedCornerShape(2.dp))
                    )
                    Column {
                      Text(
                        text = category.name,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp
                      )
                      Text(
                        text = category.description,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.5.sp
                      )
                    }
                  }

                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                  ) {
                    NextDnsSwitch(
                      checked = category.active,
                      onCheckedChange = { viewModel.toggleParentalCategory(category.id) }
                    )
                    IconButton(
                      onClick = { viewModel.toggleParentalCategory(category.id) },
                      modifier = Modifier.size(28.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Kaldır",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                      )
                    }
                  }
                }
              }
            }
          }
        }

        NextDnsButton(
          text = "KATEGORİ EKLE",
          onClick = { showAddCategoryDialog = true },
          icon = Icons.Default.Add
        )
      }
    }

    // 3. Rekreasyon Süresi Kartı
    item {
      NextDnsCard(
        title = "Rekreasyon Süresi",
        subtitle = "Haftanın her günü için, yukarıdaki web sitelerinin, uygulamaların, oyunların veya kategorilerin bazılarının engellenmeyeceği bir dönem belirleyin — örneğin, Facebook'a Pazartesi ve Salı günleri 18:00 ile 20:00 arasında izin verin."
      ) {
        Button(
          onClick = { showRecreationDialog = true },
          modifier = Modifier
            .height(36.dp)
            .bounceClick { showRecreationDialog = true },
          shape = RoundedCornerShape(10.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFF84CC16), // Lime green
            contentColor = Color.Black
          ),
          contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
        ) {
          Text(
            text = "REKREASYON SÜRESİNİ AYARLA",
            style = MaterialTheme.typography.labelMedium.copy(
              fontWeight = FontWeight.Black,
              fontSize = 11.sp
            )
          )
        }
      }
    }

    // 4. Güvenli Arama Kartı
    item {
      NextDnsSettingToggle(
        title = "Güvenli Arama",
        subtitle = "Resimler ve videolar dahil olmak üzere tüm büyük arama motorlarında yetişkinlere yönelik içeriği filtreleyin. Bu ayrıca, bu özelliği desteklemeyen arama motorlarına erişimi de engelleyecektir.",
        checked = settings.safeSearch,
        onCheckedChange = { viewModel.setSafeSearch(it) }
      )
    }

    // 5. YouTube Kısıtlı Modu Kartı
    item {
      NextDnsSettingToggle(
        title = "YouTube Kısıtlı Modu",
        subtitle = "YouTube'daki yetişkin içerikli videoları filtreleyin ve gömülü yetişkin içerikli videoların diğer web sitelerinde izlenmesini engelleyin. Bu aynı zamanda tüm yorumları da gizleyecektir.",
        checked = settings.youtubeRestrictedMode,
        onCheckedChange = { viewModel.setYoutubeRestricted(it) }
      )
    }

    // 6. Atlatma Yöntemlerini Engelle Kartı
    item {
      NextDnsSettingToggle(
        title = "Atlatma Yöntemlerini Engelle",
        subtitle = "Ağda NextDNS filtrelemesini atlatmaya yardımcı olabilecek yöntemlerin kullanımını önleyin veya engelleyin. Buna VPN'ler, proxy'ler, Tor ile ilgili yazılımlar ve şifreli DNS sağlayıcıları dahildir.",
        checked = settings.blockBypass,
        onCheckedChange = { viewModel.setBlockBypass(it) }
      )
    }
  }

  // Dialog: Servis Ekle
  if (showAddServiceDialog) {
    val inactiveServices = settings.services.filter { !it.active }
    AlertDialog(
      onDismissRequest = { showAddServiceDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(18.dp),
      title = {
        Text("Uygulama / Oyun Engelle", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      },
      text = {
        LazyColumn(
          modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = 350.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          items(inactiveServices) { srv ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier
                .fillMaxWidth()
                .bounceClick {
                  viewModel.toggleParentalService(srv.id)
                  showAddServiceDialog = false
                }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                  FaviconImage(
                    domain = "${srv.id.lowercase()}.com",
                    modifier = Modifier.size(16.dp).clip(RoundedCornerShape(4.dp))
                  )
                  Text(srv.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Icon(Icons.Default.Add, contentDescription = "Ekle", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showAddServiceDialog = false }) {
          Text("Kapat", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    )
  }

  // Dialog: Kategori Ekle
  if (showAddCategoryDialog) {
    val inactiveCats = settings.categories.filter { !it.active }
    AlertDialog(
      onDismissRequest = { showAddCategoryDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(18.dp),
      title = {
        Text("Kategori Engelle", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          inactiveCats.forEach { cat ->
            Surface(
              color = MaterialTheme.colorScheme.surfaceVariant,
              shape = RoundedCornerShape(10.dp),
              border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
              modifier = Modifier
                .fillMaxWidth()
                .bounceClick {
                  viewModel.toggleParentalCategory(cat.id)
                  showAddCategoryDialog = false
                }
            ) {
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Column {
                  Text(cat.name, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                  Text(cat.description, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
                }
                Icon(Icons.Default.Add, contentDescription = "Ekle", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
              }
            }
          }
        }
      },
      confirmButton = {},
      dismissButton = {
        TextButton(onClick = { showAddCategoryDialog = false }) {
          Text("Kapat", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    )
  }

  // Dialog: Rekreasyon Süresi Ayarla
  if (showRecreationDialog) {
    AlertDialog(
      onDismissRequest = { showRecreationDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(18.dp),
      title = {
        Text("Rekreasyon Süresi Ayarları", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Text("İzin verilen serbest saat aralığı:", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
          Text("Hafta içi: 18:00 - 20:00 (Aktif)", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
          Text("Hafta sonu: 14:00 - 21:00 (Aktif)", color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
        }
      },
      confirmButton = {
        NextDnsButton(text = "Kaydet", onClick = { showRecreationDialog = false })
      },
      dismissButton = {
        TextButton(onClick = { showRecreationDialog = false }) {
          Text("Kapat", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    )
  }
}
