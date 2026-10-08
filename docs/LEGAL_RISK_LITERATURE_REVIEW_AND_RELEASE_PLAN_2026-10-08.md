# Open Source Client for NextDNS — 8 Ekim 2026 hukuki risk araştırması ve güncellenmiş yayın planı

**Statü: Araştırma ve ürün gereksinimi. Hukuk mütalaası, NextDNS izni veya Google Play onayı DEĞİLDİR.** İncelenen taslak dal: phase8-compliance; PR #9.

## A. Temel sonuç ve kavramlar

- Sıfır sorumluluk vaadi yapılamaz. NextDNS DNS çözümleyici, abonelik ve sunucu hizmetleri ayrı sağlayıcıya aittir; buna karşılık uygulama sahibi kendi kodu, API kimlik bilgileri, doğru API istekleri, yerel veriler, güvenlik, tüketici bilgilendirmesi ve Play beyanlarından kanunun gerektirdiği ölçüde sorumludur.
- Kanunlar: TBK m.115 ağır kusurdan önceden sorumsuzluk anlaşmasını geçersiz sayar; 6502 tüketici sözleşmelerinin haksız şart denetimini gündeme getirebilir. Ücretsiz, Apache-2.0 veya unofficial ibareleri otomatik istisna değildir.
- Veri koruma rolü: veri sorumlusu / veri işleyen ayrımı sunucunun kime ait olduğu veya sözleşmede yazılan unvana göre değil, her veri işleme faaliyetinin amacını ve araçlarını kimin belirlediğine göre yapılır. Kullanıcı cihazından bağımsız NextDNS sunucusuna erişim bu incelemeyi gereksiz kılmaz.
- Google Play: Data Safety kapsamında collection verinin uygulamadan cihaz dışına gönderilmesini kapsar; NextDNS'e doğrudan API trafiği de incelenir. Sharing ayrı kategoridir; kullanıcı tarafından özellikle istenen aktarımlar için belirli istisnalar olabilir. Uygulama yayıncısı beyanlardan sorumludur.
- Marka: Uygulamanın adı, ikonu, giriş ekranı ve listesi resmî NextDNS izlenimi veremez. Disclaimers tek başına yanıltıcı tasarımı veya hak ihlalini düzeltmez.

## B. Resmî ve akademik kaynaklar

1. Google Play Impersonation: https://support.google.com/googleplay/android-developer/answer/9888374?hl=en
2. Google Play Intellectual Property: https://support.google.com/googleplay/android-developer/answer/9888072?hl=en
3. Google Play User Data, privacy policy and SDK responsibility: https://support.google.com/googleplay/android-developer/answer/10144311?hl=en
4. Google Play Data Safety collection/sharing definitions: https://support.google.com/googleplay/android-developer/answer/10787469?hl=en
5. Google Play App access / review: https://support.google.com/googleplay/android-developer/answer/9859455?hl=en
6. Google Play Account deletion: https://support.google.com/googleplay/android-developer/answer/13327111?hl=en
7. NextDNS public Beta API, profile operations, logs and download/clear: https://nextdns.github.io/api/
8. NextDNS Privacy Policy: https://nextdns.io/privacy
9. NextDNS Help Center Terms: https://help.nextdns.io/terms — **yalnızca Forumbee yardım merkezi sitesinin şartları; DNS/API genel hizmet sözleşmesi olarak KULLANILMAMALI.**
10. NextDNS kullanıcılarının genel hizmet koşullarını bulamadığına ilişkin topluluk kayıtları (bağlayıcı değildir): https://help.nextdns.io/t/35ylcw2/nextdns-terms-of-use
11. Benzer bağımsız istemci NextDNS Manager: https://github.com/doubleangels/nextdnsmanager — unofficial beyanı, Google Play/F-Droid farkları, GPLv3, opsiyonel FCM/Sentry.
12. Rethink DNS app vs DNS ayrılmış policy: https://www.rethinkdns.com/privacy
13. AdGuard 4 Ağustos 2025 Android-specific privacy notice: https://adguard.com/en/privacy/android.html
14. KVKK veri sorumlusu: https://www.kvkk.gov.tr/Icerik/2032/Veri-Sorumlusu-Kimdir
15. KVKK Aydınlatma Tebliği ve m.10: https://www.kvkk.gov.tr/Icerik/4132/aydinlatma-yukumlulugunun-yerine-getirilmesinde-uyulacak-usul-ve-esaslar-hakkinda-teblig
16. KVKK 18.02.2026 tarihli 2026/347 ilke kararı: https://www.kvkk.gov.tr/Icerik/8710/veri-sorumlulari-tarafindan-acik-riza-ve-aydinlatma-metinlerinin-ayri-ayri-duzenlenmesi-gerektigi-hakkinda-kisisel-verileri-koruma-kurulunun-18-02-2026-tarihli-ve-2026-347-sayili-ilke-kararina-iliskin-kamuoyu-duyurusu
17. KVKK m.9 yurt dışına aktarım reformu: https://www.kvkk.gov.tr/Icerik/2053/Yurtdisina-Aktarim
18. TBK m.115: https://mevzuat.mturkoglu.av.tr/belge/6098/madde/115
19. Ticaret Bakanlığı 6502 ve haksız şartlar mevzuatı: https://ticaret.gov.tr/tuketici/mevzuat/6502-sayili-tuketicinin-korunmasi-mevzuati
20. EDPB Guidelines 07/2020 controller / processor: https://www.edpb.europa.eu/documents/guideline/guidelines-072020-on-the-concepts-of-controller-and-processor-in-the-gdpr_en
21. Fransa CNIL mobil uygulama rehberi (2024, Nisan 2025 güncelleme): https://www.cnil.fr/fr/recommandations-applications-mobiles
22. Bilimsel sistematik tarama (2024): https://orbilu.uni.lu/handle/10993/62753
23. OWASP MASVS güvenlik standardı: https://mas.owasp.org/MASVS/

