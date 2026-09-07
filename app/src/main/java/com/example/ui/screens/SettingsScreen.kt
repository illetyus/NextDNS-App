package com.example.ui.screens

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ConfigSettings
import com.example.data.model.RewriteItem
import com.example.data.preferences.ThemePreferences
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.NextDnsViewModel
import kotlinx.coroutines.launch

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

  var showRewriteDialog by remember { mutableStateOf(false) }
  val profileName = activeProfile?.name ?: "Varsayılan Profil"

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // 1. Görünüm (Appearance)
    item {
      AppearanceSection()
    }

    // 2. Profil İsmi
    item {
      ProfileNameSection(
        initialName = profileName,
        onSaveName = { newName ->
          viewModel.renameProfile(newName)
          Toast.makeText(context, "Profil ismi güncellendi: $newName", Toast.LENGTH_SHORT).show()
        }
      )
    }

    // 3. Günlükler (Logs & Gizlilik)
    item {
      LogsAndPrivacySection(
        configSettings = configSettings,
        viewModel = viewModel,
        onExportClick = { exportLauncher.launch("nextdns_logs.csv") },
        onClearLogsClick = {
          viewModel.clearLogs()
          Toast.makeText(context, "Kayıtlı tüm günlükler temizlendi", Toast.LENGTH_SHORT).show()
        }
      )
    }

    // 4. Engel Sayfası
    item {
      BlockPageSection(
        blockPageEnabled = configSettings.blockPage,
        onToggleBlockPage = { viewModel.toggleBlockPage(it) }
      )
    }

    // 5. Performans
    item {
      PerformanceSection(
        configSettings = configSettings,
        viewModel = viewModel
      )
    }

    // 6. Yeniden Yazmalar (Rewrites)
    item {
      RewritesSection(
        rewrites = configSettings.rewrites,
        onAddRewriteClick = { showRewriteDialog = true },
        onRemoveRewrite = { domain -> viewModel.removeRewrite(domain) }
      )
    }

    // 7. Web3
    item {
      Web3Section(
        web3Enabled = configSettings.web3,
        onToggleWeb3 = { viewModel.toggleWeb3(it) }
      )
    }

    // 8. Erişim
    item {
      AccessSection()
    }

    // 9. Profil Eylemleri (Kopyala / Sil)
    item {
      ProfileActionsSection(
        profileName = profileName,
        onCopyProfile = {},
        onDeleteProfile = {}
      )
    }
  }

  // Yeniden Yazma Ekle Dialog
  if (showRewriteDialog) {
    RewriteDialog(
      onDismiss = { showRewriteDialog = false },
      onConfirm = { domain, answer ->
        viewModel.addRewrite(domain, answer)
        showRewriteDialog = false
      }
    )
  }
}

/**
 * 1. Görünüm Ayarları Bölümü
 */
@Composable
fun AppearanceSection(
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val themePrefs = remember { ThemePreferences(context) }
  val themeMode by themePrefs.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
  val scope = rememberCoroutineScope()

  NextDnsCard(title = "Görünüm", modifier = modifier) {
    SingleChoiceSegmentedButtonRow(
      modifier = Modifier
        .fillMaxWidth()
        .padding(top = 4.dp)
    ) {
      ThemeMode.entries.forEachIndexed { index, mode ->
        SegmentedButton(
          selected = (themeMode == mode),
          onClick = { scope.launch { themePrefs.setThemeMode(mode) } },
          shape = SegmentedButtonDefaults.itemShape(
            index = index,
            count = ThemeMode.entries.size
          ),
          label = {
            Text(
              text = when (mode) {
                ThemeMode.LIGHT -> "Açık"
                ThemeMode.DARK -> "Koyu"
                ThemeMode.SYSTEM -> "Otomatik"
              },
              maxLines = 1
            )
          }
        )
      }
    }
  }
}

/**
 * 2. Profil İsmi Bölümü
 */
