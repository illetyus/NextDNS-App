package com.example.data.model

import java.util.UUID

enum class DnsStatus {
  ALLOWED,
  BLOCKED
}

data class NextDnsProfile(
  val id: String = java.util.UUID.randomUUID().toString(),
  val name: String,
  val fingerprint: String = ""
)

data class SecuritySettings(
  val threatIntelligenceFeeds: Boolean = true,
  val aiThreatDetection: Boolean = true,
  val googleSafeBrowsing: Boolean = true,
  val cryptojacking: Boolean = true,
  val dnsRebinding: Boolean = true,
  val idnHomographs: Boolean = true,
  val typosquatting: Boolean = true,
  val dga: Boolean = true,
  val nrd: Boolean = true,
  val ddns: Boolean = false,
  val parkedDomains: Boolean = true,
  val csam: Boolean = true,
  val blockedTlds: List<String> = listOf("top", "xyz", "click", "work", "loan", "surf")
)

data class BlocklistEntry(
  val id: String = java.util.UUID.randomUUID().toString(),
  val name: String,
  val description: String,
  val entriesCount: Long = 0,
  val active: Boolean = false,
  val website: String? = null,
  val category: String = "Genel",
  val updatedTime: String = "4 saat önce"
)

data class NativeTrackingEntry(
  val id: String = java.util.UUID.randomUUID().toString(),
  val name: String,
  val description: String,
  val active: Boolean = false
)

