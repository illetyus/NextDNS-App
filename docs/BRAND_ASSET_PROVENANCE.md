# Faz 9.3 — özgün ağ düğümleri simgesi

9 Ekim 2026: Kullanıcının seçtiği DNS/ağ düğümleri fikri, proje içinde beş daire ve dört bağlantı çizgisinden oluşan özgün vektör geometriyle uygulandı. Kaynak `docs/brand/network-nodes.svg`; tekrar üretim aracı `scripts/generate_launcher_assets.py`. Harici logo, görsel, font veya stok varlık kullanılmadı. Projenin özgün varlıkları Apache-2.0 kapsamındadır.

Koyu zemin, turkuaz bağlantılar, açık düğümler ve amber merkez; NextDNS kalkanı veya harf biçimi kullanılmaz. Önceki launcher kalkanı ve eski WebP dosyaları değiştirildi. Başlık/giriş ekranındaki dekoratif kalkan da yeni düğüm biçimini kullanır; güvenlik sekmelerinin genel Material simgeleri bu marka varlığı değildir.

Adaptive katman 108dp, motif merkezde 66dp güvenli alan içinde; Android 13+ temalı ikon için tek renk katman bulunur. API 24–25 için 48/72/96/144/192px PNG'ler ve yuvarlak karşılıkları vardır. Mağaza simgesi 512px RGBA PNG; mağazanın uyguladığı köşe/ gölge işlemleri kaynakta taklit edilmez.

Kaynaklar: [Android adaptive icons](https://developer.android.com/develop/ui/compose/system/icon_design_adaptive), [Google Play icon specifications](https://developer.android.com/distribute/google-play/resources/icon-design-specifications).

Bu değişiklik, eski kalkanın benzerlik riskini gideren görsel ayrıştırmadır; marka araştırmasının tamamlandığı veya NextDNS'ten onay alındığı iddiası değildir. Uygulama bağımsız ve gayriresmîdir. Launcher maskeleri ve temalı görünümün gerçek cihaz kontrolü B11–B12 kapsamında kalır.
