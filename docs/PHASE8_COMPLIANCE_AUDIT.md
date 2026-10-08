# Faz 8 — Hukuk, marka, lisans ve gelir modeli denetimi

**Tarih:** 2026-10-08  
**İncelenen kaynak dalı:** `phase7-ci-quality`, HEAD `16256c86721b5f376dc1a13ec4bc63a22fc0fe5d`  
**Durum:** Araştırma ve risk kaydı tamamlandı; haricî izin, eser kökeni ve yayın belgeleri kesinleşmeden release onayı verilmez.  
**Nitelik:** Teknik uyumluluk denetimi; hukuk mütalaası veya NextDNS tarafından verilmiş izin değildir.

## 1. Kararlar ve engeller

| Konu | Kanıt / durum | Karar | Yayın engeli |
|---|---|---|---|
| NextDNS marka/uygulama adı | `app/src/main/res/values/strings.xml` içindeki `app_name=NextDNS` | Resmî sahiplik izlenimi önlenmeli; yazılı onay veya özgün uygulama adı gerekir | **Evet** |
| Uygulama simgesi | `ic_launcher_foreground.xml` yorumunda `NextDNS Blue Shield Logo` yazıyor; mipmap PNG/WEBP türevleri de mevcut | Lisans/izin doğrulanmalı; aksi halde özgün simgeye geçilip bütün çözünürlükler güncellenmeli | **Evet** |
| NextDNS API | https://nextdns.github.io/api/ API'yi beta olarak tanımlar | Kullanımın teknik olarak mümkün olması marka/ticari kullanım izni sayılamaz; NextDNS'e yazılı kapsamlı soru gönderilecek | **Evet: ticari/marka kararı için** |
| GPL kod kökeni | https://github.com/doubleangels/nextdnsmanager GPL-3.0; ağırlıkla Java+WebView. Bizim kaynak ağaç Kotlin+Compose/API'dir | Mimari fark kod türetimi olmadığını kanıtlamaz. Yazar, önceki kaynaklar, varlıklar ve başlangıç dosyaları tek tek teyit edilecek | **Evet** |
| Kök lisans | `LICENSE` yok | Provenance doğrulanmadan varsayılan Apache-2.0/GPLv3 eklenmeyecek | **Evet** |
| Gelir modeli | Google Play tek seferlik tüketilemeyen ürün olarak reklam kaldırmayı destekler | İlk üretim sürümü **reklamsız, ücretsiz, satın alma ve reklam SDK'sız**. İzin ve politika sonrasında reklamlı + bir defalık `remove_ads` ayrıca değerlendirilecek | **Hayır: reklamsız çıkış için** |
| Gizlilik | Faz 4 şifreleme/backup/PII azaltma, Faz 6 opt-in arka plan sorgusu mevcut | Uygulama içi ve herkesin erişebildiği Privacy Policy ile gerçek Data Safety eşleştirmesi zorunlu | **Evet** |
| Yayın hazırlığı | `applicationId=com.aistudio.nextdns.mgrqvt`, `namespace=com.example`, `versionCode=1` | Kalıcı paket kimliği, geliştirici açıklamaları ve mağaza varlıkları Faz 9'da netleşmeli | **Evet** |
| Metadata | `metadata.json` hâlâ `MAJOR_CAPABILITY_SERVER_SIDE_GEMINI_API` içeriyor; Faz 4 Gemini/Firebase entegrasyonlarını kaldırdı | Yayınla ilgili içerikte Gemini özelliği ileri sürülmemeli; metadata temizliği ayrıca yapılmalı | Kontrol |
| PR birleştirme | Faz 2–7 draft PR; `main` eski | Kontrollü entegrasyon ve tüm CI kontrolü Faz 9 ön koşulu | **Evet** |

## 2. NextDNS izin sorusunun kapsamı

Yazılı olarak ayrı ayrı doğrulanacak maddeler:
1. Kullanıcının **kendi API anahtarıyla** çalışan bağımsız Android istemcisinin Google Play'de dağıtımı.
2. Uygulama adı ve arama/listing açıklamasında açıklayıcı şekilde “NextDNS” kullanımı; yanlış resmî ilişki iddiası olmadan kullanım sınırları.
3. NextDNS resmî logo/renk/ikon kullanımının ayrıca izne tabi olup olmadığı.
4. API'nin ücretsiz, reklamlı ve ücretli uygulamalarda kullanım şartları, limitleri ve varsa iptal koşulları.
5. “Unofficial / NextDNS ile bağlantısızdır” açıklamasının kabulü ve gerekli atıf dili.
6. Yanıt alınamaması halinde *izin verilmiş kabul edilmeyecek.* Yayın öncesinde özgün marka/ikon ve ayrıca API sözleşmesi/şartları doğrulama kararı gerekecek.

Not: https://help.nextdns.io/terms destek sitesinin şartlarını gösterebilir; bu belgenin API ticari lisansı olduğunu varsaymayın. İlgili güncel yazılı yetki yoksa “API ticari kullanımı izinli” şeklinde beyanda bulunmayın.

## 3. Google Play politika çerçevesi

