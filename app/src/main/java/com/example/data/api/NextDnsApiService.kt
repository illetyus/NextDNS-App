package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit
import com.example.data.model.AnalyticsItemDto

// ==========================================
// NextDNS API Response DTOs
// ==========================================

data class NextDnsApiResponse<T>(
  val data: T? = null,
  val errors: List<ApiErrorDetail>? = null
)

data class ApiErrorDetail(
  val code: String? = null,
  val detail: String? = null
)

data class ProfileDto(
  val id: String,
  val name: String,
  val fingerprint: String? = null
)

data class SecurityDto(
  val threatIntelligenceFeeds: Boolean? = true,
  val aiThreatDetection: Boolean? = true,
  val googleSafeBrowsing: Boolean? = true,
  val cryptojacking: Boolean? = true,
  val dnsRebinding: Boolean? = true,
  val idnHomographs: Boolean? = true,
  val typosquatting: Boolean? = true,
  val dga: Boolean? = true,
  val nrd: Boolean? = true,
  val ddns: Boolean? = false,
  val parking: Boolean? = true,
  val csam: Boolean? = true,
  val tlds: List<TldDto>? = null
)

data class TldDto(
  val id: String
)

data class BlocklistDto(
  val id: String,
  val name: String? = null,
  val description: String? = null,
  val entries: Long? = null,
  val website: String? = null
)