data class PrivacySettings(
  val blocklists: List<BlocklistEntry> = defaultBlocklistCatalog(),
  val nativeTracking: List<NativeTrackingEntry> = defaultNativeTracking(),
  val disguisedTrackers: Boolean = true,
  val allowAffiliates: Boolean = false
) {
  companion object {
    fun defaultBlocklistCatalog() = listOf(
      BlocklistEntry(
        id = "nextdns-recommended",
        name = "NextDNS Reklam & İzleyici Koruması",
        description = "NextDNS tarafından optimize edilmiş dengeli ve kapsamlı reklam/izleyici engelleme listesi.",
        entriesCount = 68450,
        active = true,
        website = "https://nextdns.io",
        category = "Genel"
      ),
      BlocklistEntry(
        id = "adguard-dns",
        name = "AdGuard DNS filter",
        description = "AdGuard Base, Mobil Reklamlar ve İzleme Koruması listelerinden DNS seviyesinde optimize edilmiş ana filtre.",
        entriesCount = 184200,
        active = true,
        website = "github.com/AdguardTeam/AdguardSDNSFilter",
        category = "Genel"
      ),
      BlocklistEntry(
        id = "oisd",
        name = "OISD (Big)",
        description = "İnternet genelindeki reklamları ve izleyicileri minimum yanlış pozitif ile filtreleyen popüler liste.",
        entriesCount = 289400,
        active = false,
        website = "https://oisd.nl",
        category = "Genel"
      ),
      BlocklistEntry(
        id = "oisd-small",
        name = "OISD (Small)",
        description = "Düşük bellekli cihazlar ve temel düzey koruma için OISD'nin kompakt sürümü.",
        entriesCount = 89000,
        active = false,
        website = "https://oisd.nl",
        category = "Genel"
      ),
      BlocklistEntry(
        id = "hagezi-multi-pro",
        name = "HaGeZi's Multi PRO",
        description = "Reklamları, izleyicileri ve telemetriyi sıkı şekilde engelleyen popüler topluluk listesi.",
        entriesCount = 215000,
        active = false,
        website = "github.com/hagezi/dns-blocklists",
        category = "Güvenlik"
      ),
      BlocklistEntry(
        id = "hagezi-multi-light",
        name = "HaGeZi's Multi LIGHT",
        description = "Günlük kullanımda hiçbir sitenin bozulmaması garanti edilen hafif HaGeZi filtresi.",
        entriesCount = 65000,
        active = false,
        website = "github.com/hagezi/dns-blocklists",
        category = "Genel"
      ),
      BlocklistEntry(
        id = "1hosts-pro",
        name = "1Hosts (Pro)",
        description = "Kötü amaçlı alan adları, izleyiciler, reklamlar ve telemetri için agresif filtre.",
        entriesCount = 380000,
        active = false,
        website = "1hosts.cf",
        category = "Güvenlik"
      ),
      BlocklistEntry(
        id = "1hosts-lite",
        name = "1Hosts (Lite)",
        description = "1Hosts'un günlük ev ve mobil kullanım için optimize edilmiş hafif sürümü.",
        entriesCount = 95000,
        active = false,
        website = "1hosts.cf",
        category = "Genel"
      ),
      BlocklistEntry(
        id = "goodbye-ads",
        name = "GoodbyeAds",
        description = "Mobil uygulamalar, oyunlar ve web reklamlarını engelleyen kapsamlı liste.",
        entriesCount = 145000,
        active = false,
        website = "github.com/jerryn70/GoodbyeAds",
        category = "Genel"
      ),
      BlocklistEntry(
        id = "steven-black",
        name = "Steven Black Hosts",
        description = "Kötü amaçlı yazılım, reklam ve izleyicileri birleştiren dünyaca ünlü hosts listesi.",
        entriesCount = 194000,
        active = false,
        website = "github.com/StevenBlack/hosts",
        category = "Genel"
      ),
      BlocklistEntry(
        id = "easylist",
        name = "EasyList",
        description = "Uluslararası web sayfalarındaki reklam banner ve açılır pencerelerini hedefler.",
        entriesCount = 82000,
        active = false,
        website = "https://easylist.to",
        category = "Genel"
      ),
      BlocklistEntry(
        id = "easyprivacy",
        name = "EasyPrivacy",
        description = "Kullanıcı davranışlarını takip eden web analitik ve izleme komut dosyalarını engeller.",
        entriesCount = 42000,
        active = false,
        website = "https://easylist.to",
        category = "Gizlilik"
      ),
      BlocklistEntry(
        id = "adaway",
        name = "AdAway Default Blocklist",
        description = "Android cihazlar için optimize edilmiş mobil reklam engelleme kuralları.",
        entriesCount = 38000,
        active = false,
        website = "https://adaway.org",
        category = "Genel"
      ),
      BlocklistEntry(
        id = "peter-lowe",
        name = "Peter Lowe's Ad and tracking server list",
        description = "1999'dan beri güncellenen, reklam ve izleyici sunucuları için sıfır hatalı pozitif listesi.",
        entriesCount = 4100,
        active = false,
        website = "pgl.yoyo.org/adservers",
        category = "Gizlilik"
      ),
      BlocklistEntry(
        id = "nocoin",
        name = "NoCoin Filter List",
        description = "Tarayıcınız üzerinden izinsiz kripto para madenciliği (kriptojacking) yapan komut dosyalarını engeller.",
        entriesCount = 1200,
        active = false,
        website = "github.com/hoshsadiq/adblock-nocoin-list",
        category = "Güvenlik"
      ),
      BlocklistEntry(
        id = "fanboy-annoyance",
        name = "Fanboy's Annoyance List",
        description = "Çerez uyarı pencerelerini, sosyal medya widget'larını ve rahatsız edici açılır pencereleri temizler.",
        entriesCount = 68000,
        active = false,
        website = "https://easylist.to",
        category = "Gizlilik"
      ),
      BlocklistEntry(
        id = "turkish-ad-hosts",
        name = "Turkish Ad Hosts (Türkiye)",
        description = "Türkçe haber, video, dizi ve e-ticaret sitelerindeki yerel reklam ve izleyicileri engeller.",
        entriesCount = 12500,
        active = false,
        website = "github.com",
        category = "Bölgesel"
      )
    )

    fun defaultNativeTracking() = listOf(
      NativeTrackingEntry("windows", "Windows", "Windows 10/11 sistem telemetrisi ve arka plan izleyicileri", true),
      NativeTrackingEntry("samsung", "Samsung", "Samsung SmartTV, SmartThings ve cihaz telemetrisi", true),
      NativeTrackingEntry("xiaomi", "Xiaomi", "MIUI/HyperOS sistem reklamları ve hata raporlama", true),
      NativeTrackingEntry("apple", "Apple", "Siri ses analizleri ve cihaz telemetrisi", false),
      NativeTrackingEntry("huawei", "Huawei", "HMS Core arka plan telemetri servisleri", false),
      NativeTrackingEntry("roku", "Roku", "Roku TV ve yayın cihazı izleme telemetrisi", false),
      NativeTrackingEntry("sonos", "Sonos", "Sonos hoparlör kullanım telemetrisi", false)
    )
  }
}

data class BlockedServiceEntry(
  val id: String = java.util.UUID.randomUUID().toString(),
  val name: String,
  val category: String,
  val icon: String = "🔥",
  val active: Boolean = false,
  val scheduleActive: Boolean = false,
  val startHour: Int = 22,
  val endHour: Int = 7
)

data class BlockedCategoryEntry(
  val id: String = java.util.UUID.randomUUID().toString(),
  val name: String,
  val description: String,
  val active: Boolean = false
)