- [Google Play — Impersonation](https://support.google.com/googleplay/android-developer/answer/9888374?hl=en): Uygulama adı/ikon/store listing resmî ortaklık izlenimi yaratamaz.
- [Google Play — Intellectual Property](https://support.google.com/googleplay/android-developer/answer/9888072?hl=en): Kullanılan içerik ve görsellerin hakları geliştiricide veya lisanslı olmalı.
- [Google Play — Developer Program Policy](https://support.google.com/googleplay/android-developer/answer/18258653?hl=en): Kişisel ve hassas veri, Data Safety, Privacy Policy, SDK beyanları.
- [Google Play — Ads](https://support.google.com/googleplay/android-developer/answer/9857753?hl=en): İstenmeyen/geçişi engelleyen reklamlar yasak; güvenlik/ayar ekranlarıyla çakışmamalı.
- [Google Play — Payments](https://support.google.com/googleplay/android-developer/answer/10281818?hl=en): Dijital fayda için ödeme şartları; ülke/program istisnaları ayrıca kontrol edilecek.
- [Google Play — One-time products](https://developer.android.com/google/play/billing/one-time-products): Kalıcı reklam kaldırma **non-consumable** ürün kategorisi.
- [Google Play — Purchase lifecycle](https://developer.android.com/google/play/billing/lifecycle/one-time): `PURCHASED` doğrulaması, geri yükleme, acknowledgement, iptal/iade senaryoları.

**Reklam eklenirse gereken ikinci denetim:** reklam SDK'sı ve veri paylaşımı, Data Safety yeniden beyanı, çocuk/yaş hedefleme, reklam kişiselleştirme/onay, mağaza Ads beyanı, gizlilik metni, yeni BrowserStack testleri. Yaklaşık **1 USD** sadece önceki iş fikridir; son fiyat ülke, vergi ve kur tablosuyla karar verilir. “Bağış” adı kullanılarak reklam kaldırma dijital hakkı satılmayacak.

## 4. Güvenlik ve gerçek veri akışı nedeniyle gizlilik kapsamı

Depoda görülen uygulama izinleri: `INTERNET`, `ACCESS_NETWORK_STATE`, `POST_NOTIFICATIONS`.
- Kullanıcı API anahtarı Android Keystore/AES-GCM ile cihazda korunur; NextDNS API isteklerinde NextDNS sunucusuna gönderilir.
- DNS sorgu geçmişi uygulama tarafından kullanıcının profil API'sinden alınır; ekran/akış ve dışa aktarma işlevleri vardır. Kullanıcının NextDNS hesabındaki saklama tercihleri ile uygulama içi saklama birbirine karıştırılmamalı.
- Faz 6 WorkManager, **isteğe bağlı** bildirimler için belirli aralıklarla arka planda hesap/veri isteği yapabilir. Bu durum Privacy Policy ve gerektiğinde uygulama içi açıklamada yazılmalı.
- Faz 4'te gereksiz Firebase/AppCheck kaldırılmış. Depoda reklam/billing SDK'sı bulunmadığı ancak bağımlılık/nihai APK incelemesi ile doğrulanmalı.
- Geliştiriciye veri gönderilmediği iddiası ayrıca ağ trafik analizi ile sınanmalı; üçüncü taraf **NextDNS** aktarımı ve olası log indirimi açıkça belirtilmeli.
- Veri silme: Uygulama oturum kapatma/yerel sıfırlama ile NextDNS hesabını silme farklı işlevlerdir. Google Play account deletion kuralının gerçekten uygulanıp uygulanmadığı kullanıcı hesabı akışına göre değerlendirilecek.
- Privacy Policy'nin gerçek geliştirici iletişim bilgileri ve herkese açık URL'si henüz sağlanmamış; uydurma iletişim bilgisi yazılmayacak.

## 5. Faz 8 kabul kontrolü

**Tamamlanan analiz**
- [x] Resmî API belgesinin beta konumu
- [x] Google Play fikrî mülkiyet, impersonation, reklam, ödeme, kullanıcı verisi kuralları incelendi
- [x] Uygulama adı/ikon/paket adı ve üçüncü taraf bileşen giriş envanteri incelendi
- [x] GPLv3 referans proje mimarisi ile genel fark kaydedildi
- [x] Reklamsız ilk sürüm ve gelecekteki `remove_ads` için koşullu karar kaydedildi
- [x] BrowserStack'in Faz 9 yayın kapısı olması benimsendi

**Açık yayın kapıları**
- [ ] NextDNS'ten kapsamı belirli yazılı yanıt **veya** bağımsız marka/ikonla geçerli API kullanım dayanağı
- [ ] Bütün proje kaynaklarının ve görsellerin provenance onayı; GPL türetim kararı
- [ ] Bu karar üzerine gerçek `LICENSE`, uygun `NOTICE` / `THIRD_PARTY_NOTICES` ve gerekiyorsa kaynak sunumu
- [ ] Geliştirici kimliği/iletişim ile nihai Privacy Policy/Terms, uygulama içi bağlantılar, Data Safety
- [ ] Ad/icon/listing'de resmî bağlantı ima edilmediğinin son kontrolü
- [ ] Faz 2–8 entegrasyonu ve başarılı ana dal CI
- [ ] Faz 9: BrowserStack gerçek cihaz + Google Play dahili/kapalı test kapıları

**Risk değerlendirmesi:** Araştırma tamamlandı ancak Faz 8 **yayın için henüz kabul edilmiş değildir**. Hukuki değerlendirme kesin izin/lisans yerine geçmez.
