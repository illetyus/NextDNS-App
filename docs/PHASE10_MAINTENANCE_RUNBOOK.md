# Faz 10 — yayın sonrası bakım planı

Bu dosya bakım hazırlığıdır; uygulama yayımlanmış veya bakım çalışmaları yapılmış sayılmaz. Sürekli çalışan izleyici/otomasyon kurulmamıştır.

1. NextDNS API belgesindeki değişiklikleri sürüm hazırlığında kontrol et; beklenmeyen alan, hata kodu, SSE ve export sözleşmelerini fixture'larla değerlendir. API değişimi kullanıcının gerçek ayarlarını sessizce değiştirmemelidir.
2. Üretim bağımlılıklarını aynı çözülmüş varyantta yeniden tara; SBOM, POM/NOTICE ve dağıtılan paket hash'lerini sürüm kanıtına bağla. Tarama hatasını başarı olarak kaydetme.
3. Düzeltmeleri geliştirme dalı, PR ve CI üzerinden al; ilgili B vakalarını yeni aday üzerinde yeniden çalıştır. `versionCode` Play'deki son gerçek sürümden yüksek olmalıdır; mevcut değer 1 hazırlık sürümüdür.
4. Koşullarda maddi değişiklik olduğunda belge revizyonunu artır, beş dilin karşılığını ve registry özetlerini güncelle; uygulama yeniden açık kabul ister. Yalnız dil değişimi yeni kabul gerektirmez.
5. Internal/closed test ve prelaunch bulgularını inceleyip kademeli yayını son yayıncı kararıyla başlat. API kesintisi, yanlış profil işlemi veya hassas veri sızıntısında rollout'u durdurma/düzeltme sürecini aynı vaka kayıtlarıyla izle.

Bu plan belirli bakım aralığı, SLA, kesintisiz API uyumluluğu veya kullanıcıya ek garanti taahhüdü oluşturmaz.
