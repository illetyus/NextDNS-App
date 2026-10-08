# Faz 8 — Kaynak kökeni ve üçüncü taraf lisans envanteri

**Durum:** Apache-2.0 lisans seçimi uygulandı; üretim bağımlılıkları ve marka/görsel incelemesi bekliyor. Tarih: 2026-10-08. İncelenen kaynak: `phase7-ci-quality@16256c86`; belgelendirme dalı `phase8-compliance`.

## 1. Doğrudan kod kökeni

- Kullanıcı deposu: https://github.com/illetyus/NextDNS-App
- İlk `main` commit tarihi: **31 Ağustos 2026** (`7e3bfae`); Faz 7 geliştirmeleri ayrı PR/dallarda.
- Faz 8 öncesinde kök `LICENSE`, `NOTICE`, `README` yoktu. **Faz 8 dalında artık resmî Apache-2.0 `LICENSE`, `NOTICE` ve bağımsızlık/gizlilik konulu `README.md` bulunuyor.**
- **Geliştiricinin beyanı (8 Ekim 2026):** İlk uygulama kaynakları AI araçlarıyla sıfırdan üretildi; başka projelerden kod veya görsel alınmadı; başka hak sahibi veya katkıcı bulunmuyor; lisans tercihi **Apache-2.0**. Bu beyan, otomatik telif/benzerlik denetimi veya gelecekteki marka onayının yerine geçmez.
- Referans proje: https://github.com/doubleangels/nextdnsmanager — GPL-3.0; çoğunlukla `app/src/main/java/com/doubleangels/nextdnsmanagement/` altında Java ve WebView uygulaması.
- Bizim uygulama `app/src/main/java/com/example/` altında Kotlin, Jetpack Compose, Retrofit/OkHttp/NextDNS API yapısı kullanıyor.
- **Mimari ve dosya adı farklılığı tek başına kopyalama/türetme olmadığının kanıtı değildir.** Bu aşamada birebir kaynak kökeni kesinleştirilmedi. İkon, görseller, XML, UI metinleri, örnek kod ve başlangıçta içe aktarılmış kod da inceleme kapsamındadır.

## 2. Kaynak sahipliği doğrulama prosedürü

1. İlk 20 `main` commitini ve son Faz 2–7 PR'ların değişikliklerini kaynak dosya bazında gözden geçir; başka repodan alınan kod/ikon var mı araştır.
2. Başlangıçta kullanılan AI Studio/GitHub dışı taslakları ve kaynak bağlantılarını yazarın kayıtlarıyla eşleştir; `metadata.json` ve `com.aistudio.nextdns.mgrqvt` kimliği incelenecek.
3. Referans NextDNS Manager'ın Java ve XML varlıklarıyla **anlamlı parça eşleşmeleri** (salt ortak REST yol adları değil) ve görsel tasarım karşılaştırması yap.
4. Kod, XML, ikon, font, ekran görüntüsü veya şablon için köken, orijinal lisans, varsa değişiklik ve atıf kaydı tut.
5. Eğer GPLv3 eserinden telif kapsamına giren kod/görsel uyarlaması tespit edilirse lisans yükümlülüklerini değerlendir ve uygun lisans/kaynak paylaşımı olmadan APK yayımlama.
6. Hak sahibinin beyan edilen tercihiyle özgün uygulama kodu/belgeleri için değiştirilmemiş Apache-2.0 lisans metni köke **eklendi** (SPDX resmi kayıt ile karşılaştırıldı). Bu adım görsel/marka hakkı veya GPL türetim ihtimalini çözmez; somut ihlal bulgusu olursa giderilmeden yayın yapılmaz.
7. Nihai bağımlılık raporu için üretim varyantının **resolved runtime dependency tree**, AAR/JAR ve POM lisans bilgileri ve transitif lisansları çıkar. Sadece `libs.versions.toml` listesi yeterli değildir.

## 3. İncelenen doğrudan bileşen aileleri