@Composable
fun ProfileNameSection(
  initialName: String,
  onSaveName: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var profileNameInput by remember(initialName) { mutableStateOf(initialName) }

  NextDnsCard(
    title = "Profil İsmi",
    subtitle = "Bu profile ayırt edici bir isim verin. Ev, Ofis veya Mobil gibi farklı cihaz gruplarını kolayca yönetebilirsiniz.",
    modifier = modifier
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
        modifier = Modifier.weight(1f),
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
            onSaveName(profileNameInput.trim())
          }
        }
      )
    }
  }
}

/**
 * 3. Günlükler ve Gizlilik Ayarları Bölümü State Modeli
 */
@Immutable
data class LogsAndPrivacySectionState(
  val configSettings: ConfigSettings,
  val onToggleLogsEnabled: (Boolean) -> Unit,
  val onToggleLogClientIps: (Boolean) -> Unit,
  val onToggleLogDomains: (Boolean) -> Unit,
  val onSetLogRetention: (String) -> Unit,
  val onSetLogStorageLocation: (String) -> Unit,
  val onExportClick: () -> Unit,
  val onClearLogsClick: () -> Unit
)

/**
 * 3. Günlükler ve Gizlilik Ayarları Bölümü
 */
@Composable
fun LogsAndPrivacySection(
  state: LogsAndPrivacySectionState,
  modifier: Modifier = Modifier
) {
  val retentionOptions = listOf("6 saat", "1 gün", "1 hafta", "1 ay", "3 ay", "6 ay", "1 yıl", "2 yıl")
  val locationOptions = listOf("İsviçre (CH)", "Avrupa Birliği (AB)", "Amerika Birleşik Devletleri (ABD)")
  val configSettings = state.configSettings

  NextDnsCard(
    title = "Günlükler & Gizlilik Ayarları",
    subtitle = "Cihazlarınızdan gelen DNS sorgularını analiz etmek için kaydedin. İstemci IP'lerini ve alan adlarını ayrı ayrı gizleyebilir veya saklama süresini özelleştirebilirsiniz.",
    modifier = modifier
  ) {
    // Günlükleri etkinleştir toggle
    NextDnsSettingToggleRow(
      title = "Günlükleri etkinleştir",
      subtitle = "DNS sorgularının analiz ve inceleme için kaydedilmesini sağlar.",
      checked = configSettings.logsEnabled,
      onCheckedChange = state.onToggleLogsEnabled
    )

    if (configSettings.logsEnabled) {
      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = MaterialTheme.colorScheme.outline)
      Spacer(modifier = Modifier.height(10.dp))

      // IP & Domain kayıt onay kutuları
      NextDnsCheckboxRow(
        title = "İstemci IP adreslerini kaydet",
        subtitle = "Kapatılırsa günlüklerde cihaz IP adresleri anonimleştirilir.",
        checked = configSettings.logClientIps,
        onCheckedChange = state.onToggleLogClientIps
      )

      NextDnsCheckboxRow(
        title = "Ziyaret edilen alan adlarını kaydet",
        subtitle = "Kapatılırsa sadece engellenen/izin verilen sorgu sayıları tutulur.",
        checked = configSettings.logDomains,
        onCheckedChange = state.onToggleLogDomains
      )

      Spacer(modifier = Modifier.height(10.dp))

      // Saklama Süresi ve Depolama Konumu Dropdown'ları
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        NextDnsDropdownSelector(
          label = "Saklama süresi",
          selectedValue = configSettings.logRetention,
          options = retentionOptions,
          onOptionSelected = state.onSetLogRetention,
          modifier = Modifier.weight(1f)
        )

        NextDnsDropdownSelector(
          label = "Depolama konumu",
          selectedValue = configSettings.logStorageLocation,
          options = locationOptions,
          onOptionSelected = state.onSetLogStorageLocation,
          modifier = Modifier.weight(1f)
        )
      }

      Spacer(modifier = Modifier.height(14.dp))

      // Aksiyon Butonları
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        NextDnsOutlineButton(
          text = "Günlükleri indir",
          onClick = state.onExportClick,
          colors = NextDnsOutlineButtonColors(
            borderColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.primary
          ),
          icon = Icons.Default.Download,
          modifier = Modifier.weight(1f)
        )

        NextDnsOutlineButton(
          text = "Günlükleri temizle",
          onClick = state.onClearLogsClick,
          colors = NextDnsOutlineButtonColors(
            borderColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.error
          ),
          icon = Icons.Default.Delete,
          modifier = Modifier.weight(1f)
        )
      }
    }
  }
}

