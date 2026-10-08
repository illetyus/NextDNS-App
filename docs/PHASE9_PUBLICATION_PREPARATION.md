# Faz 9.6–9.7 — gerçek cihaz ve mağaza hazırlığı

**DRAFT / yayımlanmadı.** Beş dilde başlık, kısa ve tam mağaza açıklamaları `fastlane/metadata/android` altında hazırlanır. Başlık 30 karakterdir; kısa açıklamalar 80, tam açıklamalar 4000 karakter sınırında doğrulanır. İlk sürüm ücretsiz/reklamsızdır; Türkiye ve AB seçimi hazırlık kaydıdır, Play Console'a uygulanmış değildir. [Google mağaza alanları](https://support.google.com/googleplay/android-developer/answer/9859152?hl=en).

`docs/release/readiness.json` B01–B22 durumlarını başlangıçta **NOT_RUN** tutar. CI'daki JVM testleri, APK üretimi veya BrowserStack erişim ekranı gerçek cihaz PASS kaydı oluşturmaz. 9 Ekim 2026 oturumunda BrowserStack panosu giriş ekranına yöneldi; bağlı BrowserStack entegrasyonu veya erişim anahtarı doğrulanamadı. Gerçek cihaz stoku/modeli, App Live oturumları ve App Automate build sonuçları henüz yoktur.

Debug APK + androidTest APK hazırlandığında SHA-256 ve commit, gerçek cihaz koşusuna bağlanır. Her B vakası için cihaz/Android sürümü, rapor adresi ve aynı aday hash'i kaydedilir. Canlı API mutasyonları için yalnız ayrı sentetik test hesabı/profili kullanılır; kişisel API anahtarı veya DNS geçmişi kanıt dosyalarına yazılmaz. Mevcut JVM fixture'ları ve tek instrumentation paket testi tüm B01–B22 otomasyonu olarak sunulmaz.

`python3 scripts/verify_publication_ready.py --draft` yalnız hazırlık şemasını denetler. Parametresiz kontrol; DRAFT hukuk, son yayıncı incelemesi, imza/lisans inceleme kaydı, gerçek cihaz sonuçları ve Play internal/closed/prelaunch/Data safety kanıtları tamamlanmadan başarısız olur. Bu script Play'e dosya göndermez veya yayın başlatmaz.

İngilizce Privacy Policy için nihai kamuya açık HTTPS adresi hâlâ doğrulanmamıştır. Data safety formu, üçüncü tarafa gönderilen API istekleri ile yerel verileri son paket trafiğinden ayıran incelemeyle hazırlanacaktır; taslak belgeler otomatik biçimde form beyanına dönüştürülmez. [Google Data safety açıklaması](https://support.google.com/googleplay/android-developer/answer/10787469?hl=en).
