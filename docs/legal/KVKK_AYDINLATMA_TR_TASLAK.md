# Open Source Client for NextDNS — Türkiye KVKK Aydınlatma Metni (koşullu taslak)

**DURUM: YAYINLANMAYACAK ÇALIŞMA TASLAĞI — 8 Ekim 2026.**  
Bu metin "KVKK sözleşmesi" veya genel açık rıza belgesi değildir. 6698 sayılı Kanun m.10 uyarınca **uygulama geliştiricisinin veri sorumlusu sıfatını taşıdığı belirlenen somut işlemler** için hazırlanacak aydınlatmanın taslak iskeletidir. Veri sorumlusu sıfatı, işleme faaliyeti bazında ayrıca doğrulanmadan bu belge uygulamaya veya internet sitesine konulamaz. NextDNS'e ait veri işleme faaliyetleri, otomatik olarak uygulama geliştiricisine atfedilemez.

**Yayıncı/kimlik:** [VERİ SORUMLUSU NİTELİĞİ DOĞRULANDIKTAN SONRA TİCARİ/GERÇEK KİŞİ ADI]  
**İletişim/başvuru:** [DOĞRULANMIŞ ADRES VE BAŞVURU YÖNTEMİ]  
**Yürürlük tarihi:** [KESİN TARİH]  
**Kapsam:** Türkiye'de uygulamanın kişisel veri işleme faaliyetleriyle bağlantısı bulunan ilgili kişiler.

## 1. Aydınlatmanın kapsamı

Open Source Client for NextDNS, NextDNS hizmetinin bağımsız ve resmî olmayan Android yönetim istemcisidir. NextDNS, hesapların, DNS altyapısının, sunucu ortamının, hizmete ait kayıtların ve abonelik işlemlerinin ayrı sağlayıcısıdır. Bu metin, yalnızca uygulama geliştiricisinin veri sorumlusu sıfatıyla gerçekleştirdiği tespit edilen işlem ve veri kategorilerini kapsar; NextDNS'in kendi sunucularındaki işlemlerinin tümünü kapsadığı iddia edilmez.

## 2. İşleme faaliyetleri, kişisel veri kategorileri, amaçlar ve hukuki sebepler

Aşağıdaki tablo **hukuki incelemesi yapılmamış aday işlem envanteridir**. Nihai metinde yalnızca geliştiricinin veri sorumluluğu kapsamında bulunduğu belirlenen ve gerçekten uygulanan işlemler yer almalıdır.

| İşlem / veri kategorisi | Kullanım amacı / işleme yöntemi | Veri sorumlusu ve somut hukuki sebep |
|---|---|---|
| Kullanıcı tarafından sağlanan NextDNS API anahtarı | Cihazda güvenli saklama ve NextDNS API isteğini yetkilendirme | [ROL TESPİTİ / KVKK 5. MADDEDEKİ SOMUT ŞART / GEREKÇE] |
| NextDNS hesabı adı, e-posta, profil adları/kimlikleri ve ayarları | Kullanıcının hesabına erişim ve seçilen ayarların arayüzde gösterilmesi | [ROL / HUKUKİ SEBEP] |
| DNS sorgu geçmişi, alan adları, cihaz/IP bilgileri, kullanım analizi | Kullanıcı istediğinde NextDNS hesabındaki günlük ve analizleri görüntüleme | [ROL / HUKUKİ SEBEP; BAŞKA İLGİLİ KİŞİLERİN VERİLERİ DE OLABİLİR] |
| Bildirim tercihleri, profil özetleri, ilgili zaman kayıtları | Kullanıcı tarafından etkinleştirilen yerel durum bildirimlerini yürütme | [ROL / HUKUKİ SEBEP] |
| Kullanıcının seçtiği günlük dışa aktarma dosyaları | Talep edilen dışa aktarımın cihazda gerçekleştirilmesi | [ROL / HUKUKİ SEBEP / DIŞ DOSYA SINIRI] |

Açık rıza gerekli olmayan işlemler için "her şeyi kabul ediyorum" şeklinde toplu rıza talep edilmeyecektir. Gerçek bir işlem için açık rıza hukuki dayanak olarak gerekiyorsa **işleme amacıyla bağlantılı ayrı bir açık rıza metni ve tercihi** hazırlanacaktır.

## 3. Veri toplama yöntemleri

Veriler, kullanıcının girdiği erişim bilgilerinden, NextDNS'in API aracılığıyla gönderdiği hesap ve profil yanıtlarından ve uygulamanın cihazda oluşturduğu sınırlı yerel işleyiş kayıtlarından elektronik ortamda elde edilebilir. Uygulamanın başlangıç tasarımı, geliştirici tarafından işletilen ayrı bir kullanıcı hesabı veya DNS geçmişi toplama sunucusu öngörmemektedir. Gerçek aktarım noktaları, son imzalı uygulama paketinde ağ gözlemiyle doğrulanmalıdır.

## 4. Alıcılar ve yurt dışına aktarım

Uygulama, ilgili işlemler için NextDNS API'sine doğrudan bağlanabilir. NextDNS tarafından verilen dışa aktarım URL'si farklı HTTPS alan adına işaret edebilir. Geliştirici sunucusuna aktarım planlanmamaktadır; ancak bu, NextDNS'e gönderilen verilerin aktarıldığı ülkenin veya KVKK m.9 bakımından sorumlu tarafın belirlendiği anlamına gelmez.

