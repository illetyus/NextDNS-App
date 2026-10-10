# BrowserStack Espresso gerçek cihaz sonuçları — 10 Ekim 2026

Yerel BrowserStack kimlik doğrulaması başarılıdır. İki CI APK çifti dört gerçek Android cihazında yürütülmüş; yerel sunucu ayarı eklendikten sonra aynı yüklemeler yeniden test edilmiştir. Sonuçlar tüm yayın kabulünün tamamlandığını göstermez. DRAFT ve önceki NDNS-011 bulgusu korunmuştur.

| Hat | Cihaz | Android | Geçti | Başarısız | Atlandı | Oturum sonucu |
|---|---|---|---:|---:|---:|---|
| debug-initial | Samsung Galaxy S20 | 10.0 | 2 | 1 | 11 | failed |
| debug-initial | Samsung Galaxy Tab S8 | 12.0 | 2 | 1 | 11 | failed |
| debug-initial | Google Pixel 7 | 13.0 | 2 | 2 | 10 | failed |
| debug-initial | Samsung Galaxy S26 | 16.0 | 2 | 2 | 10 | failed |
| release-initial | Samsung Galaxy S20 | 10.0 | 8 | 2 | 0 | failed |
| release-initial | Samsung Galaxy Tab S8 | 12.0 | 8 | 2 | 0 | failed |
| release-initial | Google Pixel 7 | 13.0 | 8 | 2 | 0 | failed |
| release-initial | Samsung Galaxy S26 | 16.0 | 8 | 2 | 0 | failed |
| debug-retry | Samsung Galaxy S20 | 10.0 | 3 | 0 | 1 | passed |
| debug-retry | Samsung Galaxy Tab S8 | 12.0 | 3 | 0 | 1 | passed |
| debug-retry | Google Pixel 7 | 13.0 | 3 | 1 | 0 | failed |
| debug-retry | Samsung Galaxy S26 | 16.0 | 3 | 1 | 0 | failed |
| release-retry | Samsung Galaxy S20 | 10.0 | 10 | 0 | 0 | passed |
| release-retry | Samsung Galaxy Tab S8 | 12.0 | 10 | 0 | 0 | passed |
| release-retry | Google Pixel 7 | 13.0 | 10 | 0 | 0 | passed |
| release-retry | Samsung Galaxy S26 | 16.0 | 10 | 0 | 0 | passed |

İlk debug çalışmasında release sınıfının 10 testi varyant kontrolü nedeniyle her cihazda atlanır; Android 10/12 üzerinde bildirim izin testi de sürüm koşulu nedeniyle atlanır. Yeniden debug çalışması yalnız dört genel native sınıfı seçer. Atlanan yöntemler başarılı sayılmaz. API sayaçları JUnit XML ile karşılaştırılmıştır.

Yerel TLS fixture bağlantıları ilk çalışmada `CONNECT 503` ile durmuştur. BrowserStack [cihaz üzerinde mock sunucu kullanımında](https://www.browserstack.com/docs/app-automate/espresso/use-mock-server) `allowDeviceMockServer=true` ister. Bu ayar gönderim betiğine eklenmiştir. Uygulamanın TLS doğrulaması veya teknik servis adresleri değiştirilmemiştir.

Bildirim kontrolü Android 13 ve 16 cihazlarında kanal görünürlüğü için `expected 0 / actual -1000` sonucuyla durmuştur. Android [kanal görünürlüğünü](https://developer.android.com/reference/android/app/NotificationChannel#setLockscreenVisibility(int)) yalnız sistemin ve notification ranker'ın değiştirebildiğini belirtir. Bu test varsayımı Q-005 olarak açık tutulur; izin reddi sonrası bildirim yayımlanmaması bu çalışma ile doğrulanmış sayılmaz. Gizli veri sızıntısı gözlendiği sonucu çıkarılmaz.

Aday kaynak commit'i `d63aea420207b8aacf7eb7866820bd173c42a519`, başarılı kaynak CI çalışması `38065297405`'tir. `0279d9f6cc48c0d171476761e92206246f046155` ana dalı ile uygulama/Gradle kaynak farkı yoktur. APK/test APK hash'leri, çalışma ve oturum kimlikleri, yöntem sonuçları ve JUnit hash'leri [makine kaydında](release/browserstack-espresso-results-2026-10-10.json); XML raporları [kanıt klasöründe](release/browserstack-espresso-reports/) saklanır. Kalıcı video indirmesi veya tam cihaz log incelemesi bu kaydın kapsamında değildir.

Release hattı CI'nin geçici sertifikasıyla imzalanmıştır; onaylı üretim imzası değildir. Release uygulama yükü önceki imzalı inceleme adayıyla imza dışında aynıdır. Yükleme öncesi hash, BrowserStack'in kurulu paketi yeniden imzalaması halinde kurulu paket hash'i olarak sunulmaz.

Gerçek NextDNS hesabı veya profil mutasyonu kullanılmamıştır. API 24/25 ve katlanabilir cihazlar doğrulanmış katalogda yoktur. İlk App Live denemelerindeki yatay hukuk ekranı hatası NDNS-011 açıktır. Beş dil, TalkBack, lifecycle, gerçek sunucu mutasyonları ve tüm B01–B22 kabulü tamamlanmamıştır. Yeni native bulguları [hata defterinde](release/quality-ledger.json) ve kısmi/başarısız alanları [hazırlık kaydında](release/readiness.json) izlenir.

BrowserStack anahtarı bu dosyalara veya Git'e yazılmamıştır. Yerel şifreli kayıt depo dışında tutulur. Uygulama ve hukuki belgeler DRAFT olarak kalır; yayın onayı verilmemiştir.
