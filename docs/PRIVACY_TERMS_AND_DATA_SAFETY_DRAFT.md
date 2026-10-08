# Taslak — Privacy Policy / Kullanım Şartları / Data Safety giriş haritası

> **YAYINLANMAZ TASLAK.** Bu doküman kullanıcıya veya Play Store'a sunulan nihai hukuk metni değildir. Gerçek geliştirici adı, iletişim, ülke, URL ve gerçek ağ/SDK veri davranışı netleşmeden yayınlanmamalı.

## 1. Privacy Policy — çerçeve

**Başlık:** [FINAL APP NAME] Privacy Policy (**English only — approved initial document language**)

**Geliştirici / sorumlu:** [DOĞRULANMIŞ GERÇEK GELİŞTİRİCİ ADI]  
**İletişim ve talepler:** [GERÇEK DESTEK E-POSTASI/URL]  
**Yürürlük tarihi:** [RELEASE TARİHİ]

**Hizmetin niteliği.** Uygulama, NextDNS hesabını yönetmek için geliştirilmiş bağımsız Android istemcisidir. NextDNS'in resmî ürünü veya NextDNS tarafından onaylanan/işletilen bir uygulama olduğu iddia edilmez. Bu açıklama marka ve API kullanım izni gerekliliğini ortadan kaldırmaz.

**İşlenen veriler.** Kullanıcı kendi NextDNS API anahtarını sağlar. Uygulama, kullanıcının seçili NextDNS profillerini, yapılandırma ayarlarını, DNS sorgu kayıtlarını ve analizlerini göstermek/değiştirmek için NextDNS API'ye gerekli istekleri yapar. Profil adı, domain kayıtları, IP/cihaz bilgisi ve analizler kullanıcının NextDNS hesabındaki mevcut veriler olarak alınabilir. İzinle etkinleştirilen bildirimler için ilgili yapılandırma özeti ve gerekli yerel durum bilgisi işlenir.

**Verinin nerede işlendiği.** API anahtarı cihazda Android Keystore/AES-GCM ile korunur ve kullanıcının isteği doğrultusunda NextDNS API sunucularına HTTPS üzerinden gönderilir. Gerçek trafik ve üçüncü taraf servis listesi yayın öncesi ağ testiyle doğrulanmalıdır. “Hiç veri toplanmıyor” veya “hiç veri paylaşılmıyor” gibi kategorik iddialar test edilmeden kullanılmaz.

