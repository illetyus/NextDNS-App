# Faz 8 — Doğrudan bağımlılık lisans denetimi

**Denetim tarihi:** 2026-10-08  
**Temel:** `app/build.gradle.kts` + `gradle/libs.versions.toml` + kaynak projenin lisans beyanları.  
**Durum:** Doğrudan paket aileleri için belgelenmiş ön denetim; **çözülmüş (resolved) `releaseRuntimeClasspath` ve her transitif POM henüz bağımsız doğrulanmadı**.

**9 Ekim ek kaydı:** Yukarıdaki tablo 8 Ekim kaynak ön incelemesidir. Faz 9.5 CI'sında `releaseRuntimeClasspath` ve `coreLibraryDesugaring` gerçekten çözülmüş; 138 modül/98 artifact hash'i, POM lisansları ve 22 gömülü bildirim çıkarılmıştır. Kesin varyant, metadata istisnaları ve bir doğrudan POM alanı boşluğu [çözülmüş lisans gözlemleri](release/RESOLVED_LICENSE_OBSERVATIONS.md) içinde ayrılır. Son imzalı paket/NOTICE dağıtımı onayı bu çıktıdan türetilmez.

## 1. Neden projenin Apache-2.0 lisansı tek başına yeterli değil?

`LICENSE` projenin özgün kaynaklarını kapsar. Android APK'nın içerdiği transitif yazılım bileşenleri kendi telif haklarını ve lisanslarını korur. Çıktı APK/AAB'nin gerçek sürümleri ve NOTICE gereksinimleri yayından önce kontrol edilir.

Google Play, kullanılan üçüncü taraf SDK'larının veri davranışından uygulama geliştiricisini sorumlu tutar:
https://support.google.com/googleplay/android-developer/answer/13326895

## 2. Doğrudan dependency aileleri — kaynak seviyesi

| Aile | Repo sürümü | Build kullanım alanı | Üst proje / lisans kanıtı | Durum |
|---|---|---|---|---|
| AndroidX Core, Compose, Material3, Lifecycle, Navigation, Activity, WorkManager, DataStore, Splashscreen | `gradle/libs.versions.toml` ve Compose BOM `2024.09.00` | **runtime** | https://android.googlesource.com/platform/frameworks/support/ ; çoğunlukla Apache-2.0 (exact artifact + BOM-resolved sürüm teyidi gerekli) | **Transitif açık** |
| Kotlin/kotlinx.coroutines `1.10.2` | `core`, `android`; Kotlin plugin `2.2.10` | runtime / build | https://github.com/Kotlin/kotlinx.coroutines ; Apache-2.0 | **Transitif açık** |
| OkHttp + Logging Interceptor `4.10.0` | API HTTP | runtime | https://github.com/square/okhttp/blob/master/LICENSE.txt ; Apache-2.0 | **Transitif açık** |
| Retrofit `2.12.0` + converter-moshi `2.12.0` | REST HTTP | runtime | https://github.com/square/retrofit/blob/trunk/LICENSE.txt ; Apache-2.0 (repo lisans/sürüm doğrula) | **Transitif açık** |
| Moshi + Moshi Kotlin `1.15.2` | JSON | runtime | https://github.com/square/moshi/blob/master/LICENSE.txt ; Apache-2.0 | **Transitif açık** |
| `com.android.tools:desugar_jdk_libs:2.1.5` | `app/build.gradle.kts` | runtime | https://android.googlesource.com/platform/tools/desugar_jdk_libs/ ; sürüme özgü metadata incelenecek | **Açık** |
| Robolectric `4.16.1` | JVM test | test only | https://github.com/robolectric/robolectric/blob/master/LICENSE — MIT ana lisansı, bazı dosyaları Apache-2.0 | **Dağıtım dışı olduğu doğrulanmalı** |
| Roborazzi `1.59.0` | screenshot test + plugin | test/build | https://github.com/takahirom/roborazzi/blob/main/LICENSE — Apache-2.0 | **Dağıtım dışı olduğu doğrulanmalı** |
| JUnit `4.13.2`, AndroidX test, Espresso, MockWebServer `4.10.0`, kotlinx-coroutines-test | test | test only | https://github.com/junit-team/junit4 , https://github.com/android/android-test , https://github.com/square/okhttp | **Dağıtım dışı olduğu doğrulanmalı** |
| JaCoCo | `app/build.gradle.kts` | build/test coverage | https://www.jacoco.org/jacoco/trunk/doc/license.html — EPL-2.0 | **Dağıtım dışı** |
| Gradle / Android Gradle Plugin `9.1.1` / KSP `2.3.5` | build | build only | https://github.com/google/ksp ; https://gradle.org/ | **Dağıtım dışı olduğu doğrulanmalı** |

**Not:** Bu bir *lisans kanıtına dayalı ön tablo*dur; her paketin **exact resolved artifact sürümüne** ait POM metadata bu belgeyle tek tek doğrulanmış değildir. Özellikle Maven publish/variant farkları sebebiyle bu belgeyi dağıtılan bileşenlerin eksiksiz listesi olarak göstermeyin.

## 3. Kullanıcının gizlilik kararına aykırı olacak SDK'lar için negatif kontrol

İlk sürümde **reklam, satın alma/billing, Firebase Analytics/Crashlytics, Sentry** planlanmıyor. Başka bir fazda kontrollü karar alınmadan bu SDK'lar production runtime graph'a girmemeli.

`scripts/verify_release_dependencies.sh` dosyası Gradle'dan `releaseRuntimeClasspath` bağımlılık ağacını alır ve yaygın reklam/telemetri/billing koordinatlarını arar. Bu **yalnız negatif kontrol**dür; APK içindeki tüm kod veya tüm ağ çağrılarını kanıtlamaz.

Yerel çalıştırma:
```bash
bash scripts/verify_release_dependencies.sh
```

Üretilen rapor: `build/compliance/releaseRuntimeClasspath.txt` (git'e commit edilmeyen çalışma çıktısı).

## 4. Yayın kabul eşiği

- [x] Gradle `releaseRuntimeClasspath` gerçek CI build ortamında çözülmüş; raw inventory ve hash raporu artifact olarak saklanmış (yukarıdaki 9 Ekim ek kaydı).
- [ ] Tüm dağıtılan koordinatların **tam sürüm**, SPDX/lisans metni, gerekli attribution/NOTICE girdisi incelenmiş.
- [ ] Gerekli lisans bildirimleri kullanıcı tarafından erişilebilir hale getirilmiş.
- [ ] `assembleRelease` ve bağımlılık negatif kontrolü başarılı; AAB/APK analiziyle üçüncü taraf SDK beyanları tutarlı.
- [ ] Reklam/izleme SDK'sı bulunmadığı statik tarama **ve** gerçek cihaz ağ gözlemiyle doğrulanmış.