/**
 * Geriye dönük uyumluluk için LogsAndPrivacySection aşırı yüklemesi
 */
@Composable
fun LogsAndPrivacySection(
  configSettings: ConfigSettings,
  viewModel: NextDnsViewModel,
  onExportClick: () -> Unit,
  onClearLogsClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  LogsAndPrivacySection(
    state = LogsAndPrivacySectionState(
      configSettings = configSettings,
      onToggleLogsEnabled = { viewModel.toggleLogsEnabled(it) },
      onToggleLogClientIps = { viewModel.toggleLogClientIps(it) },
      onToggleLogDomains = { viewModel.toggleLogDomains(it) },
      onSetLogRetention = { viewModel.setLogRetention(it) },
      onSetLogStorageLocation = { viewModel.setLogStorageLocation(it) },
      onExportClick = onExportClick,
      onClearLogsClick = onClearLogsClick
    ),
    modifier = modifier
  )
}

/**
 * 4. Engel Sayfası Bölümü
 */
@Composable
fun BlockPageSection(
  blockPageEnabled: Boolean,
  onToggleBlockPage: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  val uriHandler = LocalUriHandler.current

  NextDnsCard(
    title = "Engel Sayfası",
    modifier = modifier
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .bounceClick { onToggleBlockPage(!blockPageEnabled) },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        NextDnsSwitch(
          checked = blockPageEnabled,
          onCheckedChange = onToggleBlockPage
        )
        Text(
          text = "Engel sayfasını etkinleştir",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 13.sp
        )
      }

      Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(10.dp),
        modifier = Modifier.fillMaxWidth()
      ) {
        Column(
          modifier = Modifier.padding(14.dp),
          verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
          Text(
            text = "🔒 NextDNS Kök Sertifika Otoritesi",
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 13.5.sp
          )
          val annotatedText = buildAnnotatedString {
            pushStringAnnotation("ca", "https://nextdns.io/ca")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
              append("https://nextdns.io/ca")
            }
            pop()
            append(" adresindeki kök Sertifika Otoritemizi yükleyip güvenerek, engelleme sayfasını yüklerken HTTPS uyarısını kaldırın. Bunun nasıl yapılacağına ilişkin talimatları ")
            pushStringAnnotation("help", "https://help.nextdns.io/t/x2hmvas/how-to-install-and-trust-nextdns-root-ca")
            withStyle(SpanStyle(color = MaterialTheme.colorScheme.primary)) {
              append("buradan")
            }
            pop()
            append(" okuyun.")
          }
          androidx.compose.foundation.text.ClickableText(
            text = annotatedText,
            style = TextStyle(
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 12.sp,
              lineHeight = 16.sp
            ),
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

/**
 * 5. Performans Bölümü
 */
@Composable
fun PerformanceSection(
  configSettings: ConfigSettings,
  viewModel: NextDnsViewModel,
  modifier: Modifier = Modifier
) {
  NextDnsCard(
    title = "Performans",
    subtitle = "Göz atmanızı hızlandırın.",
    modifier = modifier
  ) {
    Column {
      EdnsSubnetBlock(
        enabled = configSettings.ednsClientSubnet,
        onToggle = { viewModel.toggleEdns(it) }
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outline)

      CacheBoostBlock(
        enabled = configSettings.cacheBoost,
        onToggle = { viewModel.toggleCacheBoost(it) }
      )

      HorizontalDivider(color = MaterialTheme.colorScheme.outline)

      CnameFlatteningBlock(
        enabled = configSettings.cnameFlattening,
        onToggle = { viewModel.toggleCnameFlattening(it) }
      )
    }
  }
}

@Composable
private fun EdnsSubnetBlock(
  enabled: Boolean,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = "Anonimize EDNS İstemci Alt Ağı",
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Bold,
      fontSize = 13.5.sp
    )
    Text(
      text = "IP adresinizi ifşa etmeden içerik dağıtım ağlarından veri dağıtımını hızlandırın.",
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 12.sp
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      NextDnsSwitch(
        checked = enabled,
        onCheckedChange = onToggle
      )
      Text(
        text = "Anonimize EDNS İstemci Alt Ağı'nı etkinleştir",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp
      )
    }
  }
}