**Kaynak seviyesi:** 1-8, 14-19 platform/resmî normatif dayanak; 9-13 sağlayıcı/emsal; 20-23 karşılaştırmalı hukuk/araştırma/güvenlik rehberi. Emsal projede bir metnin bulunması uygulamamızın hukuki doğrulanması değildir.

## C. Mevcut Faz 8 planındaki somut açıklar

1. **Kritik — marka:** strings.xml hâlâ app_name=NextDNS; LoginScreen hem marka adı hem kalkan ikonunu gösteriyor. Nihai orijinal ad/icon zorunlu.
2. **Kritik — NextDNS hizmet koşulları:** API Beta belgelenmiş. Ancak üçüncü taraf istemciler, marka ve ticari API kullanımı bakımından bütün bağlayıcı genel DNS hizmet şartları doğrulanmadı. Forumbee şartlarını dayanak yapmayın.
3. **Kritik — veri açıklaması:** Sadece geliştirici sunucusu olmaması nedeniyle Data Safety alanında No data collected seçilemez. Release AAB, tüm runtime SDK graph, merged manifest ve ağ trafiği test edilmeli.
4. **Yüksek — onaydan önce ağ isteği:** İstemci kayıtlı anahtarla yeniden bağlanıyor; WorkManager ayrı başlatılabiliyor. Yeni Terms versiyonuna kabulden önce ağ/notification work durmalı.
5. **Yüksek — logout:** NextDnsPreferences temizleniyor ama NotificationPreferences ayrı DataStore; lokal config hash, zamanlar, bildirim ayarları kalabilir. Tam temizleme ve iki hesap arası izolasyon gerekir.
6. **Yüksek — API mutation:** Profil silme, kuralları değiştirme, log temizleme riskli; açık hedef teyidi, iki adımda silme, test ve server sonuç doğrulaması gerekir.
7. **Yüksek — kaynak/lisans:** Apache-2.0 özgün koda uygulanır. Başka GPLv3 istemcinin kod/varlıklarına uygulanamaz. Türev olmadığının kaynak kanıtı ve üçüncü taraf resolved transitif lisans tablosu istenir.
8. **Yüksek — KVKK/AB:** DNS günlükleri aynı hanedeki birden çok kişinin tarama verisini içerebilir. Veri sorumlusu ve cross-border aktarım hukuki rolleri işlem bazında çözümlenmeli.
9. **Orta — 5 dilde metin eşdeğerliği:** Terms tr/en/de/fr/es, privacy İngilizce kararı korunur; ülke hukukuna göre yerel aydınlatmalar değerlendirilir.
10. **Orta — Play app account:** Uygulama kendi hesabını oluşturmuyor; kullanıcı NextDNS API key sağlıyor. Ancak nihai onboarding harici kayıt/link tasarımına göre account deletion beyanı tekrar incelenmeli.

## D. Düşük riskli teknik mimari (v1)

- Android istemci -> NextDNS API dışında geliştiriciye kullanıcı verisi toplayan backend, telemetri, reklam, Firebase/FCM/Sentry vb. yok.
- VPN/DNS resolver olarak pazarlanmaz; OS DNS trafiğini üstlenmez.
- API key Keystore AES-GCM, secrets için backup/log dışlama; export dosyası kullanıcıya ait; NextDNS'in verdiği log-download HTTPS endpointleri trafik denetiminde incelenir.
- Local hesap verileri ve notification DataStore logout ile silinir, in-flight request sonuçları eski hesaba sızamaz.
- Yıkıcı işlemler için hedef profili doğrula, çift aşamalı onay ve server GET ile sonuç teyidi. Timeout başarısızlık/unknown sonucunu sahte başarı sayma.
- Uygulamaya ilk giriş, kayıtlı API key ile auto-login ve notification scheduler yalnız kabul edilen Terms sürümünden sonra erişilebilir.
- Bildirimler tercihe bağlı; izin yalnız özellik etkinleşince istenir.
- Uygulama GDPR/KVKK gerekleri dışında ilave kişisel veri istemez; gerçek APK ile kaynak/manifest uyuşması test edilir.

## E. Doğru sorumluluk sınırı ve taslak dil

