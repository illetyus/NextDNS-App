# Faz 9.5 — üretim bağımlılığı ve paket kanıtı

Önceki kök `snyk test` komutu Android `:app` grafiğinin tarandığını kanıtlamıyordu. CI artık `--sub-project=app --configuration-matching='^releaseRuntimeClasspath$'` ile üretim varyantını hedefler; sıfır bağımlılıklı raporu reddeder ve JSON sonucunu saklar. [Snyk resmî Gradle/Android belgesi](https://docs.snyk.io/supported-languages/supported-languages-list/java-and-kotlin/snyk-cli-for-java-and-kotlin).

Gradle çözümleme sonucundan `releaseRuntimeClasspath` ve `coreLibraryDesugaring` modülleri, artifact SHA-256 özetleri, ilgili POM lisans adları/adresleri ve arşiv içi LICENSE/NOTICE girdileri çıkarılır. `release-sbom.cdx.json` CycloneDX 1.6 bağımlılık envanteridir; imzalı paket içinde bulunabilecek gömülü/transformed kodun bağımsız taraması olarak sunulmaz. Lisans metadatası bulunamayan girdiler ayrı raporlanır; varsayımsal lisans atanmaz. [Gradle MavenPomArtifact API](https://docs.gradle.org/current/javadoc/org/gradle/maven/MavenPomArtifact.html).

CI debug APK, androidTest APK ve imzasız release APK/AAB üretir. Bunların artifact hash ve commit kayıtları gerçek cihaz testinde kullanılacaktır. İmzasız CI paketi üretim imzalı paket kabul edilmez. Son imzalı varyantın manifest, ağ trafiği, anahtar/log sızıntısı, NOTICE dağıtımı ve imza eşdeğerliği B07, B13, B14 ve B22 kapsamında ayrıca doğrulanır.

Yerel [imzalı inceleme akışı](release/SIGNED_REVIEW_CANDIDATE.md) bu çıktılara bağlanır. CI ayrıca release içeriğinde 10 native güvenlik testini API 35 emülatöründe çalıştırır; geçici imza dışındaki tüm girdileri eşleştirir. Yerel signer APK/AAB ve public sertifikayı doğrular. Ağ fixture'ları, logcat canary ve AndroidKeyStore denetimi gerçek hesap/BrowserStack kapsamından ayrı kaydedilir. Tam lisans/NOTICE metinleri paket içine ve çevrimdışı okuyucuya eklenmiştir. DRAFT ve son yayın onayları korunur.

Snyk servis/limit hatası güvenlik başarısı sayılmaz; kapı atlanmaz. BrowserStack B01–B22, son yayıncı incelemesi ve Play test kanalları çalıştırılmadan public release kararı verilmez.