@Composable
private fun CacheBoostBlock(
  enabled: Boolean,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = "Önbellek Arttırma",
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Bold,
      fontSize = 13.5.sp
    )
    Text(
      text = "Minimum TTL (Time to live) uygulayarak DNS sorgularını en aza indirin.",
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 12.sp
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      NextDnsSwitch(
        checked = enabled,
        onCheckedChange = onToggle
      )
      Text(
        text = "Önbellek arttırmayı etkinleştir",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp
      )
    }
  }
}

@Composable
private fun CnameFlatteningBlock(
  enabled: Boolean,
  onToggle: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = 12.dp),
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Text(
      text = "CNAME Düzleştirme",
      color = MaterialTheme.colorScheme.onSurface,
      fontWeight = FontWeight.Bold,
      fontSize = 13.5.sp
    )
    Text(
      text = "CNAME izleyen çözümleyicilerin gereksiz sorgular yapmasını önleyin ve günlükleri ara alan adlarıyla doldurun.",
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 12.sp
    )
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      NextDnsSwitch(
        checked = enabled,
        onCheckedChange = onToggle
      )
      Text(
        text = "CNAME düzleştirmeyi etkinleştir",
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 12.sp
      )
    }
  }
}

/**
 * 6. Yeniden Yazmalar (Rewrites) Bölümü
 */