data class NativeTrackingDto(
  val id: String
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class PrivacyDto(
  val blocklists: List<BlocklistDto>? = null,
  val natives: List<NativeTrackingDto>? = null,
  val disguisedTrackers: Boolean? = true,
  @Json(name = "allowAffiliate") val allowAffiliateLinks: Boolean? = false
)

data class ParentalServiceDto(
  val id: String,
  val active: Boolean? = true
)

data class ParentalCategoryDto(
  val id: String,
  val active: Boolean? = true
)

data class ParentalControlDto(
  val services: List<ParentalServiceDto>? = null,
  val categories: List<ParentalCategoryDto>? = null,
  val safeSearch: Boolean? = true,
  val youtubeRestrictedMode: Boolean? = false,
  val blockBypass: Boolean? = true
)

data class AllowDenyRuleDto(
  val id: String,
  val active: Boolean? = true
)

data class LogReasonDto(
  val id: String? = null,
  val name: String? = null
)

data class DeviceDto(
  val id: String?,
  val name: String?,
  val model: String?
)

data class DnsLogDto(
  val timestamp: Any? = null,
  val domain: String? = null,
  val root: String? = null,
  @Json(name = "client_ip") val clientIp: String? = null,
  @Json(name = "device_name") val deviceName: String? = null,
  val device: DeviceDto? = null,
  val status: String? = null,
  val reasons: List<LogReasonDto>? = null,
  val protocol: String? = null,
  val dnssec: Boolean? = null,
  @Json(name = "response_time") val responseTime: Int? = null
)

data class AnalyticsStatusItem(
  val status: String? = null,
  val queries: Long? = null
)

data class AnalyticsDeviceItem(
  val id: String? = null,
  val name: String? = null,
  val queries: Long? = null
)

data class AnalyticsDomainItem(
  val domain: String? = null,
  val queries: Long? = null,
  val root: String? = null
)

data class AnalyticsReasonItem(
  val id: String? = null,
  val name: String? = null,
  val queries: Long? = null
)

data class AnalyticsProtocolItem(
  val protocol: String? = null,
  val queries: Long? = null
)

data class SettingsDto(
  val logs: SettingsLogsDto? = null,
  val blockPage: SettingsBlockPageDto? = null,
  val performance: SettingsPerformanceDto? = null,
  val web3: Boolean? = null
)

data class SettingsLogsDto(
  val enabled: Boolean? = true,
  val retention: Int? = 720,
  val location: String? = "ch",
  val drop: SettingsLogsDropDto? = null
)

data class SettingsLogsDropDto(
  val ip: Boolean? = false,
  val domain: Boolean? = false
)

data class SettingsBlockPageDto(
  val enabled: Boolean? = false
)

data class SettingsPerformanceDto(
  val ecs: Boolean? = true,
  val cacheBoost: Boolean? = true,
  val cnameFlattening: Boolean? = true
)

data class NextDnsTestResponse(
  val status: String? = null,
  val protocol: String? = null,
  val profile: String? = null,
  val client: String? = null,
  val server: String? = null,
  val anycast: Boolean? = null
)

// ==========================================
// Strongly-Typed Request DTOs for Mutations
// ==========================================

data class NameRequest(
  val name: String
)

data class IdRequest(
  val id: String
)

data class SecurityUpdateRequest(
  val threatIntelligenceFeeds: Boolean? = null,
  val aiThreatDetection: Boolean? = null,
  val googleSafeBrowsing: Boolean? = null,
  val cryptojacking: Boolean? = null,
  val dnsRebinding: Boolean? = null,
  val idnHomographs: Boolean? = null,
  val typosquatting: Boolean? = null,
  val dga: Boolean? = null,
  val nrd: Boolean? = null,
  val ddns: Boolean? = null,
  val parking: Boolean? = null,
  val csam: Boolean? = null
)

@com.squareup.moshi.JsonClass(generateAdapter = true)
data class PrivacyUpdateRequest(
  val disguisedTrackers: Boolean? = null,
  @Json(name = "allowAffiliate") val allowAffiliateLinks: Boolean? = null
)

data class ParentalControlUpdateRequest(
  val safeSearch: Boolean? = null,
  val youtubeRestrictedMode: Boolean? = null,
  val blockBypass: Boolean? = null
)

data class ParentalItemRequest(
  val id: String,
  val active: Boolean? = true
)

data class ParentalActiveRequest(
  val active: Boolean
)

data class AllowDenyItemRequest(
  val id: String,
  val active: Boolean? = true
)

data class AllowDenyActiveRequest(
  val active: Boolean
)

data class SettingsUpdateRequest(
  val logs: SettingsLogsDto? = null,
  val blockPage: SettingsBlockPageDto? = null,
  val performance: SettingsPerformanceDto? = null,
  val web3: Boolean? = null
)

data class SettingsPerformanceUpdateRequest(
  val ecs: Boolean? = null,
  val cacheBoost: Boolean? = null,
  val cnameFlattening: Boolean? = null
)

data class SettingsLogsUpdateRequest(
  val enabled: Boolean? = null,
  val retention: Int? = null,
  val location: String? = null,
  val drop: SettingsLogsDropDto? = null
)

data class SettingsBlockPageUpdateRequest(
  val enabled: Boolean? = null
)

// ==========================================
// NextDNS Retrofit Service Interface
// ==========================================

interface NextDnsApiService {

  // Profiles
  @GET("profiles")
  suspend fun getProfiles(
    @Header("X-Api-Key") apiKey: String
  ): Response<NextDnsApiResponse<List<ProfileDto>>>

  @POST("profiles")
  suspend fun createProfile(
    @Header("X-Api-Key") apiKey: String,
    @Body body: NameRequest
  ): Response<NextDnsApiResponse<ProfileDto>>

  @DELETE("profiles/{profileId}")
  suspend fun deleteProfile(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String
  ): Response<ResponseBody>

  @PATCH("profiles/{profileId}")
  suspend fun renameProfile(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: NameRequest
  ): Response<ResponseBody>

  // Security
  @GET("profiles/{profileId}/security")
  suspend fun getSecurity(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String
  ): Response<NextDnsApiResponse<SecurityDto>>

  @PATCH("profiles/{profileId}/security")
  suspend fun updateSecurity(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: SecurityUpdateRequest
  ): Response<ResponseBody>

  @POST("profiles/{profileId}/security/tlds")
  suspend fun addSecurityTld(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: IdRequest
  ): Response<ResponseBody>

  @DELETE("profiles/{profileId}/security/tlds/{tld}")
  suspend fun removeSecurityTld(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Path("tld") tld: String
  ): Response<ResponseBody>

  // Privacy
  @GET("profiles/{profileId}/privacy")
  suspend fun getPrivacy(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String
  ): Response<NextDnsApiResponse<PrivacyDto>>

  @GET("profiles/{profileId}/privacy/blocklists")
  suspend fun getProfileBlocklists(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String
  ): Response<NextDnsApiResponse<List<BlocklistDto>>>

  @GET("profiles/{profileId}/privacy/natives")
  suspend fun getProfileNatives(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String
  ): Response<NextDnsApiResponse<List<NativeTrackingDto>>>

  @GET("privacy/blocklists")
  suspend fun getAvailableBlocklists(
    @Header("X-Api-Key") apiKey: String
  ): Response<NextDnsApiResponse<List<BlocklistDto>>>

  @PATCH("profiles/{profileId}/privacy")
  suspend fun updatePrivacy(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: PrivacyUpdateRequest
  ): Response<ResponseBody>

  @POST("profiles/{profileId}/privacy/blocklists")
  suspend fun addBlocklist(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: IdRequest
  ): Response<ResponseBody>

  @DELETE("profiles/{profileId}/privacy/blocklists/{blocklistId}")
  suspend fun removeBlocklist(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Path("blocklistId") blocklistId: String
  ): Response<ResponseBody>

  @POST("profiles/{profileId}/privacy/natives")
  suspend fun addNativeTracking(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: IdRequest
  ): Response<ResponseBody>

  @DELETE("profiles/{profileId}/privacy/natives/{nativeId}")
  suspend fun removeNativeTracking(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Path("nativeId") nativeId: String
  ): Response<ResponseBody>

  // Parental Control
  @GET("profiles/{profileId}/parentalcontrol")
  suspend fun getParentalControl(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String
  ): Response<NextDnsApiResponse<ParentalControlDto>>

  @PATCH("profiles/{profileId}/parentalcontrol")
  suspend fun updateParentalControl(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: ParentalControlUpdateRequest
  ): Response<ResponseBody>

  @POST("profiles/{profileId}/parentalcontrol/categories")
  suspend fun addParentalCategory(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: ParentalItemRequest
  ): Response<ResponseBody>

  @PATCH("profiles/{profileId}/parentalcontrol/categories/{categoryId}")
  suspend fun updateParentalCategory(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Path("categoryId") categoryId: String,
    @Body body: ParentalActiveRequest
  ): Response<ResponseBody>

  @DELETE("profiles/{profileId}/parentalcontrol/categories/{categoryId}")
  suspend fun removeParentalCategory(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Path("categoryId") categoryId: String
  ): Response<ResponseBody>

  @POST("profiles/{profileId}/parentalcontrol/services")
  suspend fun addParentalService(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: ParentalItemRequest
  ): Response<ResponseBody>

  @PATCH("profiles/{profileId}/parentalcontrol/services/{serviceId}")
  suspend fun updateParentalService(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Path("serviceId") serviceId: String,
    @Body body: ParentalActiveRequest
  ): Response<ResponseBody>

  @DELETE("profiles/{profileId}/parentalcontrol/services/{serviceId}")
  suspend fun removeParentalService(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Path("serviceId") serviceId: String
  ): Response<ResponseBody>

  // Denylist
  @GET("profiles/{profileId}/denylist")
  suspend fun getDenylist(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String
  ): Response<NextDnsApiResponse<List<AllowDenyRuleDto>>>

  @POST("profiles/{profileId}/denylist")
  suspend fun addDenylist(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: AllowDenyItemRequest
  ): Response<ResponseBody>

  @PATCH("profiles/{profileId}/denylist/{domain}")
  suspend fun toggleDenylist(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Path("domain") domain: String,
    @Body body: AllowDenyActiveRequest
  ): Response<ResponseBody>

  @DELETE("profiles/{profileId}/denylist/{domain}")
  suspend fun removeDenylist(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Path("domain") domain: String
  ): Response<ResponseBody>

  // Allowlist
  @GET("profiles/{profileId}/allowlist")
  suspend fun getAllowlist(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String
  ): Response<NextDnsApiResponse<List<AllowDenyRuleDto>>>

  @POST("profiles/{profileId}/allowlist")
  suspend fun addAllowlist(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: AllowDenyItemRequest
  ): Response<ResponseBody>

  @PATCH("profiles/{profileId}/allowlist/{domain}")
  suspend fun toggleAllowlist(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Path("domain") domain: String,
    @Body body: AllowDenyActiveRequest
  ): Response<ResponseBody>

  @DELETE("profiles/{profileId}/allowlist/{domain}")
  suspend fun removeAllowlist(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Path("domain") domain: String
  ): Response<ResponseBody>

  // Logs
  @Streaming
  @GET("profiles/{profileId}/logs/stream")
  suspend fun getLogsStream(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Query("search") search: String? = null
  ): retrofit2.Response<okhttp3.ResponseBody>

  @GET("profiles/{profileId}/logs")
  suspend fun getLogs(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Query("limit") limit: Int = 50,
    @Query("search") search: String? = null
  ): Response<NextDnsApiResponse<List<DnsLogDto>>>

  @DELETE("profiles/{profileId}/logs")
  suspend fun clearLogs(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String
  ): Response<ResponseBody>

  // Analytics
  @GET("profiles/{profileId}/analytics/status")
  suspend fun getAnalyticsStatus(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Query("device") device: String? = null,
    @Query("from") from: String? = null
  ): Response<NextDnsApiResponse<List<AnalyticsStatusItem>>>

  @GET("profiles/{profileId}/analytics/devices")
  suspend fun getAnalyticsDevices(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Query("device") device: String? = null,
    @Query("from") from: String? = null
  ): Response<NextDnsApiResponse<List<AnalyticsDeviceItem>>>

  @GET("profiles/{profileId}/analytics/domains")
  suspend fun getAnalyticsDomains(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Query("device") device: String? = null,
    @Query("from") from: String? = null,
    @Query("status") status: String? = null
  ): Response<NextDnsApiResponse<List<AnalyticsDomainItem>>>

  @GET("profiles/{profileId}/analytics/reasons")
  suspend fun getAnalyticsReasons(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Query("device") device: String? = null,
    @Query("from") from: String? = null
  ): Response<NextDnsApiResponse<List<AnalyticsReasonItem>>>

  
  @GET("profiles/{profileId}/analytics/companies")
  suspend fun getAnalyticsCompanies(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Query("device") device: String? = null,
    @Query("from") from: String? = null
  ): Response<NextDnsApiResponse<List<AnalyticsItemDto>>>

  @GET("profiles/{profileId}/analytics/destinations")
  suspend fun getAnalyticsDestinations(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Query("device") device: String? = null,
    @Query("from") from: String? = null,
    @Query("type") type: String? = null
  ): Response<NextDnsApiResponse<List<AnalyticsItemDto>>>

  @GET("profiles/{profileId}/analytics/dnssec")
  suspend fun getAnalyticsDnssec(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Query("device") device: String? = null,
    @Query("from") from: String? = null
  ): Response<NextDnsApiResponse<List<AnalyticsItemDto>>>

  @GET("profiles/{profileId}/analytics/encryption")
  suspend fun getAnalyticsEncryption(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Query("device") device: String? = null,
    @Query("from") from: String? = null
  ): Response<NextDnsApiResponse<List<AnalyticsItemDto>>>

  @GET("profiles/{profileId}/analytics/protocols")
  suspend fun getAnalyticsProtocols(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Query("device") device: String? = null,
    @Query("from") from: String? = null
  ): Response<NextDnsApiResponse<List<AnalyticsProtocolItem>>>

  // Settings
  @GET("profiles/{profileId}/settings")
  suspend fun getSettings(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String
  ): Response<NextDnsApiResponse<SettingsDto>>

  @PATCH("profiles/{profileId}/settings")
  suspend fun updateSettings(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: SettingsUpdateRequest
  ): Response<ResponseBody>

  @PATCH("profiles/{profileId}/settings/performance")
  suspend fun updateSettingsPerformance(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: SettingsPerformanceUpdateRequest
  ): Response<ResponseBody>

  @PATCH("profiles/{profileId}/settings/logs")
  suspend fun updateSettingsLogs(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: SettingsLogsUpdateRequest
  ): Response<ResponseBody>

  @PATCH("profiles/{profileId}/settings/blockPage")
  suspend fun updateSettingsBlockPage(
    @Header("X-Api-Key") apiKey: String,
    @Path("profileId") profileId: String,
    @Body body: SettingsBlockPageUpdateRequest
  ): Response<ResponseBody>
}

interface NextDnsTestService {
  @GET("/")
  suspend fun testConnection(): Response<NextDnsTestResponse>
}

object NextDnsNetworkClient {
  private const val BASE_URL = "https://api.nextdns.io/"
  private const val TEST_URL = "https://test.nextdns.io/"

  private val logging = HttpLoggingInterceptor().apply {
    level = HttpLoggingInterceptor.Level.BASIC
  }

  val client = OkHttpClient.Builder()
    .addInterceptor(logging)
    .connectTimeout(15, TimeUnit.SECONDS)
    .readTimeout(15, TimeUnit.SECONDS)
    .build()

  val moshi = Moshi.Builder()
    .add(KotlinJsonAdapterFactory())
    .build()

  val api: NextDnsApiService by lazy {
    Retrofit.Builder()
      .baseUrl(BASE_URL)
      .client(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(NextDnsApiService::class.java)
  }

  val testApi: NextDnsTestService by lazy {
    Retrofit.Builder()
      .baseUrl(TEST_URL)
      .client(client)
      .addConverterFactory(MoshiConverterFactory.create(moshi))
      .build()
      .create(NextDnsTestService::class.java)
  }
}