**Yayımdan önce doldurulması zorunlu alanlar:**
- [HANGİ VERİ KATEGORİSİ HANGİ ALICIYA GİDİYOR?]
- [AKTARIMIN HUKUKİ SINIFLANDIRMASI VE VERİ AKTARAN TARAF]
- [YURT DIŞI AKTARIM MEKANİZMASI, UYGULANABİLİRLİK VE KANIT]
- [DİĞER ÜÇÜNCÜ TARAF SDK VEYA SUNUCULAR VAR MI?]

Bu alanlar çözülmeden "yurt dışına hiçbir veri aktarılmıyor" veya "bütün veri aktarımına rıza verilmiştir" ifadesi kullanılmaz.

## 5. Saklama ve silme

Kaynak kodunda çıkış işlemi, yerel API anahtarının, ilgili hesap/profil kayıtlarının ve hesapla bağlantılı bildirim verilerinin ve görevlerinin kaldırılmasını uygular. Bu işlem henüz son sürümde doğrulanmamış olup uygulama yayımlanmadan önce test edilmelidir. Kullanıcı tarafından seçilen konuma dışa aktarılan dosyalar, NextDNS sunucularındaki günlükler ve NextDNS hesabı, uygulamadaki çıkış işleminden kendiliğinden etkilenmez. Nihai aydınlatma, gerçekten uygulanan saklama sürelerini veya bu süreleri belirleyen objektif kriterleri açıkça bildirmelidir.

## 6. İlgili kişi hakları ve başvuru kanalı

Kanun'un 11. maddesindeki hakların ilgili veri sorumlusuna yöneltilmesi için [DOĞRULANMIŞ BAŞVURU ADRESİ VE YÖNTEMİ] kullanılacaktır. NextDNS sunucularındaki kayıtlar üzerinde NextDNS tarafından yürütülen işlemler bakımından doğru başvuru muhatabı ayrıca açıklanmalıdır. Başvuruların hangi kuruluş ve veri işleme faaliyeti kapsamında değerlendirildiği kullanıcıdan gizlenmemelidir.

## 7. Kullanıcı arayüzünde sunuluş şekli

- Aydınlatma metni ilgili veri işlemeye başlanmadan önce okunabilir şekilde erişilebilir olmalı; genel bir sözleşmeye gömülmemelidir.
- Terms of Use kabul kutusu **ayrı** tutulur. KVKK aydınlatması için genel bir "veri işlemeyi kabul ediyorum" kutusu kullanılmaz.
- Eğer işlemin hukuki şartı açık rıza değilse, ayrı bir açık rıza kutusu istenmez.
- Hukuki/akademik bir içerik disiplini korunmakla birlikte Türkçe metin özellikle kısa, açık ve anlaşılır olmalıdır.
- English-only Privacy Policy bağlantısı bu Türkçe aydınlatma metninin yerine kendiliğinden geçmez.

## 8. Yayına hazırlık kontrol listesi — bu bölüm kullanıcıya gösterilmez

- [ ] İşlem bazında veri sorumlusu/veri işleyen/bağımsız NextDNS tarafı rollerinin hukuken sınıflandırılması.
- [ ] Geliştirici kimliği ve doğrulanmış başvuru adresi.
- [ ] Her işlem için kişisel veri kategorileri, açık amaç, **somut** KVKK işleme şartı, toplama yöntemi.
- [ ] Her alıcı ve yurt dışı aktarımın tespiti; uygulanabilir m.9 koşulları.
- [ ] Gerekli saklama süresi veya kriterleri ve gerçek logout davranışı.
- [ ] Türkiye'de erişilebilir Türkçe, açık ve sade nihai metin.
- [ ] KVKK 2026/347 uyarınca açık rızanın metinden ayrılması, yalnız gerektiği işlemler için kullanılması.
- [ ] Gerçek ağ trafiği ve Google Play Data Safety ile çapraz tutarlılık kontrolü.

## Resmî referanslar — editoryal

- 6698 sayılı Kanun ve aydınlatma yükümlülüğü (KVKK m.10): https://www.kvkk.gov.tr/Icerik/4132/aydinlatma-yukumlulugunun-yerine-getirilmesinde-uyulacak-usul-ve-esaslar-hakkinda-teblig
- 18 Şubat 2026, Kurul 2026/347: https://www.kvkk.gov.tr/Icerik/8710/veri-sorumlulari-tarafindan-acik-riza-ve-aydinlatma-metinlerinin-ayri-ayri-duzenlenmesi-gerektigi-hakkinda-kisisel-verileri-koruma-kurulunun-18-02-2026-tarihli-ve-2026-347-sayili-ilke-kararina-iliskin-kamuoyu-duyurusu
- KVKK veri sorumlusu kimdir?: https://www.kvkk.gov.tr/Icerik/2032/Veri-Sorumlusu-Kimdir
- NextDNS ayrı hizmet gizliliği: https://nextdns.io/privacy

## 8 Ekim 2026 — Veri akışı ve aktarım matrisi

[D01–D12 veri akışı, KVKK/GDPR sıfat değerlendirmesi ve aktarım hukuku kontrolü](KVKK_GDPR_DATA_FLOW_AND_TRANSFER_ASSESSMENT_2026-10-08.md) hazırlanmıştır. Belge, hukuki sıfatları kesinleştirmez; sonraki denetim/test gereksinimlerini ayrı listeler.

Güncel araştırma: [Türkiye/AB literatür taraması — 9 Ekim 2026, DRAFT](LITERATURE_REVIEW_TR_EU_2026-10-09_DRAFT.md).
