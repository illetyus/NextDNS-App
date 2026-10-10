# Hata defteri — yayın öncesi kod incelemesi

İnceleme tarihi: **10 Ekim 2026**. Kaynak: `4abf26a1a556cf42235fbf05c70f89cb5ad79e8c`. Bu dosya ve [makinece okunabilir kayıt](release/quality-ledger.json) aynı bulguları taşır.

**10 açık kod kusuru: 3 P1, 7 P2.** Ayrıca 3 kalite/süreç gözlemi vardır. Bu incelemede P0 düzeyinde bir kusur için kanıt elde edilmemiştir; bu ifade kusursuzluk veya tam güvenlik garantisi değildir.

`SOURCE_CONFIRMED` kaynak kodda koşullu hata akışının doğrulandığını belirtir. Buradaki tekrar üretim senaryoları henüz Android/BrowserStack üzerinde çalıştırılmış sonuç değildir. B01–B22 kayıtları **22 NOT_RUN** olarak kalır. [İnceleme raporu](CODE_QUALITY_REVIEW_TR_2026-10-10.md) yöntem ve kapsam sınırını açıklar.

P1, son kabul aşamasından önce düzeltilmesi gereken yüksek öncelik; P2, belirli akışlarda yanlış davranış/çökme/erişilebilirlik veya kaynak yönetimi kusuru; P3, bakım önerisi anlamındadır. Bir hata, düzeltme commit’i ve saklanan regresyon/tekrar test kanıtı olmadan kapatılmaz.

| Kayıt | Öncelik | Bulgu | Durum | İlgili cihaz vakaları |
|---|---|---|---|---|
| NDNS-001 | P1 | Geciken API yanıtı eski hesap/profil verisini yeniden uygulayabilir | AÇIK; kaynakta doğrulandı | B08, B13, B21 |
| NDNS-002 | P1 | A profiline gönderilen mutasyon B profili okunarak doğrulanabilir | AÇIK; kaynakta doğrulandı | B04, B08, B19, B21 |
| NDNS-003 | P1 | Başarısız veya başka profile ait DNS ölçümü olumlu koruma durumu gösterir | AÇIK; kaynakta doğrulandı | B02, B05, B08, B19 |
| NDNS-004 | P2 | Web3 değeri sunucudan yerel ayar modeline aktarılmıyor | AÇIK; kaynakta doğrulandı | B04, B15, B19 |
| NDNS-005 | P2 | Kurulum ekranının tanılama döngüsü arka planda da çalışır | AÇIK; kaynakta doğrulandı | B05, B08, B16 |
| NDNS-006 | P2 | Coroutine iptali senkron OkHttp çağrısını kapatmıyor | AÇIK; kaynakta doğrulandı | B06, B08, B13, B21 |
| NDNS-007 | P2 | IP bağlama ve katalog hata yollarında Response kaynakları açık kalabilir | AÇIK; kaynakta doğrulandı | B03, B05, B19 |
| NDNS-008 | P2 | Senkronizasyon bayrakları iptal ve eşzamanlı güncellemede tutarsızlaşabilir | AÇIK; kaynakta doğrulandı | B05, B08, B16 |
| NDNS-009 | P2 | Günlük cihaz menüsünde Tüm cihazlar etiketi çevrilmiyor | AÇIK; kaynakta doğrulandı | B12, B17 |
| NDNS-010 | P2 | Bozuk export URL veya çıktı açma hatası kontrolsüz istisna oluşturabilir | AÇIK; kaynakta doğrulandı | B07, B16, B19 |

## Kod kusurları

### NDNS-001 — P1: Geciken API yanıtı eski hesap/profil verisini yeniden uygulayabilir

**Durum:** AÇIK. Runtime tekrar üretimi: çalıştırılmadı. Düzeltme commit’i: yok.

