# BrowserStack ilk gerçek cihaz gözlemleri — 10 Ekim 2026

**Sonuç: Kısmi test; tam yayın kabulü yok.** Pixel 7 yatay hukuk ekranında `NDNS-011` hatası yeniden üretildi. Önceden düzeltilmiş 10 bulgunun cihaz kabulü kapatılmadı.

## Aday

- Kaynak commit: `d63aea420207b8aacf7eb7866820bd173c42a519`
- Kaynak CI: [38065297405](https://github.com/illetyus/open-source-client-for-nextdns/actions/runs/38065297405)
- APK: `OpenSourceClientForNextDNS-1.0-review.apk`, sürüm `1.0 / 1`
- Uygulama kimliği: `com.aistudio.nextdns.mgrqvt`
- Yükleme öncesi SHA-256: `80503ac215330cc26140b7f7252574b94850dd10ba0959164351f9f548e9a3d2`

App Live yükleme listesinde APK adı ve sürümü doğrulandı. BrowserStack tarafından yeniden imzalanmış olabilecek kurulu paketin hash'i ayrıca alınmadı; yükleme öncesi hash kurulu paketin hash'i olarak sunulmaz.

## Yürütülen denemeler

| Gerçek cihaz | Android | Doğrulanan gözlem | Sınır / sonuç |
|---|---|---|---|
| Google Pixel 7 | 13 | Kurulum ve ilk açılış; boş kabul kutusu ve kapalı devam düğmesi; beş dilde başlangıç ekranı ve Terms; İspanyolca ekrandan İngilizce Privacy açılması | Yatay pencerede hukuk metni ve devam kontrolüne erişilemiyor; `B11 FAIL` |
| Samsung Galaxy Tab S8 | 12 | Dikey başlangıç ekranı, okunabilir Terms ve Privacy, boş kutu ve kapalı devam düğmesi; BrowserStack Screen Reader kontrolünün etkinleşmesi | Sesli çıktı ve tam TalkBack gezinmesi doğrulanmadı; tablet yatay düzeni test edilmedi |
| Samsung Galaxy Z Fold 5 | 13 | Kapalı cihazda kurulum ve başlangıç ekranı, boş kabul kutusu ve kapalı devam düğmesi | Açma komutu verildi; geniş ekranın sonucu görünmeden ücretsiz cihaz süresi doldu |

Hukuki kabul gönderilmedi. Gerçek NextDNS hesabı, profil değişikliği veya gerçek API anahtarı kullanılmadı. `DRAFT` işaretleri ve bağımsız/gayriresmî açıklamalar korundu.

## NDNS-011 — Yatay hukuk ekranında metin ve devam alanı kayboluyor

Pixel 7 / Android 13 üzerinde, kabul verilmeden Privacy görüntülendi ve cihaz yatay konuma çevrildi. Belge alanı görünmez oldu; devam düğmesi ekran dışında kaldı. Sayfayı yukarı kaydırma denemesi bunlara erişim sağlamadı.

Kaynakta `LegalWelcomeScreen` dış sütunu kaydırılamıyor; sabit kontroller arasında kalan belge alanı `weight(1f)` kullanıyor. Kısa pencerede kullanılabilir yükseklik tükeniyor. Düzeltme için kısa pencere ve büyük yazı boyutlarında okunabilir belge alanı ile erişilebilir kontroller sağlanmalı; başlangıçtaki boş kutu ve kapalı devam düğmesi korunmalı. Yeni imzalı adayda yeniden test gereklidir.

## Kanıt ve tamamlanmamış kontroller

Gözlemler canlı BrowserStack arayüzü, cihaz görüntüleri ve görünür Logcat satırlarından alındı. Görüntüler araç çıktılarında incelendi; kalıcı ekran görüntüsü dışa aktarımı tamamlanmadı. BrowserStack session ID, tam log paketi ve video elde edilmedi. Bu nedenle kayıt tam kabul kanıtı olarak kullanılamaz.

App Automate panelinde 100 ücretsiz dakika görünüyordu. App Live cihaz süreleri ayrıydı: Pixel 7 için 5, Tab S8 için 2, Fold 5 için 1 dakikalık etkin oturum sınırları görüldü. Fold 5 denemesi süre dolumuyla kapandı. Yeni Android 15/16 cihazları hesap kataloğunda devre dışı görünüyordu; API 24/25 erişimi doğrulanmadı.

Espresso için yerel `BROWSERSTACK_USERNAME` ve `BROWSERSTACK_ACCESS_KEY` bulunmadığından otomatik build gönderilmedi. Tarayıcı girişi otomasyon anahtarı yapılandırması yerine geçmez. Anahtarlar raporlara veya Git'e yazılmamalı.

Görünür platform/vendor tanıları (`jdwp agent`, `OnBackInvokedCallback`, `QSPM HAL`, `libpenguin.so`) uygulama çökmesi olarak sınıflandırılmadı. Tam Crash/ANR kabulü, bildirimler, canlı API mutasyonları, lifecycle, veri temizleme ve erişilebilirlik incelemesi tamamlanmadı.

`B01`, `B12`, `B14`, `B17`, `B18`, `B20` yalnız **PARTIAL**; `B11` **FAIL**; diğer vakalar **NOT_RUN**. Hiçbir bütün B senaryosu **PASS** olarak işaretlenmedi. Faz 9 ve yayın kapısı açık kalır.

Makine kaydı: [browserstack-initial-results-2026-10-10.json](release/browserstack-initial-results-2026-10-10.json). Hata defteri: [quality-ledger.json](release/quality-ledger.json).
