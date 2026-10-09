# Yerel imzalı inceleme paketi

Bu dilim, başarıyla tamamlanan `main` CI'sından APK/AAB alır ve yayın öncesi kullanıcı incelemesi için yerelde imzalar. `SIGNED_REVIEW_CANDIDATE` durumu Play'e yükleme, public yayın veya hukuki metinlerin onayı anlamına gelmez. Koşullar ve Privacy **DRAFT** kalır; `readiness.json` içindeki nihai onaylar değiştirilmez.

## Teknik kanıt

CI, `ReleaseSecurityRuntimeTest` sınıfının **10 testini** Android 15/API 35 emülatöründe release APK üzerinde çalıştırır. Geçici CI anahtarıyla imzalanan APK'nın imza kayıtları dışındaki bütün ZIP girdileri, imzasız release çıktısıyla aynı olmalıdır. Testler release/debug ayrımını; kapalı HTTP günlüklerini; cleartext ve güvenilmeyen TLS sertifikasının reddini; anahtarlı isteğin yönlendirilmemesini; anahtarsız export istemcisinin HTTPS → HTTP yönlendirmesini reddetmesini; gerçek AndroidKeyStore AES-GCM saklama, kurcalama ve eski anahtar göçünü; yerel temizlemeyi; backup/transfer dışlamalarını ve paket içindeki lisans/hukuk dosyalarını inceler. Ağ sınamaları yerel TLS fixture'ları kullanır; gerçek hesaba işlem yapmaz. Sentetik anahtarın logcat'te bulunması kapıyı başarısız kılar.

Yerelde imzalanan APK'nın aynı payload'a sahip olması da doğrulanır. CI emülatörünün anahtarı ile yerel anahtar farklıdır; imzanın Android doğrulaması ayrıca yapılır. Bu kanıt, imza dışındaki release içeriğine ilişkindir; BrowserStack B01–B22 veya gerçek hesapla uçtan uca test sonucu yerine geçmez.

`verify_signed_review.py`, dört giriş paketinin SHA-256 değerlerini; başarılı `main` CI commit'ini; APK v2 imzasını; AAB'nin her veri girdisinin aynı sertifikayla imzasını; 16 KB ZIP hizalamasını; paket kimliği, debuggable ve cleartext manifest değerlerini; lisans metninin bire bir dağıtımını; test/coverage kodunun ve sentetik anahtarın release DEX'inde bulunmamasını; Snyk high/critical çıktısını ve runtime payload eşdeğerliğini kontrol eder. APK/AAB, public sertifika, özetler ve `signed-review-evidence.json` birlikte teslim edilir.

DEX kontrolü gerçek tür referanslarını okur; Compose/DataStore/OkHttp'in üretim kodundaki Robolectric/MockWebServer metinleri test sınıfı olarak sayılmaz. `release_dex_audit.py` gerçek test APK'sını ve kesilmiş DEX dosyalarını reddederek bu ayrımı CI'da sınar. Test/coverage türleri, JaCoCo alanları ve sentetik anahtar kapısı korunur. DEX 041 container biçimi otomatik kabul edilmez; desteklenmeyen biçim doğrulamayı durdurur. [DEX tür ve dize tabloları](https://source.android.com/docs/core/runtime/dex-format).

## Lisans dağıtımı ve kaynak kapsamı

Tam proje Apache-2.0 metni, upstream GPLv2/Classpath ve BSD metinleri, 22 artifact lisans/NOTICE girdisi ile 138 çözülmüş girdinin metadata listesi uygulamada çevrimdışı sunulur. Liste platform/BOM ve desugaring girdilerini de kapsar; APK'da 138 kütüphane bulunduğu iddiası değildir. `components.json` 98 artifact hash kaydını korur; CI `prepare_bundled_notices.py --check` ile sürüm değişimini reddeder. İlk envanterdeki 98 hash, Google Maven/Maven Central'dan bağımsız indirilen dosyalarla eşleştirilmiş; AAR içindeki JAR'lar da lisans metni için taranmıştır. Ek nested bildirim bulunmamıştır.

