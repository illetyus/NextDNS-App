# Ana plan ve Faz 9 uygulama kaydı

Bu kayıt önceki fazların kapsamını korur. Bir alt işin kaynak koduna eklenmesi, Faz 9'un veya yayın kapılarının tamamlandığı anlamına gelmez. PR sayfaları ilgili entegrasyon ve CI kanıtını taşır; gerçek cihaz sonuçları `release/readiness.json` içinde ayrı tutulur.

| Faz | Kaynakta bulunan çalışma | Durumun sınırı |
|---|---|---|
| 2–7 | API sözleşmesi, canlı uzlaşma, sunucu mutasyonları, güvenlik, adaptif gezinme, bildirim ve test altyapısı | Önceki ana dal çalışmaları korunur; yeniden geliştirilmez. |
| 8 | Apache-2.0, kaynak/lisans kayıtları, marka/API ve Türkiye/AB hukuk araştırması | Lisans seçimi uygulanmıştır; son paket ve yayın incelemesi ayrı kalır. |
| 9.1 | Open Source Client for NextDNS adı ve yeni GitHub adresi | [PR #11](https://github.com/illetyus/open-source-client-for-nextdns/pull/11), [PR #12](https://github.com/illetyus/open-source-client-for-nextdns/pull/12); paket kimliği, API adresleri ve protokoller korunur. |
| 9.2 | TR/EN/DE/FR/ES arayüz, İngilizce fallback, sistem ve uygulama içi dil seçimi | [PR #13](https://github.com/illetyus/open-source-client-for-nextdns/pull/13); dil kaynakları ve JVM görüntüleri, cihaz/OEM ve profesyonel dil incelemesinin yerine geçmez. |
| 9.3 | Özgün ağ düğümü simgesi; adaptive, monochrome, legacy ve mağaza varlıkları | [PR #15](https://github.com/illetyus/open-source-client-for-nextdns/pull/15); eski kalkanın görsel karıştırılma riski kaynakta giderilir, launcher/mağaza sunumu cihaz kapısında değerlendirilir. |
| 9.4 | Akademik araştırma, beş dilli Koşullar ve ayrı İngilizce Privacy; hash/revizyon doğrulaması, yerel kabul zamanı ve yeniden açılan okuyucu | [PR #14](https://github.com/illetyus/open-source-client-for-nextdns/pull/14); metinler **DRAFT**, kimlik/iletişim/kamuya açık Privacy adresi doğrulanmamıştır. |
| 9.5 | Gerçek Android release grafiğini hedefleyen tarama, lisans/SBOM/NOTICE envanteri, paket hash/manifest/imza kayıtları | [PR #16](https://github.com/illetyus/open-source-client-for-nextdns/pull/16); CI kanıtı ile son imzalı paket/ağ trafiği/lisans dağıtımı incelemesi ayrı tutulur. |
| 9.6 | Native giriş/kabul, gezinme, TLS fixture ve izin testleri; kanıta bağlı BrowserStack gönderim aracı | **B01–B22: 22 NOT_RUN, 0 PASS.** App Live ve App Automate sonuçları yoktur; erişim değişkenleri mevcut oturumda bulunamamıştır. |
| 9.7 | Beş dilde mağaza metinleri; ücretsiz/reklamsız TR+AB hazırlığı; kanıt kapısı | Play internal/closed/prelaunch/Data safety ve public yayın uygulanmamıştır. |
| 10 | API değişikliği, bağımlılık, sürüm/geri çekme ve yayın sonrası bakım çalışma planı | [PHASE10_MAINTENANCE_RUNBOOK.md](PHASE10_MAINTENANCE_RUNBOOK.md); kaynakta hazırlık kaydıdır, yayın sonrası çalışma veya sürekli izleme başlatılmamıştır. |

Hukuki yapı faaliyete özgü ayrım, ölçülü garanti/sorumluluk sınırı ve emredici hukuk istisnasına dayanır; kesin sorumsuzluk sonucu iddia etmez. Yeni SLA, destek, hizmet sürekliliği veya tazmin taahhüdü eklenmez. Nihai yayıncı incelemesi son yayın diliminde kalır; doğrulanmamış kişi, adres, onay veya iletişim bilgisi üretilmez.

Faz 9 açık durumdadır. Son imzalı paket incelemesi, gerçek cihaz kanıtı ve mağaza kanalları olmadan ilk public sürüm tamamlanmış sayılmaz. Araştırma için [9 Ekim literatür dosyası](legal/LITERATURE_REVIEW_TR_EU_2026-10-09_DRAFT.md), cihaz kapsamı için [B01–B22 kapısı](PHASE9_BROWSERSTACK_RELEASE_GATE.md) esas alınır.