@Composable
fun RewritesSection(
  rewrites: List<RewriteItem>,
  onAddRewriteClick: () -> Unit,
  onRemoveRewrite: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  NextDnsCard(
    title = "Yeniden Yazmalar",
    subtitle = "Herhangi bir alan adı için DNS yanıtını ayarlayın veya geçersiz kılın. Bu yeniden yazmalar alt alan adları için de geçerlidir ve yerel IP adresleri yanıt olarak desteklenir.",
    modifier = modifier
  ) {
    if (rewrites.isNotEmpty()) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        rewrites.forEach { rw ->
          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(10.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            modifier = Modifier.fillMaxWidth()
          ) {
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Column(modifier = Modifier.weight(1f)) {
                Text(
                  text = rw.domain,
                  color = MaterialTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp
                )
                Text(
                  text = "➔ ${rw.answer}",
                  color = MaterialTheme.colorScheme.outline,
                  fontSize = 11.5.sp
                )
              }
              IconButton(
                onClick = { onRemoveRewrite(rw.domain) },
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

    NextDnsButton(
      text = "YENİ YENİDEN YAZMA",
      onClick = onAddRewriteClick
    )
  }
}

/**
 * 7. Web3 Bölümü
 */
@Composable
fun Web3Section(
  web3Enabled: Boolean,
  onToggleWeb3: (Boolean) -> Unit,
  modifier: Modifier = Modifier
) {
  NextDnsCard(
    title = "Web3",
    isBeta = true,
    subtitle = "Web3, yenilikçi teknolojilerden oluşan, merkezi olmayan ve sansüre dirençli bir çevrimiçi ekosistemi ifade eder. Bu teknolojiler arasında blok zinciri tabanlı alan adı yazmanları (ör. Ethereum Name Service), dağıtık içerik depolama ve dağıtım ağları (örn. IPFS) sayılabilir. Bu ayarı açtığınızda, NextDNS bu yeni web'e filtresiz bir ağ geçidi görevi görecek ve hiçbir şey yüklemeye gerek kalmadan onu deneyimlemenize olanak tanıyacaktır.\n\nTarayıcıların çoğu şu anda yalnızca klasik üst düzey alan adlarını desteklediğinden, Web3 alan adlarına doğrudan erişmek için sonlarına bölü işareti (\"/\") eklemelisiniz. (örn. \"vitalik.eth\" yerine \"vitalik.eth/\")",
    modifier = modifier
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
      // Sağlayıcı Logoları
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Surface(color = MaterialTheme.colorScheme.onSurface, shape = CircleShape, modifier = Modifier.size(40.dp)) {
          Box(contentAlignment = Alignment.Center) {
            Text("ENS", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }
        }
        Surface(color = MaterialTheme.colorScheme.primary, shape = CircleShape, modifier = Modifier.size(40.dp)) {
          Box(contentAlignment = Alignment.Center) {
            Text("UD", color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }
        }
        Surface(color = MaterialTheme.colorScheme.onSurface, shape = CircleShape, modifier = Modifier.size(40.dp)) {
          Box(contentAlignment = Alignment.Center) {
            Text("HNS", color = MaterialTheme.colorScheme.onSurfaceVariant, fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }
        }
        Surface(color = MaterialTheme.colorScheme.tertiary, shape = CircleShape, modifier = Modifier.size(40.dp)) {
          Box(contentAlignment = Alignment.Center) {
            Text("IPFS", color = MaterialTheme.colorScheme.onTertiary, fontWeight = FontWeight.Bold, fontSize = 11.sp)
          }
        }
      }

      HorizontalDivider(color = MaterialTheme.colorScheme.outline)

      // Etkinleştirme Anahtarı
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .bounceClick { onToggleWeb3(!web3Enabled) }
          .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
      ) {
        Text(
          text = "Web3'ü Etkinleştir",
          color = MaterialTheme.colorScheme.onSurface,
          fontWeight = FontWeight.Bold,
          fontSize = 13.5.sp
        )
        NextDnsSwitch(
          checked = web3Enabled,
          onCheckedChange = onToggleWeb3
        )
      }
    }
  }
}

/**
 * 8. Erişim Bölümü
 */
@Composable
fun AccessSection(
  modifier: Modifier = Modifier
) {
  NextDnsCard(
    title = "Erişim",
    isBeta = true,
    subtitle = "Başkalarına bu profili düzenleme veya sadece görüntüleme yetkisi verin.",
    modifier = modifier
  ) {
    NextDnsButton(text = "INVITE", onClick = {})
  }
}

/**
 * 9. Profil Eylemleri Bölümü (Kopyala / Sil)
 */
@Composable
fun ProfileActionsSection(
  profileName: String,
  onCopyProfile: () -> Unit,
  onDeleteProfile: () -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    NextDnsActionCard(
      buttonText = "$profileName profilini kopyala",
      description = "Bu profilin tüm ayarlarını yeni bir profile kopyalayın.",
      onButtonClick = onCopyProfile
    )

    NextDnsActionCard(
      buttonText = "Sil: $profileName",
      description = "Bu işlem, bu profili ve onunla ilişkili tüm günlükleri kalıcı olarak silecektir.",
      onButtonClick = onDeleteProfile,
      isDanger = true
    )
  }
}

/**
 * Yeniden Yazma Ekle Dialog
 */
@Composable
fun RewriteDialog(
  onDismiss: () -> Unit,
  onConfirm: (String, String) -> Unit
) {
  var domain by remember { mutableStateOf("") }
  var answer by remember { mutableStateOf("") }

  AlertDialog(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(18.dp),
    title = {
      Text(
        text = "Yeni Yeniden Yazma",
        color = MaterialTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp
      )
    },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
          value = domain,
          onValueChange = { domain = it },
          placeholder = { Text("Alan adı (örn. google.com)", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp) },
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
        OutlinedTextField(
          value = answer,
          onValueChange = { answer = it },
          placeholder = { Text("Hedef (örn. 192.168.1.1)", color = MaterialTheme.colorScheme.outline, fontSize = 12.sp) },
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
      TextButton(onClick = {
        if (domain.isNotBlank() && answer.isNotBlank()) {
          onConfirm(domain.trim(), answer.trim())
        }
      }) {
        Text("Kaydet", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
      }
    },
    dismissButton = {
      TextButton(onClick = onDismiss) {
        Text("İptal", color = MaterialTheme.colorScheme.onSurfaceVariant)
      }
    }
  )
}