Aşağıdakiler kullanılan sürüm/üretim artifact metadatasına karşı **tek tek yeniden doğrulanmalıdır**. Bu envanter telif lisansı dosyasının yerini tutmaz.

| Bileşen grubu | Repo bağımlılığı / kullanım | Üst proje lisansı (ön kontrol) | Yayın kararı |
|---|---|---|---|
| AndroidX Compose/Activity/Lifecycle/Navigation/WorkManager/DataStore | `gradle/libs.versions.toml` | AndroidX yaygın Apache-2.0; **ilgili artifact/POM teyidi gerekli** | Teyit |
| Kotlin/kotlinx.coroutines | Coroutines runtime/test | Apache-2.0, https://github.com/Kotlin/kotlinx.coroutines | Teyit |
| OkHttp + Logging Interceptor + MockWebServer | Network, test | Apache-2.0, https://github.com/square/okhttp | Teyit |
| Retrofit + Moshi | API/JSON | Square ailesi Apache-2.0, https://github.com/square/retrofit ve https://github.com/square/moshi | Teyit |
| Roborazzi | Screenshot testleri | Apache-2.0, https://github.com/takahirom/roborazzi | Teyit |
| Robolectric | JVM Android testleri | https://github.com/robolectric/robolectric , kesin artifact/lisans kaydı hazırlanmalı | Teyit |
| JaCoCo | CI kapsam raporu | EPL-2.0, https://github.com/jacoco/jacoco | Özellikle yalnız geliştirme aracı mı dağıtılan yapıya giriyor mu doğrula |
| Gradle, Android Gradle Plugin, KSP | Build araçları | Her aracın ilgili kaynak/POM lisansı incelenecek | Teyit |
| NextDNS API | Haricî servis, dağıtılan açık kaynak kütüphane değil | Kamuya açık API dokümanı ve geçerli kullanım şartları kontrol edilmeli; yazılı ön izin her bağımsız istemci için otomatik zorunlu sayılmaz | Politika kontrolü |
| NextDNS Manager | **Doğrudan Gradle bağımlılığı değil**; olası kaynak/vaka inceleme hedefi | GPL-3.0, https://github.com/doubleangels/nextdnsmanager/blob/main/LICENSE | Apache-2.0 seçildi; önemli parça kopyalanmadığı teyidi son kontrolde kalır |

## 4. Üretim dağıtımını ilgilendiren ayrım

- CI aracı / test-only kütüphane ile APK içine paketlenen runtime kütüphane birbirinin yerine değerlendirilmez.
- Bir kütüphanenin izin verici olması, telif ve atıf koşullarını yok etmez.
- Üst projenin README'deki lisans etiketi, gerçek kullanılan sürümün transitif bileşenleri için otomatik doğrulama değildir.
- `GPL-3.0` kod içeriliyorsa kullanıcı projesini yalnızca “Apache-2.0” diye etiketlemek, o kodun kaynak yükümlülüğünü ortadan kaldırmaz.

## 5. Açık kabul maddeleri

- [ ] Her telif eser dosyasının sahiplik veya lisans kaynağı doğrulandı.
- [ ] NextDNS Manager veya diğer GPL eserinden alıntı/türetim var mı belgeyle karara bağlandı.
- [ ] Üretim variantının tam transitive SBOM/lisans tablosu üretildi.
- [ ] Şartları gerektiren tüm lisans/NOTICE'lar eklendi ve final APK ile dağıtım kanalı doğrulandı.
- [x] Projenin özgün kod/belgeleri için değiştirilmemiş Apache-2.0 lisans metni, `LICENSE` ve proje `NOTICE`/`README` eklendi.
- [ ] Üretimden önce NextDNS markasıyla karıştırılabilecek mevcut launcher görselleri özgün tasarımla değiştirildi veya haklar doğrulandı.

**Kaynak:** https://www.gnu.org/licenses/gpl-faq.en.html ve https://www.gnu.org/licenses/gpl.en.html