**Kaynak:** [NextDnsRepository.kt:1724–1747](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L1724-L1747); [NextDnsRepository.kt:1199–1215](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L1199-L1215); [NextDnsRepository.kt:1125–1152](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L1125-L1152); [NextDnsViewModel.kt:316–332](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/ui/viewmodel/NextDnsViewModel.kt#L316-L332).

**Tetikleyici:** A profilinde manuel log yenileme veya profil oluşturma sürerken B profiline geç ya da çıkış yap; A isteğini daha sonra tamamlat.

**Kaynakta görülen davranış:** Yanıt sonrası mevcut API anahtarı/profil/oturum nesli kontrol edilmeden StateFlow ve preferences yazılıyor. ViewModel kapsamındaki bu işler repository kapsamının cancelChildren çağrısıyla iptal edilmiyor. loadLocalProfileData geçmişi temizler; eksik olan geç yanıtı reddetme kontrolüdür.

**Düzeltme yönü:** Hesap ve profil bağlamına ait bir nesil/snapshot doğrulamasıyla eski cevapların UI ve diske yazılmasını engelle; oturuma ait işlerin kapsamını açıkça yönet.

**Kapanış ölçütü:** Geciktirilmiş A yanıtı B ekranına taşınmaz; çıkıştan sonra prefs/profil listesi yeniden oluşmaz; yeniden girişte önceki hesabın cache verisi gösterilmez.

**Cihaz tekrar testi:** B08, B13, B21.

### NDNS-002 — P1: A profiline gönderilen mutasyon B profili okunarak doğrulanabilir

**Durum:** AÇIK. Runtime tekrar üretimi: çalıştırılmadı. Düzeltme commit’i: yok.

**Kaynak:** [NextDnsRepository.kt:197–205](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L197-L205); [NextDnsRepository.kt:242–275](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L242-L275); [NextDnsRepository.kt:152–170](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L152-L170).

**Tetikleyici:** A için PATCH/POST gönder; yanıt beklerken B profiline geç; A yazma yanıtını tamamlat.

**Kaynakta görülen davranış:** mutateSection ilk key/profileId çiftini action için yakalıyor fakat refreshSection yeni aktif key/profileId çiftini yeniden okuyor. B GET başarılı olursa A işlemi doğrulanmış sayılabiliyor.

**Düzeltme yönü:** Yazma ve doğrulama GET isteklerini aynı oturum/profil snapshotına bağla; hedef durumun uygulandığını doğrula ve profil değişmişse yeni ekrana eski işlem başarısı uygulama.

**Kapanış ölçütü:** İstek kaydında PATCH ve doğrulama GET aynı hedefe gider; profil/hesap değişimi doğrulamayı geçersiz kılar; başka profil GET başarısı yazma başarısı sayılmaz.

**Cihaz tekrar testi:** B04, B08, B19, B21.

### NDNS-003 — P1: Başarısız veya başka profile ait DNS ölçümü olumlu koruma durumu gösterir

**Durum:** AÇIK. Runtime tekrar üretimi: çalıştırılmadı. Düzeltme commit’i: yok.

**Kaynak:** [NextDnsRepository.kt:498–506](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L498-L506); [SetupScreen.kt:148–187](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/ui/screens/SetupScreen.kt#L148-L187).

**Tetikleyici:** Önce status=ok sonucu al, sonraki ölçümün iki yolunu da başarısız kıl. Ayrı durumda seçili A profili varken ölçüm profileId=B ve status=ok dönsün.

**Kaynakta görülen davranış:** Hata yolu önceki status/protocol/profileId değerlerini copy ile koruyor. Banner hata mesajı varken ok durumuna öncelik veriyor ve activeProfile ile ölçülen profileId değerini karşılaştırmıyor; bu profille kullanılıyor alt yazısı seçili profil için yanlış olabiliyor.

**Düzeltme yönü:** Son ölçüm başarısını, güncelliği ve ölçülen profil eşleşmesini ayrı durumlarla göster; eski ölçümü yeni başarı gibi işaretleme.

**Kapanış ölçütü:** ok→timeout yolu güncel başarı göstermez; A seçili/B ölçülü durum profil uyuşmazlığı gösterir; son başarılı ölçüm zamanı yeni başarısız ölçüm zamanı ile değiştirilmez.

**Cihaz tekrar testi:** B02, B05, B08, B19.

### NDNS-004 — P2: Web3 değeri sunucudan yerel ayar modeline aktarılmıyor

**Durum:** AÇIK. Runtime tekrar üretimi: çalıştırılmadı. Düzeltme commit’i: yok.

**Kaynak:** [NextDnsApiService.kt:262–267](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/api/NextDnsApiService.kt#L262-L267); [NextDnsRepository.kt:1023–1035](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L1023-L1035); [NextDnsRepository.kt:1605–1613](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L1605-L1613); [SettingsScreen.kt:221–225](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/ui/screens/SettingsScreen.kt#L221-L225).

**Tetikleyici:** GET settings yanıtı web3=true dönsün veya Web3 açma PATCH işlemini başarılı kıl.

**Kaynakta görülen davranış:** SettingsDto.web3 var; applyConfigSettingsFromApi copy çağrısında s.web3 yok. Varsayılan false korunur ve ekrandaki switch false kalır; yerel cache de eski değeri saklar.

**Düzeltme yönü:** Web3 alanını yetkili sunucu yanıtından modele ve cache içine aktar; true, false ve eksik alan davranışını açıkça sınayarak doğrula.

**Kapanış ölçütü:** GET true/false ekranda ve cache içinde eşleşir; aç/kapat işlemi ve yeniden açılış aynı sunucu durumunu gösterir.

**Cihaz tekrar testi:** B04, B15, B19.

### NDNS-005 — P2: Kurulum ekranının tanılama döngüsü arka planda da çalışır

**Durum:** AÇIK. Runtime tekrar üretimi: çalıştırılmadı. Düzeltme commit’i: yok.

**Kaynak:** [SetupScreen.kt:64–73](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/ui/screens/SetupScreen.kt#L64-L73); [NextDnsViewModel.kt:603–608](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/ui/viewmodel/NextDnsViewModel.kt#L603-L608); [HomeScreen.kt:70–87](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/ui/screens/HomeScreen.kt#L70-L87).

**Tetikleyici:** Kurulum ekranı açıkken uygulamayı arka plana al ve 15 saniyeden uzun bekle.

**Kaynakta görülen davranış:** LaunchedEffect(Unit) bileşimde kaldığı sürece her 15 saniyede ViewModel işi başlatıyor; yaşam döngüsü RESUMED kontrolü yok. HomeScreen yalnız profil/sekme sorgularını durduruyor.

**Düzeltme yönü:** Tanılama üreticisini RESUMED yaşam döngüsüne bağla; başlatılan tanılama işini de aynı kapsama bağlayıp tek aktif isteği koru.

**Kapanış ölçütü:** Arka planda yeni tanılama isteği oluşmaz; ön plana dönüşte tek döngü çalışır; ekran ve Activity kapanışı işi sonlandırır.

**Cihaz tekrar testi:** B05, B08, B16.

### NDNS-006 — P2: Coroutine iptali senkron OkHttp çağrısını kapatmıyor

**Durum:** AÇIK. Runtime tekrar üretimi: çalıştırılmadı. Düzeltme commit’i: yok.

**Kaynak:** [NextDnsRepository.kt:1814–1826](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L1814-L1826); [NextDnsRepository.kt:1925–1928](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L1925-L1928); [NextDnsRepository.kt:1672–1689](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L1672-L1689); [NextDnsApiService.kt:948–963](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/api/NextDnsApiService.kt#L948-L963).

**Tetikleyici:** SSE veya export yanıtını açık/bekleyen durumda tut; profil değiştir, çıkış yap veya ilgili ekranı kapat.

**Kaynakta görülen davranış:** Call.execute/read işlemi bloklayıcı; Call nesnesi saklanmıyor ve Job iptalinde Call.cancel çağrılmıyor. stopLogsStream yalnız Job.cancel kullanır. İşin iptal edilmesi açık ağ aktarımının anında durduğunu kanıtlamaz.

**Düzeltme yönü:** Çağrı iptalini coroutine iptaline bağla; response/body kapatmayı finally/use ile garanti et; sonuç yazımından önce oturum kontrolü yap.

**Kapanış ölçütü:** Bekleyen fixture çağrısının iptal edildiği ve response kaynaklarının kapandığı ölçülür; eski iş tekrar bağlanmaz veya veri yazmaz.

**Cihaz tekrar testi:** B06, B08, B13, B21.

### NDNS-007 — P2: IP bağlama ve katalog hata yollarında Response kaynakları açık kalabilir

**Durum:** AÇIK. Runtime tekrar üretimi: çalıştırılmadı. Düzeltme commit’i: yok.

**Kaynak:** [NextDnsApiService.kt:979–988](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/api/NextDnsApiService.kt#L979-L988); [NextDnsApiService.kt:999–1006](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/api/NextDnsApiService.kt#L999-L1006); [NextDnsApiService.kt:1032–1041](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/api/NextDnsApiService.kt#L1032-L1041).

**Tetikleyici:** IP bağlamayı tekrar çalıştır; katalog fallback isteğine body içeren non-2xx yanıtlar döndür.

**Kaynakta görülen davranış:** linkIpAddress yalnız isSuccessful okur; body tüketilmez ve response kapatılmaz. Katalog fallback erken non-2xx dönüşleri de body tüketilmeden gerçekleşir. Başarılı body.string yolları zaten kapanır ve bu bulguya dahil değildir.

**Düzeltme yönü:** Bütün senkron Response sahipliğini use kapsamına al; erken dönüş/istisnalarda da body kapansın.

**Kapanış ölçütü:** Başarı, non-2xx ve parse hatası yollarında her response kapatılır; tekrarlı fixture çağrılarında açık kaynak birikmez.

**Cihaz tekrar testi:** B03, B05, B19.

### NDNS-008 — P2: Senkronizasyon bayrakları iptal ve eşzamanlı güncellemede tutarsızlaşabilir

**Durum:** AÇIK. Runtime tekrar üretimi: çalıştırılmadı. Düzeltme commit’i: yok.

**Kaynak:** [NextDnsRepository.kt:143–150](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L143-L150); [NextDnsRepository.kt:157–180](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L157-L180); [HomeScreen.kt:454–457](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/ui/screens/HomeScreen.kt#L454-L457).

**Tetikleyici:** refreshSection isRefreshing=true olduktan sonra sekme sorgusunu iptal et. Ayrı durumda farklı section güncellemelerini eşzamanlı tamamlat.

**Kaynakta görülen davranış:** CancellationException güncel bayrak temizlenmeden yeniden fırlatılır; temizleme finally içinde değil. Harita read/value→copy→write ile güncellenir; eşzamanlı değişiklikler birbirini kaybedebilir.

**Düzeltme yönü:** İstek kimliğine bağlı finally temizliği ve atomik StateFlow.update kullan; eski isteğin yeni isteğin bayrağını temizlemesine izin verme.

**Kapanış ölçütü:** İptalde eski isRefreshing kalmaz; iki section değişikliği korunur; ardışık eski/yeni istek tamamlanması yeni isteğin durumunu bozmaz.

**Cihaz tekrar testi:** B05, B08, B16.

### NDNS-009 — P2: Günlük cihaz menüsünde Tüm cihazlar etiketi çevrilmiyor

**Durum:** AÇIK. Runtime tekrar üretimi: çalıştırılmadı. Düzeltme commit’i: yok.

**Kaynak:** [LogsScreen.kt:118–120](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/ui/screens/LogsScreen.kt#L118-L120); [LogsScreen.kt:140–142](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/ui/screens/LogsScreen.kt#L140-L142); [LogsScreen.kt:270–271](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/ui/screens/LogsScreen.kt#L270-L271); [LogsScreen.kt:292–299](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/ui/screens/LogsScreen.kt#L292-L299).

**Tetikleyici:** EN/DE/FR/ES seç; Logs cihaz filtresi menüsünü aç.

**Kaynakta görülen davranış:** Seçili başlık UiLabels.canonical ile çevrilir fakat menü Text(text=dev) kullanır. Listeye eklenen Türkçe Tüm cihazlar sentinel değeri menüde aynen görünür; statik kaynak eşitliği kapısı bunu yakalamaz.

**Düzeltme yönü:** Sabit sentinel etiketlerini menüde de çevir; gerçek sunucu cihaz adlarını aynen koru.

**Kapanış ölçütü:** Beş dilde başlık ve menü etiketi aynı anlamda çevrilir; kullanıcı cihaz adları çeviri yüzünden değiştirilmez.

**Cihaz tekrar testi:** B12, B17.

### NDNS-010 — P2: Bozuk export URL veya çıktı açma hatası kontrolsüz istisna oluşturabilir

**Durum:** AÇIK. Runtime tekrar üretimi: çalıştırılmadı. Düzeltme commit’i: yok.

**Kaynak:** [NextDnsRepository.kt:1615–1618](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L1615-L1618); [NextDnsRepository.kt:1662–1673](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/data/repository/NextDnsRepository.kt#L1662-L1673); [SettingsScreen.kt:63–73](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/ui/screens/SettingsScreen.kt#L63-L73); [NextDnsViewModel.kt:678–684](https://github.com/illetyus/open-source-client-for-nextdns/blob/4abf26a1a556cf42235fbf05c70f89cb5ad79e8c/app/src/main/java/com/example/ui/viewmodel/NextDnsViewModel.kt#L678-L684).

**Tetikleyici:** Export link yanıtında https:// gibi geçersiz ama prefix kontrolünü geçen bir değer kullan. Ayrı durumda belge sağlayıcısının openOutputStream çağrısını hata ile sonlandır.

**Kaynakta görülen davranış:** Request.Builder.url try bloğundan önce çalışır; geçersiz URL IllegalArgumentException üretir ve Result akışından kaçar. UI openOutputStream çağrısı da try içinde değildir; sağlayıcı istisnası screenScope.launch içinde kontrolsüz kalır.

**Düzeltme yönü:** URLyi HttpUrl ile doğrula, bütün export/sağlayıcı işlemini yönetilen hata akışına al; cancellation istisnasını yeniden fırlat ve kısmi dosyayı başarı sayma.

**Kapanış ölçütü:** Geçersiz URL, silinen/revoked URI, sağlayıcı erişim hatası ve IO hatası kullanıcıya hata gösterir; çökme/başarı bildirimi olmaz; tüm akışlar kapanır.

**Cihaz tekrar testi:** B07, B16, B19.

## Kalite ve süreç gözlemleri

| Kayıt | Öncelik | Gözlem | Durum |
|---|---|---|---|
| Q-001 | P2 | Repository oturum/mutasyon yollarının hedefli regresyon kanıtı eksik | AÇIK |
| Q-002 | P3 | Repository çok sayıda sorumluluğu ve statik API bağımlılığını topluyor | AÇIK |
| Q-003 | P2 | PR/CI süreci uygulanıyor fakat main sunucu tarafında korunmuyor | AÇIK |

**Q-001:** JVM toplam LINE %20.73, BRANCH %10.28; NextDnsRepository sınıfı LINE %7.06. Coroutine/Compose üretilmiş kodu oranları etkiler; API 35 native güvenlik testleri bu JVM raporuna dahil değildir. Kritik bug yolları için mevcut testler başarı kanıtı vermez. Rastgele bir genel yüzde hedefi yerine yukarıdaki kapanış senaryoları gereklidir.

**Q-002:** 2456 satırlık repository ağ, cache, profil, SSE, analytics ve sunum formatlamasını birlikte yönetir. Test edilebilir API/clock/session bağımlılıkları ve küçük saf mapper sınırları bakım işidir; önceki fazları baştan geliştirme gerekçesi değildir.

**Q-003:** GitHub branch protection API main için 404 Branch not protected verdi; etkin branch rules yanıtı boş listedir. Normal PR/CI izlendiği doğrulandı; zorunlu review/check kuralı platform tarafından dayatılmıyor. Bu inceleme depo yönetim ayarlarını değiştirmez.

## Önceki dilimde giderilmiş kayıtlar

| Kayıt | Sorun | Düzeltme ve kanıt | Durum |
|---|---|---|---|
| HIST-01 | Üretim kütüphanesindeki sıradan Robolectric/MockWebServer metinlerini test kodu sayan yanlış pozitif | [PR #18](https://github.com/illetyus/open-source-client-for-nextdns/pull/18): gerçek DEX tür tablosu kontrolü; gerçek test APK ve kesilmiş DEX reddi; başarılı main CI | GİDERİLDİ; mevcut paket kanıtında doğrulandı |
| HIST-02 | Korumalı Windows imzalama klasörüne tekrar ACL yazarken SeSecurityPrivilege hatası | [PR #19](https://github.com/illetyus/open-source-client-for-nextdns/pull/19): tekrar kullanımda ACL doğrulaması; aynı yerel anahtarla imzalı paket kanıtı | GİDERİLDİ; mevcut paket kanıtında doğrulandı |
| HIST-03 | Yerel masaüstü dosya bağlantısının telefondan indirilememesi | Önceki teslim turunda APK hesaba özel Drive dosyası olarak yüklendi; ad/boyut metadata ile doğrulandı. Bu APK inceleme adayıdır; Play yayını değildir. | TESLİM DÜZELTİLDİ; cihaz kurulumu/testi iddia edilmez |

## Kapatma ve son aşama sırası

1. NDNS-001–003 için oturum/profil ayrımı ve bağlantı durumunu düzelt; diğer kayıtları ilgili küçük düzeltme dilimlerine ayır.
2. Her düzeltmeyi hedefli regresyon, PR ve CI kanıtına bağla; mevcut kaydı koruyarak düzeltme commit’i ve tekrar test adresini ekle.
3. Uygulama kodu değiştikten sonra yeni main CI’a bağlı imzalı inceleme adayı üret. Eski APK hash’i yeni kodun kanıtı olamaz.
4. Yeni aday üzerinde ilgili B vakalarını ve B01–B22 kabul matrisini çalıştır. Son yayıncı/metin incelemesi ve Play kanalları bu aşamayı izler.

Mevcut hukuk metinleri **DRAFT** kalır; yayıncı/iletişim/onay bilgisi eklenmez. Bu hata defteri kullanıcıya yeni bir hizmet, SLA veya destek taahhüdü oluşturmaz.
