package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.example.data.preferences.ThemePreferences
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import coil.compose.AsyncImage
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConfigSettings
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.NextDnsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  viewModel: NextDnsViewModel,
  modifier: Modifier = Modifier
) {
  
  val context = LocalContext.current
  val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
    if (uri != null) {
      viewModel.downloadLogs()
      Toast.makeText(context, "Günlükler CSV olarak kaydedildi", Toast.LENGTH_SHORT).show()
    }
  }

  val activeProfile by viewModel.activeProfile.collectAsState()
  val configSettings by viewModel.configSettings.collectAsState()

  var profileNameInput by remember { mutableStateOf(activeProfile?.name ?: "") }
  var showRetentionMenu by remember { mutableStateOf(false) }
  var showStorageLocationMenu by remember { mutableStateOf(false) }
  var showDeleteDialog by remember { mutableStateOf(false) }
  var showRewriteDialog by remember { mutableStateOf(false) }
  var rewriteDomainInput by remember { mutableStateOf("") }
  var rewriteAnswerInput by remember { mutableStateOf("") }

  val retentionOptions = listOf("6 saat", "1 gün", "1 hafta", "1 ay", "3 ay", "6 ay", "1 yıl", "2 yıl")
  val locationOptions = listOf("İsviçre (CH)", "Avrupa Birliği (AB)", "Amerika Birleşik Devletleri (ABD)")

  LaunchedEffect(activeProfile) {
    activeProfile?.let {
      profileNameInput = it.name
    }
  }

  val profileName = activeProfile?.name ?: "Hasiggome"

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Görünüm (Appearance) Kartı
    item {
        NextDnsCard(title = "Görünüm") {
            val context = LocalContext.current
            val themePrefs = remember { ThemePreferences(context) }
            val themeMode by themePrefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val scope = rememberCoroutineScope()
            
            SingleChoiceSegmentedButtonRow(
                modifier = Modifier.fillMaxWidth().padding(8.dp)
            ) {
                ThemeMode.values().forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = (themeMode == mode),
                        onClick = { scope.launch { themePrefs.setThemeMode(mode) } },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ThemeMode.values().size
                        ),
                        label = {
                            Text(
                                text = when(mode) {
                                    ThemeMode.LIGHT -> "Açık"
                                    ThemeMode.DARK -> "Koyu"
                                    ThemeMode.SYSTEM -> "Otomatik"
                                    ThemeMode.SCHEDULED -> "Programlı"
                                    ThemeMode.BATTERY_SAVER -> "Pil Tasarrufu"
                                }
                            )
                        }
                    )
                }
            }
        }
    }
    // 1. Profil İsmi Kartı
    item {
      NextDnsCard(
        title = "Profil İsmi",
        subtitle = "Bu profile ayırt edici bir isim verin. Ev, Ofis veya Mobil gibi farklı cihaz gruplarını kolayca yönetebilirsiniz."
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          OutlinedTextField(
            value = profileNameInput,
            onValueChange = { profileNameInput = it },
            placeholder = { Text("Profil ismi", color = MaterialTheme.colorScheme.outline) },
            singleLine = true,
            modifier = Modifier
              .weight(1f)
              ,
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

          NextDnsButton(
            text = "KAYDET",
            onClick = {
              if (profileNameInput.isNotBlank()) {
                viewModel.renameProfile(profileNameInput.trim())
                Toast.makeText(context, "Profil ismi güncellendi: ${profileNameInput.trim()}", Toast.LENGTH_SHORT).show()
              }
            },
            modifier = Modifier
          )
        }
      }
    }

    // 2. Günlükler (Logs) Ayarları Kartı
    item {
      NextDnsCard(
        title = "Günlükler & Gizlilik Ayarları",
        subtitle = "Cihazlarınızdan gelen DNS sorgularını analiz etmek için kaydedin. İstemci IP'lerini ve alan adlarını ayrı ayrı gizleyebilir veya saklama süresini özelleştirebilirsiniz."
      ) {
        // Günlükleri etkinleştir switch
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .bounceClick { viewModel.toggleLogsEnabled(!configSettings.logsEnabled) }
            .padding(vertical = 4.dp),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween
        ) {
          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "Günlükleri etkinleştir",
              color = MaterialTheme.colorScheme.onSurface,
              fontSize = 13.sp,
              fontWeight = FontWeight.Bold
            )
            Text(
              text = "DNS sorgularının analiz ve inceleme için kaydedilmesini sağlar.",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp
            )
          }
          NextDnsSwitch(
            checked = configSettings.logsEnabled,
            onCheckedChange = { viewModel.toggleLogsEnabled(it) }
          )
        }

        if (configSettings.logsEnabled) {
          Spacer(modifier = Modifier.height(10.dp))
          HorizontalDivider(color = MaterialTheme.colorScheme.outline)
          Spacer(modifier = Modifier.height(10.dp))

          // IP & Domain kayıt onay kutuları
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .bounceClick { viewModel.toggleLogClientIps(!configSettings.logClientIps) }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Checkbox(
              checked = configSettings.logClientIps,
              onCheckedChange = { viewModel.toggleLogClientIps(it) },
              colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
            Column {
              Text("İstemci IP adreslerini kaydet", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
              Text("Kapatılırsa günlüklerde cihaz IP adresleri anonimleştirilir.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
            }
          }

          Row(
            modifier = Modifier
              .fillMaxWidth()
              .bounceClick { viewModel.toggleLogDomains(!configSettings.logDomains) }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Checkbox(
              checked = configSettings.logDomains,
              onCheckedChange = { viewModel.toggleLogDomains(it) },
              colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
              )
            )
            Column {
              Text("Ziyaret edilen alan adlarını kaydet", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
              Text("Kapatılırsa sadece engellenen/izin verilen sorgu sayıları tutulur.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.5.sp)
            }
          }

          Spacer(modifier = Modifier.height(10.dp))

          // Saklama Süresi ve Depolama Konumu Seçimi
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            // Saklama Süresi Dropdown
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Saklama süresi", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
              Box {
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .bounceClick { showRetentionMenu = true },
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(configSettings.logRetention, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                  }
                }
                DropdownMenu(
                  expanded = showRetentionMenu,
                  onDismissRequest = { showRetentionMenu = false },
                  modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                  retentionOptions.forEach { opt ->
                    DropdownMenuItem(
                      text = { Text(opt, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
                      onClick = {
                        viewModel.setLogRetention(opt)
                        showRetentionMenu = false
                      }
                    )
                  }
                }
              }
            }

            // Depolama Konumu Dropdown
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
              Text("Depolama konumu", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
              Box {
                Surface(
                  modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(10.dp))
                    .bounceClick { showStorageLocationMenu = true },
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  shape = RoundedCornerShape(10.dp)
                ) {
                  Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(configSettings.logStorageLocation, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp, maxLines = 1, fontWeight = FontWeight.Medium)
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                  }
                }
                DropdownMenu(
                  expanded = showStorageLocationMenu,
                  onDismissRequest = { showStorageLocationMenu = false },
                  modifier = Modifier
                    .background(MaterialTheme.colorScheme.surface)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                ) {
                  locationOptions.forEach { loc ->
                    DropdownMenuItem(
                      text = { Text(loc, color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp) },
                      onClick = {
                        viewModel.setLogStorageLocation(loc)
                        showStorageLocationMenu = false
                      }
                    )
                  }
                }
              }
            }
          }

          Spacer(modifier = Modifier.height(14.dp))

          // Aksiyon Butonları
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
          ) {
            NextDnsOutlineButton(
              text = "Günlükleri indir",
              onClick = {
                exportLauncher.launch("nextdns_logs.csv")
              },
              borderColor = MaterialTheme.colorScheme.primary,
              contentColor = MaterialTheme.colorScheme.primary,
              icon = Icons.Default.Download
            )

            NextDnsOutlineButton(
              text = "Günlükleri temizle",
              onClick = {
                viewModel.clearLogs()
                Toast.makeText(context, "Kayıtlı tüm günlükler temizlendi", Toast.LENGTH_SHORT).show()
              },
              borderColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.error,
              icon = Icons.Default.Delete
            )
          }
        }
      }
    }

    // 3. Engel Sayfası Kartı
    item {
      NextDnsCard(
        title = "Engel Sayfası"
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth().bounceClick { viewModel.toggleBlockPage(!configSettings.blockPage) },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            NextDnsSwitch(checked = configSettings.blockPage, onCheckedChange = { viewModel.toggleBlockPage(it) })
            Text("Engel sayfasını etkinleştir", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
          }

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
              Text("🔒 NextDNS Kök Sertifika Otoritesi", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
              val annotatedText = androidx.compose.ui.text.buildAnnotatedString {
                pushStringAnnotation("ca", "https://nextdns.io/ca")
                withStyle(androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                  append("https://nextdns.io/ca")
                }
                pop()
                append(" adresindeki kök Sertifika Otoritemizi yükleyip güvenerek, engelleme sayfasını yüklerken HTTPS uyarısını kaldırın. Bunun nasıl yapılacağına ilişkin talimatları ")
                pushStringAnnotation("help", "https://help.nextdns.io/t/x2hmvas/how-to-install-and-trust-nextdns-root-ca")
                withStyle(androidx.compose.ui.text.SpanStyle(color = MaterialTheme.colorScheme.primary)) {
                  append("buradan")
                }
                pop()
                append(" okuyun.")
              }
              val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current
              androidx.compose.foundation.text.ClickableText(
                text = annotatedText,
                style = androidx.compose.ui.text.TextStyle(color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp, lineHeight = 16.sp),
                onClick = { offset ->
                  annotatedText.getStringAnnotations("ca", offset, offset).firstOrNull()?.let { uriHandler.openUri(it.item) }
                  annotatedText.getStringAnnotations("help", offset, offset).firstOrNull()?.let { uriHandler.openUri(it.item) }
                }
              )
            }
          }
        }
      }
    }

    // 4. Performans Kartı
    item {
      NextDnsCard(
        title = "Performans",
        subtitle = "Göz atmanızı hızlandırın."
      ) {
        Column {
          // EDNS
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("Anonimize EDNS İstemci Alt Ağı", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text("IP adresinizi ifşa etmeden içerik dağıtım ağlarından veri dağıtımını hızlandırın.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              NextDnsSwitch(checked = configSettings.ednsClientSubnet, onCheckedChange = { viewModel.toggleEdns(it) })
              Text("Anonimize EDNS İstemci Alt Ağı'nı etkinleştir", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outline)
          
          // Cache Boost
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("Önbellek Arttırma", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text("Minimum TTL (Time to live) uygulayarak DNS sorgularını en aza indirin.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              NextDnsSwitch(checked = configSettings.cacheBoost, onCheckedChange = { viewModel.toggleCacheBoost(it) })
              Text("Önbellek arttırmayı etkinleştir", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            }
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outline)
          
          // CNAME
          Column(
            modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Text("CNAME Düzleştirme", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            Text("CNAME izleyen çözümleyicilerin gereksiz sorgular yapmasını önleyin ve günlükleri ara alan adlarıyla doldurun.", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
              NextDnsSwitch(checked = configSettings.cnameFlattening, onCheckedChange = { viewModel.toggleCnameFlattening(it) })
              Text("CNAME düzleştirmeyi etkinleştir", color = MaterialTheme.colorScheme.onSurface, fontSize = 12.sp)
            }
          }
        }
      }
    }
    
    // 5. Yeniden Yazmalar
    item {
      NextDnsCard(
        title = "Yeniden Yazmalar",
        subtitle = "Herhangi bir alan adı için DNS yanıtını ayarlayın veya geçersiz kılın. Bu yeniden yazmalar alt alan adları için de geçerlidir ve yerel IP adresleri yanıt olarak desteklenir."
      ) {
        if (configSettings.rewrites.isNotEmpty()) {
          Column(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            configSettings.rewrites.forEach { rw ->
              Surface(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = RoundedCornerShape(10.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.fillMaxWidth()
              ) {
                Row(
                  modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 10.dp),
                  horizontalArrangement = Arrangement.SpaceBetween,
                  verticalAlignment = Alignment.CenterVertically
                ) {
                  Column(modifier = Modifier.weight(1f)) {
                    Text(rw.domain, color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    Text("➔ ${rw.answer}", color = MaterialTheme.colorScheme.outline, fontSize = 11.5.sp)
                  }
                  IconButton(onClick = { viewModel.removeRewrite(rw.domain) }, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Kaldır", tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(16.dp))
                  }
                }
              }
            }
          }
        }
        
        NextDnsButton(
          text = "YENİ YENİDEN YAZMA",
          onClick = { showRewriteDialog = true }
        )
      }
    }
    // 6. Web3
    item {
      NextDnsCard(
        title = "Web3",
        isBeta = true,
        subtitle = "Web3, yenilikçi teknolojilerden oluşan, merkezi olmayan ve sansüre dirençli bir çevrimiçi ekosistemi ifade eder. Bu teknolojiler arasında blok zinciri tabanlı alan adı yazmanları (ör. Ethereum Name Service), dağıtık içerik depolama ve dağıtım ağları (örn. IPFS) sayılabilir. Bu ayarı açtığınızda, NextDNS bu yeni web'e filtresiz bir ağ geçidi görevi görecek ve hiçbir şey yüklemeye gerek kalmadan onu deneyimlemenize olanak tanıyacaktır.\n\nTarayıcıların çoğu şu anda yalnızca klasik üst düzey alan adlarını desteklediğinden, Web3 alan adlarına doğrudan erişmek için sonlarına bölü işareti (\"/\") eklemelisiniz. (örn. \"vitalik.eth\" yerine \"vitalik.eth/\")"
      ) {
        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
          // Sağlayıcı Logoları
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Surface(color = MaterialTheme.colorScheme.onSurface, shape = CircleShape, modifier = Modifier.size(40.dp)) {
              Box(contentAlignment = Alignment.Center) { Text("ENS", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            }
            Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(40.dp)) {
              Box(contentAlignment = Alignment.Center) { Text("UD", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            }
            Surface(color = MaterialTheme.colorScheme.onSurface, shape = CircleShape, modifier = Modifier.size(40.dp)) {
              Box(contentAlignment = Alignment.Center) { Text("HNS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            }
            Surface(color = MaterialTheme.colorScheme.tertiary, shape = CircleShape, modifier = Modifier.size(40.dp)) {
              Box(contentAlignment = Alignment.Center) { Text("IPFS", color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp) }
            }
          }
          
          HorizontalDivider(color = MaterialTheme.colorScheme.outline)
          
          // Etkinleştirme Anahtarı
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .bounceClick { viewModel.toggleWeb3(!configSettings.web3) }
              .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Web3'ü Etkinleştir", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 13.5.sp)
            NextDnsSwitch(checked = configSettings.web3, onCheckedChange = { viewModel.toggleWeb3(it) })
          }
        }
      }
    }

    // 7. Erişim
    item {
      NextDnsCard(
        title = "Erişim",
        isBeta = true,
        subtitle = "Başkalarına bu profili düzenleme veya sadece görüntüleme yetkisi verin."
      ) {
        NextDnsButton(text = "INVITE", onClick = {})
      }
    }

    // 8. Profili Kopyala
    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {},
              colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
              shape = RoundedCornerShape(8.dp),
              contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
              modifier = Modifier.height(32.dp)
            ) {
              Text(
                text = androidx.compose.ui.text.buildAnnotatedString {
                  withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                    append(activeProfile?.name ?: "Profil")
                  }
                  append(" profilini kopyala")
                },
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onPrimary
              )
            }
          }
          Text(
            "Bu profilin tüm ayarlarını yeni bir profile kopyalayın.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
          )
        }
      }
    }

    // 9. Sil
    item {
      Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.fillMaxWidth().padding(16.dp),
          verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          Button(
            onClick = {},
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
            shape = RoundedCornerShape(8.dp),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
            modifier = Modifier.height(32.dp).width(IntrinsicSize.Max)
          ) {
            Text(
              text = androidx.compose.ui.text.buildAnnotatedString {
                append("Sil: ")
                withStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.Bold)) {
                  append(activeProfile?.name ?: "Profil")
                }
              },
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onError
            )
          }
          Text(
            "Bu işlem, bu profili ve onunla ilişkili tüm günlükleri kalıcı olarak silecektir.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp
          )
        }
      }
    }
  }

  // Yeniden Yazma Ekle Dialog
  if (showRewriteDialog) {
    var domain by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf("") }
    AlertDialog(
      onDismissRequest = { showRewriteDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(18.dp),
      title = { Text("Yeni Yeniden Yazma", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 16.sp) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = domain,
            onValueChange = { domain = it },
            placeholder = { Text("Alan adı (örn. google.com)", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp)
          )
          OutlinedTextField(
            value = answer,
            onValueChange = { answer = it },
            placeholder = { Text("Hedef (örn. 192.168.1.1)", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedBorderColor = MaterialTheme.colorScheme.primary, unfocusedBorderColor = MaterialTheme.colorScheme.outline,
              focusedTextColor = MaterialTheme.colorScheme.onSurface, unfocusedTextColor = MaterialTheme.colorScheme.onSurface
            ),
            shape = RoundedCornerShape(12.dp)
          )
        }
      },
      confirmButton = {
        TextButton(onClick = {
          if (domain.isNotBlank() && answer.isNotBlank()) {
            viewModel.addRewrite(domain.trim(), answer.trim())
            showRewriteDialog = false
          }
        }) {
          Text("Kaydet", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { showRewriteDialog = false }) {
          Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
      }
    )
  }
}
