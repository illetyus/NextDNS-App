# Kod kalitesi incelemesi — 10 Ekim 2026

**Sonuç:** Teknik temel ve teslim kanıtı tutarlıdır; kaynak incelemesi **3 yüksek öncelikli ve 7 orta öncelikli kod kusuru** ortaya çıkarmıştır. Son cihaz kabulü/yayın sürecine geçmeden bu kayıtlar için düzeltme ve hedefli doğrulama gerekir. Başarılı CI sonucu, aşağıdaki koşullu hata yollarının doğru çalıştığını kanıtlamaz.

İncelenen kaynak commit’i: [`4abf26a1a556cf42235fbf05c70f89cb5ad79e8c`](https://github.com/illetyus/open-source-client-for-nextdns/tree/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c). Başarılı [main CI #38004548934](https://github.com/illetyus/open-source-client-for-nextdns/actions/runs/38004548934) ve imzalı APK/AAB kanıtları aynı commit’e bağlıdır. Bu denetim belgeleri uygulama kodunu veya imzalı APK içeriğini değiştirmez.

## Yöntem ve kapsam

İnceleme; API sözleşmeleri, repository/cache/oturum sınırı, ViewModel iş kapsamları, profil ve hesap işlemleri, DNS tanılama, SSE/export, Android yaşam döngüsü, bildirimler, yerel anahtar saklama, beş dilin sunum yolları, hukuk/lisans okuyucuları, Gradle/CI ve yayın kapıları üzerinde yapılmıştır. Önceki Faz 2–8 işleri yeniden geliştirilmemiştir; Faz 9’un tamamlandığı veya Faz 10’un başladığı sonucu çıkarılmaz.

Kaynak akışı ile elde tutulan XML/JaCoCo, runtime ve imzalı paket kayıtları birlikte değerlendirilmiştir. Kaynakta hata yolu görüldüğünde tetikleyici ve kapanış ölçütü [hata defterine](HATA_DEFTERI_TR.md) yazılmıştır. Kaynakta doğrulama, gerçek Android/BrowserStack tekrar üretimi ile eş anlamlı değildir. Cihaz/gerçek hesap isteği, mutasyon veya Play yayını bu incelemede yapılmamıştır.

## Doğrulanan olumlu kanıtlar

| Alan | Mevcut kanıt | Pratik sınırı |
|---|---|---|
| Dal ve geçmiş | main uzaktaki aynı commit; önceki PR #17–19 birleşmiş, inceleme başlangıcında açık PR yok | Yeni kaynak bulgularını ortadan kaldırmaz |
| Build / Android Lint | CI debug/release APK, AAB ve androidTest çıktıları ile lintDebug başarılı | OEM, gerçek cihaz ANR veya tüm release Lint çeşitlerinin sonucu değildir |
| JVM / Compose / sözleşme testleri | XML toplamı **67 test, 0 hata, 0 atlama**; 12 üretilmiş ekran görüntüsü | Tüm ekranların, bütün ağ ve yarış koşullarının kapsamı değildir |
| Release runtime güvenliği | **10 test, 0 hata/atlama**, Android 15/API 35 x86_64 emülatörü; gerçek TLS/Keystore ve logcat fixture kanıtı | Fiziksel cihaz, gerçek hesap ve BrowserStack sonucu değildir |
| Anahtar/ağ mimarisi | Keystore AES-GCM, şifreli yerel anahtar, backup/transfer dışlaması; release debuggable=false, cleartext=false; export istemcisi anahtarsız | Yanlış oturum yanıtı veya bloklayıcı çağrı iptali için aşağıdaki kayıtlar ayrıca açık |
| Bağımlılık güvenliği | Gerçek app release grafiğinde **50 Snyk bağımlılığı**, high/critical eşiğinde 0 raporlanan bulgu | Uygulama mantığı, tüm zafiyet sınıfları veya sıfır risk beyanı değildir |
| Lisans/paket | Tam çevrimdışı LICENSE/NOTICE dağıtımı, bağımlılık envanteri, DEX test-kodu kapısı, aynı payload ve APK/AAB imza kanıtı | Nihai yayın/lisans onayı yerine geçmez |
| Dil ve hukuk bütünlüğü | Bu turda **552 kaynak × 5 dil** kontrolü ve DRAFT hash/revizyon kontrolü yeniden geçti | Çeviri niteliği ve bütün dinamik menüler kaynak eşitliğiyle kanıtlanmaz; NDNS-009 açık |
| Yayın kapısı | Draft şeması geçiyor; gerçek yayın doğrulaması DRAFT metin nedeniyle beklenen şekilde duruyor | Hukuki veya Play onayı verilmemiştir |

Mevcut inceleme APK’sı: `OpenSourceClientForNextDNS-1.0-review.apk`, 14.321.583 byte; SHA-256 `c8655f88c12b469042f30b43ea7fc99fcbf3e741b412f8f6a322c3c7b3ef5450`. Yerel dosya hash’i bu turda yeniden hesaplanmıştır. Özel imza anahtarı ve parola inceleme kayıtlarına eklenmez.

## Yüksek öncelikli sonuçlar

1. **NDNS-001 — oturum/profil izolasyonu:** geciken log/profil yanıtları bağlam değiştikten sonra UI veya preferences üzerine yazabilir. Yerel profil yükleme logları zaten temizler; kayıt, eski verinin sonradan yeniden oluşması üzerinedir. Bildirim worker’ındaki mevcut `isCurrentSession` kontrolü olumlu bir örnektir ancak repository çağrılarını korumaz.
2. **NDNS-002 — mutasyon doğrulaması:** A için yakalanan yazma hedefi, sonradan aktif olan B hedefinden GET yapılarak doğrulanabilir. Yazma ve doğrulama aynı oturum/profil snapshotına bağlanmalıdır.
3. **NDNS-003 — DNS durum doğruluğu:** önceki `ok` sonucundan sonra timeout eski başarı durumunu korur; başka profil için ölçülen `ok` de seçili profilin korunduğu şeklinde sunulabilir. Uygulama hesap yönetir; profil seçimi Android Private DNS ayarını otomatik değiştirmez.

Orta öncelikli kayıtlar Web3 mapper eksikliği, arka plan tanılaması, ağ çağrısı iptali/kapanışı, senkronizasyon bayrakları, Logs menüsünün dili ve export istisna sınırıdır. Her birinin kaynak bağlantısı, koşulu ve kabul ölçütü [hata defterindedir](HATA_DEFTERI_TR.md).

## Test kapsamı ve bakım niteliği

| JaCoCo kapsamı | Satır kapsamı | Dal kapsamı |
|---|---:|---:|
| Bütün rapor | **%20,73** (2.021/9.750) | **%10,28** (428/4.163) |
| NextDnsRepository sınıfı | **%7,06** (82/1.161) | **%0,29** (5/1.696) |
| Repository paketi; coroutine üretilmiş sınıflar dahil | **%6,67** (110/1.648) | **%0,46** (9/1.973) |
| Hukuk veri paketi | **%100** (40/40) | **%77,78** (28/36) |

Bu değerler elde tutulan JVM raporunun doğrudan XML sayaçlarıdır. Kotlin/Compose/coroutine üretilmiş kodu oranları etkiler. API 35 native güvenlik testlerinin satırları bu raporda değildir; özellikle Keystore güvenliğini yalnız düşük JVM yüzdesinden başarısız saymak doğru olmaz. Buna karşılık repository profil/mutasyon yollarının hedefli senaryolarla test edilmemesi, bulunan kusurların mevcut CI’dan geçmesini açıklar. Genel yüzdeyi yapay biçimde yükseltmek yerine NDNS kayıtlarının kapanış senaryoları sınanmalıdır.

Repository 2.456, API dosyası 1.121, ViewModel 738 satırdır. Statik ağ istemcisi ve global Application erişimi, hata/yarış fixture’larını uygulama davranışına kadar taşıyan testleri zorlaştırır. Küçük mapper, oturum ve enjekte edilebilir API sınırlarıyla ilerlemek uygundur; bu bir mimariyi yeniden kurma veya önceki fazları tekrarlama önerisi değildir. Q-001/Q-002 kayıtları bu ayrımı korur.

GitHub’da normal PR/CI yolu uygulanmış olsa da main için eski branch protection endpoint’i `Branch not protected`, etkin branch rules listesi `[]` sonucunu vermiştir. Zorunlu check/review kuralı platformca dayatılmamaktadır; Q-003 süreç gözlemidir. Depo yönetim izinleri bu incelemede değiştirilmez.

## Marka, hukuk ve kalan aşama

Özgün ağ düğümü simgesi önceki NextDNS kalkanının yerine kaynakta uygulanmıştır; özgün kaynak ve dağıtım varlıkları kayıtlıdır. Önceki kalkan bu APK’nın simgesi olarak raporlanmaz. Gerçek launcher/temalı ikon ve mağaza sunumu B11–B12 kapsamında açık kalır; marka izni veya tescil incelemesinin tamamlandığı varsayılmaz. Bağımsız/gayriresmî adlandırma, teknik paket kimliği, sınıflar ve API adresleri korunmuştur.

Hukuk metinleri **DRAFT**, yayıncı/iletişim/public Privacy/final onay alanları doğrulanmamış; BrowserStack B01–B22 **22 NOT_RUN** durumundadır. Yeni kişi/iletişim, hukuk sonucu, hizmet sürekliliği, SLA veya destek taahhüdü eklenmez. Son yayıncı incelemesi ve Play internal/closed/prelaunch/Data safety kayıtları henüz yoktur.

Son sıra: kayıtlı düzeltmeler → hedefli regresyon ve PR/CI → değişen uygulama koduna bağlı yeni imzalı aday → BrowserStack kabulü/yeniden test → son metin-yayıncı incelemesi ve Play kanalları. Bu inceleme son cihaz kabul aşamasını başlatmaz.

## İzlenebilir kayıtlar ve teknik kaynaklar

- [Hata defteri](HATA_DEFTERI_TR.md): 10 açık kod kusuru, 3 kalite/süreç gözlemi ve önceki giderilmiş kayıtlar.
- [JSON hata kaydı](release/quality-ledger.json): önem, durum, kaynak konumu/hash’i, kapanış ölçütü ve ilgili B vakası; düzeltme/tekrar test alanları henüz boş.
- [Ölçüm/kanıt özeti](release/code-quality-evidence-2026-10-10.json): CI adımları, XML sayaçları, paket hash’i, bu turdaki validator sonuçları ve kapsam sınırı.
- [NextDNS resmî API belgesi](https://nextdns.github.io/api/): HTTP 200 ile semantik hatalar, settings.web3, nested GET/PATCH ve export sözleşmesi; 10 Ekim 2026 erişimi.
- [OkHttp 4.10.0 ResponseBody kaynağı](https://raw.githubusercontent.com/square/okhttp/parent-4.10.0/okhttp/src/main/kotlin/okhttp3/ResponseBody.kt): kaynak kapanışı ve body.string yolu.
- [OkHttp 4.10.0 Call kaynağı](https://raw.githubusercontent.com/square/okhttp/parent-4.10.0/okhttp/src/main/kotlin/okhttp3/Call.kt), [Request kaynağı](https://raw.githubusercontent.com/square/okhttp/parent-4.10.0/okhttp/src/main/kotlin/okhttp3/Request.kt): gerçek çağrı iptali ve geçersiz URL istisnası.
- [Kotlin ensureActive belgesi](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines/ensure-active.html): coroutine iptalinin işbirlikçi olması; bu mekanizma bloklayıcı OkHttp çağrısını kendiliğinden kapatmaz.
