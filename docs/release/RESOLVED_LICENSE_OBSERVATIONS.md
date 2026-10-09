# Çözülmüş lisans metadatası — 9 Ekim 2026

İlk gerçek çözümleme [CI 37857163587](https://github.com/illetyus/open-source-client-for-nextdns/actions/runs/37857163587) içindeki `release-audit-and-device-tests` artifact'ından incelendi. Bu koşu paket manifesti parser'ı nedeniyle başarısızdır; **güvenlik taraması veya tüm CI başarısı olarak gösterilmez**. Bağımlılık çıkarma adımı ise 138 modül, 98 artifact hash'i ve 22 gömülü lisans/NOTICE girdisi üretti. Artifact'sız 41 modül platform/BOM veya çözümleme metadatasıdır; bu sayı APK'da 138 yazılım modülü bulunduğu anlamına gelmez.

Doğrudan POM alanlarında 134 Apache-2.0 ailesi, 2 BSD-3-Clause ve 1 GPL-2.0 Classpath Exception beyanı görüldü. Bir koordinatta doğrudan `<licenses>` alanı yoktur. Bu metadata sınıflandırması proje kaynaklarının Apache-2.0 seçimini değiştirmez; son imzalı dosyanın bağımsız içerik veya hukuk onayı değildir.

| Koordinat | Çıktıdaki beyan / kaynak |
|---|---|
| `androidx.datastore:datastore-preferences-external-protobuf:1.1.7` | BSD-3-Clause POM metadatası |
| `com.android.tools:desugar_jdk_libs_configuration:2.1.5` | BSD-3-Clause POM metadatası |
| `com.android.tools:desugar_jdk_libs:2.1.5` | GPL version 2 with Classpath Exception; [üreticinin lisans metni](https://github.com/google/desugar_jdk_libs/blob/master/LICENSE) |
| `com.google.guava:listenablefuture:1.0` | Doğrudan POM lisans alanı boş; [modül POM'u](https://repo.maven.apache.org/maven2/com/google/guava/listenablefuture/1.0/listenablefuture-1.0.pom) `guava-parent:26.0-android` kullanır. [Tam ebeveyn POM'u](https://repo.maven.apache.org/maven2/com/google/guava/guava-parent/26.0-android/guava-parent-26.0-android.pom) Apache Software License 2.0 beyan eder. Bu ilişki ayrıca okunmuştur; ham `missing-pom-licenses.json` kaydı, doğrudan alanın yokluğunu doğru biçimde korur. |

Gömülü NOTICE çıktısında `.class` dosyası veya lisans adına benzeyen derlenmiş sınıf girdisi bulunmadı. Arşiv içindeki metinler değiştirilmeden çıkarılır. AAR içindeki nested/transformed kod, R8 sonrası kalan içerik, tam metin/attribution dağıtımı ve imzalı varyant için son inceleme ayrıca kaydedilir. POM adı veya URL'si, tam lisans metninin dağıtıldığına dair kanıt yerine kullanılamaz.

9 Ekim imzalı inceleme ekinde Google Maven/Maven Central dosyaları bağımsız indirilerek 98 artifact hash kaydı eşleştirilmiştir. AAR içindeki nested JAR'lar da taranmış, ek lisans/NOTICE metni bulunmamıştır. Tam proje ve upstream lisans metinleri, 22 artifact bildirimi ve çözülmüş girdi listesi `assets/licenses` içinde dağıtılır; giriş/Settings okuyucusu çevrimdışı çalışır. Pinned metin kaynakları `upstream-sources.json` içindedir. Ham doğrudan POM boşluğu korunurken listenablefuture'ın doğrulanmış ebeveyn lisansı dağıtım listesinde açıkça belirtilir. Desugar kaynak arşivi ve binary attestation sınırı [imzalı inceleme kaydında](SIGNED_REVIEW_CANDIDATE.md) ayrılır.