**Tanım:** Bağımsız ve resmî olmayan Android NextDNS hesap yönetim istemcisi. NextDNS tarafından geliştirilmez, işletilmez, desteklenmez veya onaylanmaz (resmî izin gelmediği sürece). NextDNS DNS altyapısı, sunucu saklama, hesaplar, abonelik, API istikrarı bağımsız hizmet kapsamındadır. Uygulama geliştiricisi kendi kodu, güvenliği ve kişisel veri işleme işlemleri bakımından mevzuat kapsamındaki sorumluluğunu korur.

**Örnek İngilizce (hukuk kontrolü öncesi):**
> Independent, unofficial third-party client. This application provides an interface to selected NextDNS account-management features through the NextDNS API. It is not developed, operated, sponsored or endorsed by NextDNS. NextDNS independently operates its DNS infrastructure, account services, subscriptions and server-side processing. Features may become unavailable if NextDNS changes its service or API. Nothing in these Terms excludes responsibilities or rights that cannot lawfully be excluded.

**Yasaklı yanlış beyanlar:** 100% güvenli, tüm bilgiler cihazda kalır, hiç veri işlemiyoruz, NextDNS resmen yetkilendirdi, geliştirici hiçbir koşulda sorumlu tutulamaz, her Google API'yi kullanıyoruz.

## F. Yenilenmiş yayın kapıları

**Faz 8 — kapatma öncesi**
- [ ] NextDNS DNS hizmeti/API üçüncü taraf koşulları ve marka riskine dair birincil delil ya da yetkili hukuk görüşü al; belirsizlik varsa açık risk kaydı ve durdurma.
- [ ] Öz kaynak/asset provenance ve GPLv3/Apache-2.0 uyum denetimi.
- [ ] KVKK işlem/rol/hukuki dayanak/yurt dışı aktarım matrisi, gerekiyorsa Türkçe aydınlatma.
- [ ] Bağımlılık lisansları ve en güncel Faz8 CI koşusu doğrulansın.

**Fazlar arası entegrasyon**
- [ ] Faz 2–8 draft PR'ları bağımlılık sırasıyla birleştir; Faz 7 alternatif PR kollarını kopya merge etme.
- [ ] Tek entegre release AAB üzerinden lint/SDK/network/API/key/backups denetimi.

**Faz 9 — yayın öncesi**
- [ ] Orijinal isim/ikon, disclaimer, beş dilde Terms, İngilizce Privacy; gerekiyorsa Türkçe KVKK aydınlatma.
- [ ] Önce bilgilendirme+Terms kabulü, sonra API erişimi; hukuken gerekli bağımsız rızaları ayır.
- [ ] Logout temizliği, profil silme ve kritik mutation onayı, uygun hata ve geri alma testleri.
- [ ] Review App access (güvenli test NextDNS hesabı ve İngilizce inceleme talimatı), kimlik/iletişim/privacy HTTPS sayfası.
- [ ] Doğru Data Safety alanları, gerçek network ve transitive SDK testleri, Google Play diğer içerik beyanları.
- [ ] BrowserStack gerçek cihaz testleri: beş dil, onay, cache, backup, log export, 401/429/5xx, logout ve farklı hesap; tüm P0/P1 kapalı.

**Faz 10 — bakım**
- [ ] API değişiklikleri için uyumluluk izleme ve güncelleme; ayrı kullanıcı onayı olmadan otomatik zamanlanmış takip görevi kurulmaz.
- [ ] Güvenlik bildirim kanalı; hassas hesap verisi içermeyen hata raporlama; her sürümde privacy ve Terms revizyon kontrolü.

## G. Bilinen belirsizlikler ve yayın engeli

- Doğrulanmadı: NextDNS'in üçüncü taraf API kullanımına uygulanabilir eksiksiz sözleşme, marka izni.
- Doğrulanmadı: Nihai AAB'de bütün üçüncü taraf SDK'ların absence/presence ve gerçek trafik.
- Doğrulanmadı: Uygulama geliştiricisinin KVKK/GDPR rolü, yurt dışı aktarımdaki hukuki sıfatı.
- Doğrulanmadı: Nihai geliştiren tarafın kimlik/iletişim verileri, 5 dilde hukuki eşdeğerlik, Play hesabı türü.
- Henüz yapılmadı: Consent UI, production privacy URL, tam logout, özgün marka, BrowserStack release testi ve Play gönderimi.

**Bu rapor yeni sorumsuzluk iddiası üretmez; belirsiz ve hukuken riskli alanları yayın kararından önce doğrulanacak durdurma şartları hâline getirir.**


### 8 Ekim 2026 — Marka adı uygulama durum güncellemesi

Kararlaştırılan uygulama adı **Open Source Client for NextDNS** olarak kaynaklara uygulanmıştır; `app_name=NextDNS` tespiti bu belgenin önceki kaynak durumu için geçerlidir. Özgün uygulama simgesi, NextDNS marka/hizmet şartları ve mağaza açıklamaları **ayrıca** onaylanacaktır. Depo `illetyus/open-source-client-for-nextdns` olarak yeniden adlandırılmıştır.
