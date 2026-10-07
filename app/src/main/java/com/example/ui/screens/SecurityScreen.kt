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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.NextDnsViewModel

@Composable
fun SecurityScreen(
  viewModel: NextDnsViewModel,
  modifier: Modifier = Modifier
) {
  val settings by viewModel.securitySettings.collectAsState()
  var showAddTldDialog by remember { mutableStateOf(false) }
  var newTldInput by remember { mutableStateOf("") }

  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(top = 12.dp, bottom = 40.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 1. Tehdit İstihbaratı Beslemeleri
    item {
      NextDnsSettingToggle(
        title = "Tehdit İstihbaratı Beslemeleri",
        subtitle = "Tümü gerçek zamanlı olarak güncellenen, en saygın tehdit istihbarat beslemelerinin bir karışımını kullanarak kötü amaçlı yazılım dağıttığı, kimlik avı saldırıları başlattığı ve komuta ve kontrol sunucuları barındırdığı bilinen alan adlarını engelleyin.",
        checked = settings.threatIntelligenceFeeds,
        onCheckedChange = { viewModel.toggleSecurityFeature("threatIntelligenceFeeds", it) }
      )
    }

    // 2. Yapay Zekâ Destekli Tehdit Algılama [BETA]
    item {
      NextDnsSettingToggle(
        title = "Yapay Zekâ Destekli Tehdit Algılama",
        subtitle = "Yapay zekâ teknolojimizle tespit edilen milyonlarca saldırıyı engelleyin. Tescilli yapay zekâ motorumuz yüzlerce sinyal, terabaytlarca eğitim verisi ve gerçek zamanlı karar verme kabiliyetleriyle baştan sona DNS için tasarlandı.",
        checked = settings.aiThreatDetection,
        onCheckedChange = { viewModel.toggleSecurityFeature("aiThreatDetection", it) },
        isBeta = true
      )
    }

    // 3. Google Güvenli Tarama
    item {
      NextDnsSettingToggle(
        title = "Google Güvenli Tarama",
        subtitle = "Güvenli olmayan web sitelerini arayarak her gün milyarlarca URL'yi inceleyen bir teknoloji olan Google Güvenli Tarama'yı kullanarak kötü amaçlı yazılımları ve kimlik avı alan adlarını engelleyin. Bazı tarayıcılarda yerleşik olan sürümden farklı olarak, bu, genel IP adresinizi tehditlerle ilişkilendirmez ve engellemeyi aşmaya izin vermez.",
        checked = settings.googleSafeBrowsing,
        onCheckedChange = { viewModel.toggleSecurityFeature("googleSafeBrowsing", it) }
      )
    }

    // 4. Kripto Korsanlık (Cryptojacking) Koruması
    item {
      NextDnsSettingToggle(
        title = "Kripto Korsanlık (Cryptojacking) Koruması",
        subtitle = "Cihazlarınızın kripto para madenciliği yapmak için yetkisiz kullanımını önleyin.",
        checked = settings.cryptojacking,
        onCheckedChange = { viewModel.toggleSecurityFeature("cryptojacking", it) }
      )
    }

    // 5. DNS Rebinding Koruması
    item {
      NextDnsSettingToggle(
        title = "DNS Rebinding Koruması",
        subtitle = "Özel IP adresleri içeren DNS yanıtlarını otomatik olarak engelleyerek saldırganların İnternet üzerinden yerel cihazlarınızın kontrolünü ele geçirmesini önleyin.",
        checked = settings.dnsRebinding,
        onCheckedChange = { viewModel.toggleSecurityFeature("dnsRebinding", it) }
      )
    }

    // 6. IDN Eşyazımı Saldırı Koruması
    item {
      NextDnsSettingToggle(
        title = "IDN Eşyazımı Saldırı Koruması",
        subtitle = "Uluslararası Alan Adlarının (IDN'ler) gelişiyle sağlanan büyük karakter kümesini kötüye kullanarak diğer etki alanlarını taklit eden etki alanlarını engelleyin - örneğin, Latince \"e\" harfini Kiril harfi \"е\" ile değiştirmek.",
        checked = settings.idnHomographs,
        onCheckedChange = { viewModel.toggleSecurityFeature("idnHomographs", it) }
      )
    }

    // 7. Yanlış Siteye Yönlendirme Koruması
    item {
      NextDnsSettingToggle(
        title = "Yanlış Siteye Yönlendirme Koruması",
        subtitle = "Tarayıcılarına yanlış bir şekilde web sitesi adresi yazan kullanıcıları hedefleyen kötü niyetli kişiler tarafından kaydedilen alan adlarını engelleyin - ör. Google.com yerine gooogle.com.",
        checked = settings.typosquatting,
        onCheckedChange = { viewModel.toggleSecurityFeature("typosquatting", it) }
      )
    }

    // 8. Alan Adı Oluşturma Algoritmaları (DGA) Koruması
    item {
      NextDnsSettingToggle(
        title = "Alan Adı Oluşturma Algoritmaları (DGA) Koruması",
        subtitle = "Çeşitli kötü amaçlı yazılım gruplarında görülen ve komuta ve kontrol sunucularıyla buluşma noktaları olarak kullanılabilen Alan Adı Oluşturma Algoritmaları (DGA) tarafından oluşturulan etki alanlarını engelleyin.",
        checked = settings.dga,
        onCheckedChange = { viewModel.toggleSecurityFeature("dga", it) }
      )
    }

    // 9. Yeni Kaydedilmiş Alan Adlarını (NRD'ler) Engelle
    item {
      NextDnsSettingToggle(
        title = "Yeni Kaydedilmiş Alan Adlarını (NRD'ler) Engelle",
        subtitle = "30 günden daha kısa süre önce kaydedilen alanları engelleyin. Bu alanların tehdit aktörleri tarafından kötü amaçlı kampanyalar başlatmak için tercih edildiği bilinmektedir.",
        checked = settings.nrd,
        onCheckedChange = { viewModel.toggleSecurityFeature("nrd", it) }
      )
    }

    // 10. Dinamik DNS Ana Bilgisayar Adlarını Engelle [BETA]
    item {
      NextDnsSettingToggle(
        title = "Dinamik DNS Ana Bilgisayar Adlarını Engelle",
        subtitle = "Dinamik DNS (kısaca DDNS) servisleri, kötü niyetli kişilerin herhangi bir doğrulama ve kimlik kontrolü olmaksızın hızlı ve ücretsiz bir şekilde ana bilgisayar adları oluşturmalarına olanak tanır. Normal amaçlarla kullanılan DDNS ana bilgisayar adları günlük kullanımda pek karşınıza çıkmaz ama kötü niyetli olanları kimlik avı kampanyalarında yoğun olarak kullanılır. (örn. paypal-login.duckdns.org)\nDDNS kullanıyorsanız unutmayın ki bu ayar, DDNS hizmetinin web sitesini ve güncelleme API'sini engellemeyecektir.",
        checked = settings.ddns,
        onCheckedChange = { viewModel.toggleSecurityFeature("ddns", it) },
        isBeta = true
      )
    }

    // 11. Park Edilmiş Alan Adlarını Engelle
    item {
      NextDnsSettingToggle(
        title = "Park Edilmiş Alan Adlarını Engelle",
        subtitle = "Park edilmiş alanlar, genellikle reklamlarla yüklü ve hiçbir değeri olmayan tek sayfalık web siteleridir. Park edilmiş alandan para kazanma, bazen şüpheli uygulamalar ve kötü amaçlı içerikle karışabilir.",
        checked = settings.parkedDomains,
        onCheckedChange = { viewModel.toggleSecurityFeature("parkedDomains", it) }
      )
    }

    // 12. Üst Seviye Alan Adlarını (TLD'ler) Engelle
    item {
      NextDnsCard(
        title = "Üst Seviye Alan Adlarını (TLD'ler) Engelle",
        subtitle = "Belirli TLD'lere ait tüm alanları ve alt alanları engelleyin."
      ) {
        if (settings.blockedTlds.isNotEmpty()) {
          Column(
            modifier = Modifier
              .fillMaxWidth()
              .padding(bottom = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            settings.blockedTlds.forEach { tld ->
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
                  Text(
                    text = ".$tld",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                  )
                  IconButton(
                    onClick = { viewModel.removeBlockedTld(tld) },
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
          text = "TLD EKLE",
          onClick = { showAddTldDialog = true },
          icon = Icons.Default.Add
        )
      }
    }

    // 13. Çocukların Cinsel İstismarına İlişkin Materyalleri Engelle
    item {
      NextDnsSettingToggle(
        title = "Çocukların Cinsel İstismarına İlişkin Materyalleri Engelle",
        subtitle = "Canadian Centre for Child Protection tarafından işletilen Project Arachnid'in yardımıyla çocuklara yönelik cinsel istismar materyalleri barındıran alanları engelleyin. Bir alan adı engellendiğinde Project Arachnid'e hiçbir bilgi iletilmez.",
        checked = settings.csam,
        onCheckedChange = { viewModel.toggleSecurityFeature("csam", it) }
      )
    }
  }

  // TLD Ekle Dialog
  if (showAddTldDialog) {
    val liveTldCatalog by viewModel.availableTldsCatalog.collectAsState()
    var tldSearchQuery by remember { mutableStateOf("") }

    val rawTldList = if (liveTldCatalog.isNotEmpty()) {
      liveTldCatalog.map { it.id }
    } else {
      listOf("work", "fit", "surf", "review", "asia", "tokyo", "cn", "monster", "info", "机构", "xyz", "top", "ru", "zip", "mov", "app", "dev")
    }

    val availableTlds = rawTldList
      .map { if (it.startsWith(".")) it else ".$it" }
      .filter { tld ->
        !settings.blockedTlds.any { added -> added.equals(tld.removePrefix("."), ignoreCase = true) }
      }
      .filter { tld ->
        tldSearchQuery.isBlank() || tld.contains(tldSearchQuery.trim().removePrefix("."), ignoreCase = true)
      }

    AlertDialog(
      onDismissRequest = { showAddTldDialog = false },
      containerColor = MaterialTheme.colorScheme.surface,
      shape = RoundedCornerShape(16.dp),
      modifier = Modifier.fillMaxWidth().height(600.dp),
      title = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Text("TLD Ekle", color = MaterialTheme.colorScheme.onSurface, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            IconButton(onClick = { showAddTldDialog = false }, modifier = Modifier.size(24.dp)) {
              Icon(Icons.Default.Close, contentDescription = "Kapat", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
          }
          OutlinedTextField(
            value = tldSearchQuery,
            onValueChange = { tldSearchQuery = it },
            placeholder = { Text("TLD ara... (.xyz, .top, .ru)", fontSize = 12.sp) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(8.dp)
          )
        }
      },
      text = {
        LazyColumn(
          modifier = Modifier.fillMaxSize(),
          verticalArrangement = Arrangement.spacedBy(0.dp)
        ) {
          items(availableTlds) { tld ->
            Row(
              modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 16.dp, vertical = 10.dp),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(tld, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
              Button(
                onClick = { viewModel.addBlockedTld(tld.removePrefix(".")) },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(6.dp),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                modifier = Modifier.height(28.dp)
              ) {
                Text("EKLE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
              }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline, modifier = Modifier.padding(horizontal = 16.dp))
          }
        }
      },
      confirmButton = {},
      dismissButton = {}
    )
  }
}
