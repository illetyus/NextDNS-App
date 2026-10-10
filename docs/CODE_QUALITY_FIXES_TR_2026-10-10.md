# Kod kalitesi düzeltmeleri — 10 Ekim 2026

**NDNS-001–010 kodda giderildi; 37 hedefli regresyon dahil 104 JVM testi başarılı. Cihaz kabulü bekliyor.** [PR #21](https://github.com/illetyus/open-source-client-for-nextdns/pull/21), [tam başarılı CI](https://github.com/illetyus/open-source-client-for-nextdns/actions/runs/38063979745); düzeltme commit’i `40aefa8a6446bd1f16824cc14e290b6307f35440`. PR CI, sentetik birleştirme ağacı `9eabdabd1a82bbca29ceef7169066a66f5fd8609` üzerinde çalışmıştır.

İlk inceleme ve 3 P1/7 P2 önem kaydı korunur. Durum `FIXED_JVM_VERIFIED_DEVICE_PENDING` olarak güncellenmiştir; Android/BrowserStack kabulüyle aynı anlamda kullanılmaz. [Hata defteri](HATA_DEFTERI_TR.md), [JSON kayıt](release/quality-ledger.json) ve [ölçüm kanıtı](release/code-quality-fix-evidence-2026-10-10.json) düzeltme ile vaka adlarını taşır.

## Yapılan düzeltmeler

| Kayıt | Değişiklik |
|---|---|
| NDNS-001 | Oturum nesli ve değişmez hesap/profil bağlamı, eski yanıtların UI/cache yazmasını engeller; oturum işleri iptal edilir. |
| NDNS-002 | Yazma ve doğrulama aynı hedefe bağlanır; 23 bölüm mutasyonunda istenen değerin geri okunduğu doğrulanır. |
| NDNS-003 | Başarısız ölçüm eski başarıyı güncel göstermez; olumlu profil durumu seçili ve ölçülen profil eşleşmesini gerektirir. |
| NDNS-004 | Web3 true/false sunucudan modele ve cache içine aktarılır; eksik alan önceki yetkili değeri korur. |
| NDNS-005 | Tanılama RESUMED yaşam döngüsünde tek sıralı döngü olarak çalışır; arka plana geçiş devam eden kontrolü iptal eder. |
| NDNS-006 | Coroutine iptali doğrudan OkHttp Call.cancel çağrısına bağlanır; SSE, tanılama ve export yanıtları yönetilir. |
| NDNS-007 | IP bağlama, katalog HTTP hatası ve JSON hata yollarında Response/body kaynakları kapanır. |
| NDNS-008 | Senkronizasyon haritası atomik güncellenir; en güncel istek belirteci ve finally temizliği eski isteğin yeni bayrağı silmesini engeller. |
| NDNS-009 | Tüm cihazlar seçimi null ile temsil edilir; beş dilde menü ve başlık çevrilir, gerçek cihaz adları korunur. |
| NDNS-010 | URL, belge sağlayıcısı açma/yazma/kapama işlemleri yönetilen Result sınırına alınır; iptal yeniden fırlatılır, kapama hatası başarı sayılmaz. |

## Doğrulama

- [Düzeltme öncesi CI](https://github.com/illetyus/open-source-client-for-nextdns/actions/runs/38060835008): yeni 9 testin tamamı başarısız, önceki 67 test başarılı; toplam 76. Bu sonuç 37 vakanın tümünün önce başarısız çalıştırıldığı iddiası değildir.
- Düzeltme sonrası: 104 JVM/contract/Compose/Robolectric testi; 0 başarısız, 0 atlanan. 37 gerekli regresyon gerçek JUnit XML adları ve SHA-256 değerleriyle doğrulanır. Kontrollü HTTP/TLS ve belge sağlayıcısı fixture’ları gerçek hesaba işlem yapmaz.
- Android Lint; debug APK, release APK ve AAB derlemesi; lisans/NOTICE, release bağımlılık/SDK ve gerçek DEX test-kodu kapıları başarılı. Snyk `50` bağımlılık üzerinde high/critical kapısında 0 raporlanan bulgu döndürdü.
- Android 15/API 35 emülatöründe release payload’a bağlı 10 native güvenlik testi: 0 başarısız, 0 atlanan; imza dışı payload eşitliği ve sentetik anahtarın logcat’te bulunmaması doğrulandı.
- Beş dilde 556 kaynak; hukuk hash/revizyon ve DRAFT yayın şeması kontrolleri başarılı. JaCoCo toplam satır %27.87, repository sınıfı satır %33.3; eski ölçümler sırasıyla %20,73 ve %7,06. Kotlin/coroutine üretilmiş kodu sayaçları etkiler; native testler JVM kapsamına dahil değildir.

## Kalan işler

Q-001 için hedefli regresyon boşluğu giderildi; Q-002 repository bakım gözlemi ve Q-003 platformca zorunlu tutulmayan PR/check süreci açık kalır. Depo yönetim kuralları değiştirilmedi. Önceki APK yeni düzeltmelerin kanıtı değildir; başarılı main CI’dan aynı yerel anahtarla yeni aday üretilmelidir.

BrowserStack B01–B22 **22 NOT_RUN**; gerçek hesapla uçtan uca kabul, launcher/OEM, bağlantı ve belge sağlayıcısı davranışı yeni adayda değerlendirilir. Hukuk **DRAFT**, nihai yayıncı/iletişim/Privacy/onay ve Play internal/closed/prelaunch/Data safety kayıtları tamamlanmamıştır. Faz 9 açık kalır; Faz 2–8 yeniden geliştirilmemiştir. Teknik paket kimliği, API adresleri/protokoller, bağımsız konumlandırma ve ücretsiz/reklamsız ilk sürüm kararı korunur.