data class ParentalControlSettings(
  val services: List<BlockedServiceEntry> = defaultServices(),
  val categories: List<BlockedCategoryEntry> = defaultCategories(),
  val safeSearch: Boolean = true,
  val youtubeRestrictedMode: Boolean = false,
  val blockBypass: Boolean = true
) {
  companion object {
    fun defaultServices() = listOf(
      BlockedServiceEntry("tinder", "Tinder", "Yetişkin", "🔥", true),
      BlockedServiceEntry("snapchat", "Snapchat", "Sosyal Medya", "👻", true),
      BlockedServiceEntry("tiktok", "TikTok", "Sosyal Medya", "🎵", false),
      BlockedServiceEntry("instagram", "Instagram", "Sosyal Medya", "📸", false),
      BlockedServiceEntry("youtube", "YouTube", "Video & Yayın", "▶️", false),
      BlockedServiceEntry("roblox", "Roblox", "Oyunlar", "🎮", false),
      BlockedServiceEntry("fortnite", "Fortnite", "Oyunlar", "🕹️", false),
      BlockedServiceEntry("discord", "Discord", "Mesajlaşma", "💬", false),
      BlockedServiceEntry("facebook", "Facebook", "Sosyal Medya", "👥", false),
      BlockedServiceEntry("netflix", "Netflix", "Video & Yayın", "🎬", false),
      BlockedServiceEntry("steam", "Steam", "Oyunlar", "🎲", false),
      BlockedServiceEntry("reddit", "Reddit", "Sosyal Medya", "🤖", false),
      BlockedServiceEntry("twitter", "Twitter", "Sosyal Medya", "🐦", false)
    )

    fun defaultCategories() = listOf(
      BlockedCategoryEntry("gambling", "Kumar", "Kumar içeriklerini engeller.", true),
      BlockedCategoryEntry("dating", "Flört", "Tüm flört web sitelerini ve uygulamalarını engeller.", true),
      BlockedCategoryEntry("porn", "Pornografi", "Pornografik ve yetişkin içerikleri engeller.", false),
      BlockedCategoryEntry("piracy", "Korsanlık", "Telif hakkı ihlali ve torrent sitelerini engeller.", false),
      BlockedCategoryEntry("social-networks", "Sosyal Ağlar", "Tüm sosyal medya sitelerini engeller.", false)
    )
  }
}

data class AllowDenyItem(
  val id: String = java.util.UUID.randomUUID().toString(),
  val domain: String = id,
  val active: Boolean = true,
  val comment: String = "",
  val addedAt: Long = System.currentTimeMillis()
)

data class DnsLogEntry(
  val id: String = java.util.UUID.randomUUID().toString(),
  val timestamp: String = "şimdi",
  val domain: String,
  val rootDomain: String = domain,
  val clientIp: String? = "37.130.67.187",
  val deviceName: String? = "Hasiggome-PC",
  val blocked: Boolean = false,
  val blockReason: String? = null,
  val protocol: String = "DoH",
  val responseTimeMs: Int? = 14
)

typealias DnsLogItem = DnsLogEntry

data class DomainMetric(
  val domain: String,
  val queries: Long = 0L,
  val queriesCount: Long = queries,
  val percentage: Float = 0f,
  val root: String = ""
)

data class DeviceMetric(
  val name: String,
  val queries: Long = 0L,
  val deviceName: String = name,
  val queriesCount: Long = queries,
  val percentage: Float = 0f,
  val clientIp: String = ""
)

data class AnalyticsSummary(
  val totalQueries: Long = 125932L,
  val blockedQueries: Long = 14899L,
  val blockRate: Double = 11.83,
  val blockedPercentage: Float = blockRate.toFloat(),
  val topAllowedDomains: List<DomainMetric> = emptyList(),
  val topBlockedDomains: List<DomainMetric> = emptyList(),
  val topBlockedReasons: Map<String, Long> = emptyMap(),
  val topDevices: List<DeviceMetric> = emptyList(),
  val topDomains: List<DomainMetric> = emptyList(),
  val gafamMetrics: Map<String, Pair<Double, Long>> = emptyMap(),
  val encryptedDnsPercentage: Float = 0f,
  val dnssecPercentage: Float = 0f,
  val topCountries: List<Pair<String, Double>> = emptyList()
)



data class RewriteItem(
  val id: String = java.util.UUID.randomUUID().toString(),
  val domain: String,
  val answer: String
)


data class AnalyticsItemDto(
  val id: String? = null,
  val name: String? = null,
  val company: String? = null,
  val queries: Long? = null,
  val validated: Boolean? = null,
  val encrypted: Boolean? = null,
  val code: String? = null
)

data class DiagnosticTestResult(
  val status: String = "",
  val protocol: String = "",
  val profileId: String = "",
  val clientIp: String = "",
  val serverPoP: String = "",
  val latencyMs: Int = 0,
  val isEncrypted: Boolean = true
)

data class ConfigSettings(
  val logsEnabled: Boolean = true,
  val logDomains: Boolean = true,
  val logClientIps: Boolean = false,
  val logRetention: String = "1 ay",
  val logStorageLocation: String = "İsviçre",
  val blockPage: Boolean = false,
  val ednsClientSubnet: Boolean = true,
  val cacheBoost: Boolean = false,
  val cnameFlattening: Boolean = true,
  val web3: Boolean = false,
  val bypassAgeVerification: Boolean = false,
  val rewrites: List<RewriteItem> = emptyList()
)

data class ConfigStatus(
  val status: String = "Yok",
  val clientIp: String? = null
)