**Günlükler ve dışa aktarma.** Uygulama DNS sorgu geçmişini ekran üzerinde işler; kullanıcı log indirme işlemini başlatırsa dosya kendi tercih ettiği konuma yazılabilir. NextDNS'in **sunucudaki** sorgu saklama politikası kullanıcı hesabındaki tercihlerine bağlıdır ve NextDNS'in ayrı gizlilik politikasına tabidir (https://nextdns.io/privacy). Uygulama yerel cache davranışı ve dosya dışa aktarımını ayrıca açıklar.

**Bildirimler ve arka plan.** Android 13+ bildirim izni kullanıcının isteği üzerine sorulur. Varsayılan durumda isteğe bağlı değişiklik/özet bildirimleri kapalıdır. Etkinleştirilirse WorkManager yaklaşık 30 dakikalık aralıklarla, Android tarafından ertelenebilir biçimde gerekli NextDNS API kontrolü gerçekleştirebilir. Hassas domainler kilit ekranı bildiriminde gösterilmez.

**Saklama ve silme.** Yerel API anahtarının ve uygulama içi ilgili verilerin silme/logout akışı kontrol edilecek ve kullanıcıya açıklanacaktır. NextDNS hesabını kapatma/sunucu verilerini silme işlemi uygulamanın yerel temizliğinden farklıdır. Kullanıcının hangi veri için hangi sağlayıcıya başvuracağı açık yazılmalıdır.

**Reklam ve satın alma.** İlk üretim sürümünde reklam veya reklam kaldırma satın alması olmayacak. Daha sonra reklam/Billing eklenirse politika ve Data Safety tekrar güncellenip gerekli izin/onay süreçleri işletilmeden sürüm yayımlanmayacak.

**Kullanıcı hakları ve iletişim.** [UYGULANABİLİR MEVZUAT/TALEP KANALI — gerçek yetki ve ülke doğrulandıktan sonra doldurulacak].

## 2. Data Safety için kontrol tablosu (BEYAN DEĞİL)

| Veri / yetki | Kaynak | Kullanım | Yayın öncesi inceleme |
|---|---|---|---|
| NextDNS API key | Kullanıcı | NextDNS API'ye kimlik doğrulama | Cihaz/NextDNS aktarımı, diğer sunuculara sızıntı kontrol |
| Profil bilgileri, izin/engelleme kuralları | NextDNS API | Ayar yönetimi | Local cache ve transfer |
| DNS sorguları, domain/IP/cihaz, analizler | NextDNS API | Logs, Analytics, export | Cihaz/bulut saklama, dosya dışa aktarma |
| Bildirim tercihi/hash/son zamanlar | Cihaz | Opt-in arka plan uyarıları | Silme, export, backup |
| Network state | Android | Retry, WorkManager kısıtları | İşlev/izin açıklaması |
| POST_NOTIFICATIONS | Android 13+ | Kullanıcının seçtiği bildirim | Runtime isteme/ret geri alma |
| Reklam/Billing SDK | İlk sürümde planlanmadı | Yok | Final resolved dependencies/manifest doğrula |

Google Play'in Data Safety beyanı, **uygulamanın gerçekten topladığı ve paylaştığı** veri akışına göre doldurulmalıdır; NextDNS'e kullanıcı isteğiyle gönderilen veriyle uygulama sahibine gönderilen veri aynı kategoride otomatik değerlendirilmemeli. Resmî kaynak: https://support.google.com/googleplay/android-developer/answer/18258653?hl=en

## 3. Kullanım Şartları — taslak başlıkları

1. Bağımsız uygulama niteliği / NextDNS ile resmî bağlantı iddiası yok (izin sonrasında gerekirse tam onaylı dil).
2. Kullanıcı için uygulamaya özgü **kendi hesabı** şartı getirilmez; kullanıcı NextDNS hizmet koşullarına ve yürürlükteki hukuka uymakla yükümlüdür. Başkalarının hesaplarına yetkisiz erişim meşru kabul edilmez. Abonelik NextDNS üzerinden yönetilir.
3. NextDNS servisinin müsaitliği/API değişiklikleri uygulamanın kontrolü dışındadır; sunucu sonuçlarının doğrulanması esastır.
4. Geçerli hukuk çerçevesinde hata bildirimi, sürüm desteği ve sorumluluk koşulları.
5. Yerel veri temizliği, API key iptali, NextDNS sunucu verisi sahipliği.
6. İletişim, yürürlük ve değişiklik duyuruları.
7. Reklam ve satın alma ilk sürümde yok; sonradan eklenirse açıkça güncellenecek.

## 4. Altı kesinleşmiş ürün kararı (8 Ekim 2026)

- Kullanım Koşullarının ilk açılışta **işaretlenmeyen zorunlu kabul kutusu** ile açık kabulü; belgelerin okunabilmesi için bağlantılar.
- **Önemli Terms değişikliklerinde** yeniden kabul; önemsiz yazım değişikliklerinde gerekmez.
- Genel kullanıcı kitlesi, çocukları özellikle hedeflemez.
- NextDNS hizmet koşullarına ve hukuka uyulmasını şart koşar; ayrıca yalnız kendi hesabına erişim kısıtı koymaz.
- Logout: API key, yerel hesap/önbellek ve bildirim ayarları/görevleri silinir; tema korunabilir. NextDNS sunucu verileri ve dışa aktarılmış kullanıcı dosyaları etkilenmez.
- **Güncellenen dil kararı (8 Ekim 2026):** Terms of Use beş dilde (Türkçe, İngilizce, Almanca, Fransızca, İspanyolca) hazırlanacak; Privacy Policy tam metni **şimdilik yalnızca İngilizce**. Yerel mevzuatın ayrıca bilgilendirme/yerel dil gereklilikleri yayın kapısıdır. Ayrıntılar: [PHASE9_LOCALIZATION_PLAN.md](PHASE9_LOCALIZATION_PLAN.md).
- **Ayrıntılı teknik plan ve kabul testleri:** [TERMS_PRIVACY_IMPLEMENTATION_PLAN.md](TERMS_PRIVACY_IMPLEMENTATION_PLAN.md).

## 5. Açık yayın maddeleri

- [ ] Geliştirici adı, destek e-postası ve privacy URL doğrulandı (Play listede geliştirici/uygulama adı eşleşiyor).
- [ ] Gerçek uygulama mağaza başlığı ve logo/NextDNS izni çözülmüş.
- [ ] Final APK üzerindeki network trace ve dependency manifest incelendi.
- [ ] Logout, backup ve dışa aktarılan dosyaların gerçek saklama/silme senaryoları test edildi.
- [ ] Nihai Privacy Policy uygulama içinde gösteriliyor/linkleniyor ve kamuya açık HTTPS adresinde barındırılıyor.
- [ ] Data Safety + ads beyanları hukuki metin ve APK ile tutarlı.
- [ ] Kullanım Şartları, geçerli hukuka ve hedef dağıtım bölgelerine göre gerçek geliştirici tarafından gözden geçirildi.
