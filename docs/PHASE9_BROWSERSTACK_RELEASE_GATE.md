# Faz 9 — BrowserStack gerçek cihaz test kapısı (ZORUNLU)

**Karar:** Google Play herkese açık yayınından **önce** BrowserStack üzerinde test yapılmadan release kabul edilmeyecek. Bu dosya Faz 8'de planlandı; **henüz BrowserStack testleri çalıştırılmadı**.

## 1. Kullanılacak hatlar

- **App Live:** Gerçek Android cihazlarında manuel keşif, kullanıcı akışı, konuşma ekranları, farklı form faktörleri, ekran okuyucu ve izin akışları.
- **App Automate / Espresso:** Uygulama APK (veya AAB) ve ayrıca `androidTest` test APK'sı ile tekrarlanabilir otomatik E2E.
- Android `testDebugUnitTest` / Robolectric/Compose JVM testleri **BrowserStack gerçek cihaz Espresso testi değildir**; ayrı gerçek cihaz testleri yazılmalıdır.
- BrowserStack gerektiğinde paketleri kendi sertifikasıyla yeniden imzalayabilir; sertifika fingerprint'ine bağlı başka SDK'lar varsa etki ayrıca test edilir.

Referanslar:
- https://www.browserstack.com/docs/app-automate/espresso/getting-started
- https://www.browserstack.com/docs/app-automate/espresso/upload-app-from-filesystem

## 2. Ön koşul

1. Faz 8 marka/API kullanım kararı ve kaynak lisansı kontrolü belgelendi.
2. Entegre development head/main üzerinde compile, lint, unit/API contract/Compose, Snyk, coverage yeşil.
3. Test edilecek **aynı sürüm**, git commit SHA + `versionCode` + artifact hash ile sabitlendi.
4. Gerçek kullanıcı API anahtarı kullanılmaz. Ayrı bir **test NextDNS hesabı, ayrı test profilleri**, önceden hazırlanmış sentetik domain/rule/log senaryoları kullanılır.
5. Geçici test credentials GitHub deposuna, issue'ya, loga veya ekran görüntüsüne konmaz; test sonrası revocation uygulanır.
6. BrowserStack'e yüklenen paketlerde geliştirici kimliği/marka durumu yanıltıcı olmamalı; üretim varlıkları ile test varlıkları ayrıştırılmalı.

## 3. Cihaz matrisi (somut stok test gününde seçilecek)

| Sınıf | Koşul / örnek | Neyi test eder |
|---|---|---|
| Android API 24–25 | desteklenen en eski OS; BrowserStack stok durumuna bağlı | minSdk=24 ve `java.time` desugaring |
| Android 9–11 | mevcut gerçek Android cihaz | ağ/hata ve UI temel akışlar |
| Android 12L+ tablet | medium/expanded pencere | rail/sidebar, landscape, büyük ekran |
| Android 13+ telefon | bildirim izni | `POST_NOTIFICATIONS` opt-in/denial |
| Android 15/16+ telefon | güncel güvenlik/görüntü | lifecycle, edge-to-edge ve bildirimler |
| Foldable | katla/aç, pencere yeniden boyutlandırma | Compose state korunumu, adaptif gezinme |

Gerçek cihaz modeli ve Android sürümü **test anındaki BrowserStack katalogundan kaydedilecek**; burada teyit edilmemiş model/stok iddiası yoktur.

## 4. Kapsam — manuel ve otomatik

| ID | Senaryo | Kabul |
|---|---|---|
| B01 | Uygulama açılışı + login/invalid API key | Sunucu onayı yoksa sahte success yok |
| B02 | Profil listeleme, yaratma, değiştirme, silme | UI ancak authoritative GET ile onaylar |
| B03 | Security/Privacy/Parental/Allow/Deny/Settings değişikliği | Minimal mutation; hatada rollback ve açık mesaj |
| B04 | NextDNS web panelinde dışarıdan ayar değişikliği | Foreground açılış/refresh ile veriler uzlaşır, freshness açıklanır |
| B05 | İnternet kesilmesi/yeniden gelmesi; 401/429/500; timeout | Veri uydurulmaz, backoff, çökme yok, uygun yeniden deneme |
| B06 | Log SSE bağlanma/kesilme/yeniden bağlanma | Duplicate kaydı ve sahte canlı durum yok; background'da kapalı |
| B07 | Analytics ve log CSV export | API sözleşmesi ve gerçek indirme; API key başka hosta gitmez |
| B08 | Foreground/background, profil değişimi | Gereksiz polling durur, eski profil verisi gösterilmez |
| B09 | Notification opt-in, izin ret, channel off | Kullanıcı izni olmadan yayın yok; private lockscreen |
| B10 | WorkManager delay, dedupe, local mutation | Yanlış tekrar uyarısı yok; 30 dk işleyişi tam-zamanlı iddia etmez |
| B11 | Telefon/tablet/foldable compact/medium/expanded | Gezinme, yazı taşması, erişilebilir alanlar |
| B12 | TalkBack, büyük font, dark mode, yüksek kontrast | 48dp hedef, anlamlı semantik ve okunabilirlik |
| B13 | Logout/cache/clipboard/backup ögeleri | API key ifşası yok, hassas sorgu geçmişi diskte kalmaz |
| B14 | İmzasız/üretim varyantı test eşdeğerliği | Paket, applicationId, imza, min/targetSDK ve Crash/ANR kontrol |
| B15 | Güncelleme / eski sürümden veri migrasyonu | Anahtar düz metin bırakılmaz; kayıp profile sahte success yok |
| B16 | Düşük bellek/uygulama tekrar açma | Kalıcı işlem kaybı, çökme veya boş başarı bildirimi yok |

## 5. Gerçek cihaz otomasyonu ve güvenlik

- `androidTest` altında yeniden kullanılabilir en az **login, permission, mutation, navigation** Espresso senaryoları gerekli. Mevcut `app/src/androidTest/.../ExampleInstrumentedTest.kt` tek başına bu gereksinimi karşılamaz.
- CI/artifact pipeline debug APK + test APK üretmeli; BrowserStack test raporları, ekran görüntüleri, crash/ANR/loglar, SHA ve cihaz kimlikleri ilişkilendirilmeli.
- API anahtarı ve DNS sorgu domainleri test raporlarında redakte edilmeli.
- Gerçek satın alma veya reklam içermeyen ilk sürümde Billing/ads testi **uygulanmaz**; sonradan açılırsa yeni kapsama eklenir.

## 6. Yayın kararı

**PASS için:** B01–B16 içindeki geçerli senaryolar tamamlanmış olmalı; P0/P1 kritik/yüksek açık **0**; güvenlik ve API anahtarının yanlış aktarılması **0**; tüm zorunlu cihaz sınıfları test edilmiş; bulgular ve düzeltme sonrası tekrar testler aynı release adayıyla tutarlı olmalı.

**FAIL/BLOCK:** Kritik crash/ANR, izinsiz veri aktarımı, hatalı ayarın başarılı gösterilmesi, başka profil verisinin görünmesi, gizli API key sızıntısı, ciddi a11y/OS çökmesi veya marka/lisans izni eksikliği.

**Son sıra:** Faz 8 uygunluk -> entegre paket -> BrowserStack App Live + App Automate -> düzeltmeler ve yeniden test -> Play internal/closed testing + prelaunch -> kademeli prod yayın.

**Durum:** **NOT RUN — yayın kapısı açık.**
