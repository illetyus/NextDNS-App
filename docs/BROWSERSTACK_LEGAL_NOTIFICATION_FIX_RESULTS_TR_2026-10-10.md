# Hukuk ekranı ve bildirim testi düzeltmeleri — 10 Ekim 2026

NDNS-011 ve Q-005 için hedefli tekrarlar yeni CI adayının imzalı release paketi üzerinde başarılıdır. Bu sonuç Faz 9 veya bütün B01–B22 kabulünün tamamlandığı anlamına gelmez.

Kaynak CI: [çalışma 38082499286](https://github.com/illetyus/open-source-client-for-nextdns/actions/runs/38082499286). Aday commit: `4bfda98e0028628982f920be03029c073e76f67d`. APK SHA-256: `ccb4e8d5a16160ce0bd344f7099739e83d901273a8c34a34174bd8672a2b454f`; test APK SHA-256: `bca6562db90d42ef533f9987a40dc1609ed10070103fbb33e946880a3d5148fe`. Uygulama ve test APK imzaları yükleme öncesinde aynı sertifikayla doğrulanmıştır; bu geçici CI sertifikası üretim imzası onayı değildir.

Başlangıç regresyonu [CI 38080658261](https://github.com/illetyus/open-source-client-for-nextdns/actions/runs/38080658261) üzerinde eski yerleşimde **0 dp belge yüksekliği** ile başarısız oldu. Sonraki yerleşim, belge alanına en az 160 dp yükseklik verir ve tüm sayfayı kaydırılabilir tutar. Beş dilde arayüz/sözleşme, 2× yazı boyutu, gerçek dikey/yatay döndürme, çevrimdışı belge erişimi, işaretsiz varsayılan seçim ve seçim yapılmadan devre dışı kalan devam düğmesi cihazda denetlendi. Mevcut ayrı Privacy belgesi İngilizcedir; beş ayrı Privacy çevirisinin doğrulandığı iddia edilmez. Ayrı gezinme testi açık seçim sonrası misafir akışını doğruladı.

Bildirim izni reddedildiğinde hem yapılandırma hem günlük özet gönderimi engellenir. İzin verildiğinde Android'e ulaşan iki gerçek Notification nesnesi PRIVATE görünürlük ve genel publicVersion bakımından denetlendi. Genel görünüm sentetik sorgu sayılarını taşımaz. Sistem kanalının kilit ekranı görünürlüğü uygulama garantisi olarak kullanılmaz; kullanıcı/sistem ayarlarına ilişkin tam B09 incelemesi ayrı kalır. [Android kaynak belgesi](https://developer.android.com/reference/android/app/NotificationChannel#setLockscreenVisibility(int)).

BrowserStack çalışma kimliği: `d26b7608d1989476f8df332cb258a2131e795228`. API sayaçları kalıcı JUnit raporlarıyla eşleştirilmiştir.

İlk tam release tekrarı (`75dab76179c601b8506cebb9f88a6cd9f1583fa0`) **56 başarılı, 6 başarısız, 2 uygulanamaz** sonucu verdi. Dört misafir akışı, test APK'sından yüklenen `j$.time.Instant` sınıfındaki eksik `parse` metodu nedeniyle durdu; Android 13/16 pozitif bildirim yöntemleri ayrıca süreç çökmesi bildirdi. Test koduna UTC/ofset/yerel ISO API ve gerçek log biçimlendirme kontrolleri eklendi; test sonunda izni geri alarak süreci sonlandırma riski giderildi. Q-006 ve ilk JUnit kayıtları korunur. Uygulamanın tarih/protokol kodu veya ağ güvenliği değiştirilmedi.

İkinci tekrar (`2046297ec2e91b098b1ba8c9f21aa69ec77eb491`) **60 başarılı, 2 başarısız, 2 uygulanamaz** sonucu verdi. Misafir akışı ve gerçek PRIVATE/publicVersion kontrolleri geçti; Android 13/16'da izin verilmiş testten kalan durum, sonraki izin geri alma işlemi sırasında test sürecini sonlandırdı. Son tekrar aynı yüklenmiş APK çiftiyle `clearPackageData=true` kullanır. Bu ayar geçici uygulama verisini her yöntemden sonra temizler; iki izin yolu bağımsız çalıştırılır. Q-007, yürütüm sırası ve başarısız raporlar korunur. [BrowserStack resmî veri temizleme belgesi](https://www.browserstack.com/docs/app-automate/espresso/clear-app-data).

| Gerçek cihaz | Android | Başarılı | Başarısız | OS nedeniyle uygulanamaz |
|---|---|---:|---:|---:|
| Samsung Galaxy S20 | 10.0 | 15 | 0 | 1 |
| Samsung Galaxy Tab S8 | 12.0 | 15 | 0 | 1 |
| Google Pixel 7 | 13.0 | 16 | 0 | 0 |
| Samsung Galaxy S26 | 16.0 | 16 | 0 | 0 |

Toplam **62 başarılı, 0 başarısız, 2 uygulanamaz** yöntem. İki uygulanamaz yöntem, Android 10/12 üzerinde Android 13 bildirim izni senaryosudur. Release güvenlik tekrarları ayrıca **40/40 başarılı**, sıfır atlama sonucunu verir. [Commit/hash ve yöntem kaydı](release/browserstack-legal-notification-fix-results-2026-10-10.json) ve [hata defteri](release/quality-ledger.json) kalıcı kanıttır.

Önceki başarısız raporlar tarihsel kanıt olarak korunur. Aynı Pixel 7 için App Live deneme erişimi artık kapalıdır; döndürme ve büyük yazı tekrarı aynı modelde App Automate üzerinden yapılmıştır. Pixel 7 Pro üzerinde App Live yeniden denendi; tarayıcı denetim gecikmeleri nedeniyle başlangıç denemesi tam bir manuel kabul olarak kaydedilmez. Doğrulanan ek manuel gözlemler ayrı kaydedilir; otomasyon manuel test olarak sunulmaz.

Kalanlar: NDNS-001–010 için kendi cihaz kapanış senaryoları; gerçek test hesabında API/profil/SSE/lifecycle/WorkManager akışları; API 24/25, katlanabilir, TalkBack ve diğer B01–B22 kontrolleri. Hukuki metinler DRAFT kalır. Nihai yayıncı/hukuk/lisans ve üretim imzası incelemesi ile Play test ve Data Safety kayıtları tamamlanmamıştır. Özgün ağ düğümü simgesi kaynakta uygulanmış olsa da launcher/temalı ikon, mağaza sunumu ve marka incelemesi yayın kabulünde açık kalır.