`desugar_jdk_libs` kaynak arşivi, üreticinin 2.1.5 sürüm hazırlama commit'i `73170c345e6a762fc6a1f0301bb15218850023ef` üzerinden değiştirilmeden alınır ve yerel inceleme ZIP'inde saklanır. Arşiv kendi GPL metnini ve dosya telif bildirimlerini içerir. Bu kaynak commit'i, Google'ın Maven binary'sinin yeniden üretilebilirliği açısından doğrulanmış build attestation olarak tanımlanmaz. Protobuf'un BSD metni upstream `v3.25.5/LICENSE` üzerinden aynen alınmıştır; bu URL, AndroidX'in shaded protobuf iç sürümüne ilişkin bir beyan değildir. Nihai yayın incelemesi bu ayrımları korur.

## İmzalama ve yerel saklama

`scripts/sign_review_windows.ps1` yalnız başarılı, eşleşen `main` CI çıktısını kabul eder. Mevcut proje anahtarı varsa onu kullanır; ilk kez oluştururken RSA-3072 ve 10.000 günlük sertifika üretir. Sertifika CN'si yalnız uygulama adıdır; doğrulanmamış gerçek kişi veya şirket bilgisi içermez.

Anahtar `%LOCALAPPDATA%/OpenSourceClientForNextDNS/signing/upload.p12` altında, güçlü rastgele parolası Windows kullanıcı hesabına bağlı DPAPI ile şifreli `password.dpapi` dosyasında tutulur. Klasör erişimi mevcut kullanıcı ve SYSTEM ile sınırlandırılır. Özel anahtar veya parola Git'e, CI'a ve inceleme ZIP'ine eklenmez. Yerel yedek kullanıcının Documents klasöründeki `CodexPrivate/OpenSourceClientForNextDNS-signing-backup` konumunda aynı erişim sınırıyla saklanır; **DPAPI yedeği başka bir Windows hesabında taşınabilir bir parola yedeği değildir**. Public `upload-certificate.pem` sır içermez.

İlk oluşturma erişimi sınırlar; sonraki imzalamalarda mevcut korumalı klasörün izinleri yeniden yazılmaz. Mevcut ACL'nin kalıtım kapalı ve yalnız kullanıcı/SYSTEM için FullControl olması doğrulanır; beklenmeyen izin varsa imzalama durur. Böylece tekrar kullanım, mevcut korumalı klasörün security descriptor'ını yeniden yazmak için Windows'un isteyebileceği ek yetkiye dayanmaz.

Play App Signing kaydı henüz yoktur. Yerel APK imzası ve AAB upload imzası doğrulanabilir olsa da Google'ın ileride kullanacağı uygulama imza anahtarı hakkında varsayım yapılmaz. İlk imzalı APK, eski debug imzalı kurulumun üzerine farklı sertifikayla güncelleme olarak kurulamaz; mevcut uygulama verisini silme işlemi otomatik yapılmaz.

Örnek kullanım: PowerShell'de `scripts/sign_review_windows.ps1` dosyasına `-EvidenceDirectory`, `-OutputDirectory`, `-BuildToolsDirectory` ve başarılı `main` koşusunun `-RunId` değerini verin. Çıktı klasörü önceden imzalı paket içeriyorsa dosyalar üzerine yazılmaz.

Nihai kullanıcı incelemesi, BrowserStack, mağaza kanalları ve kamuya açık Privacy sayfası sonraki yayın dilimlerinde kalır.

İmzalama yöntemi: [Android apksigner](https://developer.android.com/tools/apksigner), [Android uygulama imzalama](https://developer.android.com/studio/publish/app-signing). Emülatör ve instrumentation: [komut satırı emülatörü](https://developer.android.com/studio/run/emulator-commandline), [Android test komutları](https://developer.android.com/studio/test/command-line).
