# Backend yayın kontrolü

Bu belge yerel backend ile gerçek servis yayını arasındaki geçişi izler. Yerelde geçen testler, gerçek Apple/Google/RevenueCat/Gemini hesaplarının yayın doğrulaması yerine kullanılmaz.

## Yerel kapılar

- Maven format kontrolü, unit ve Testcontainers testleri.
- Docker Compose sağlık/readiness ve uçtan uca HTTP smoke.
- DB/Storage volume kalıcılığı ve restart sonrasında yeniden giriş.
- Kullanıcı izolasyonu, plan/kota yarışları, idempotency, hata/blocked kredi kuralları.
- Görsel tür/boyut kontrolü, private erişim, orphan cleanup, hesap silme ve eski JWT reddi.
- OpenAPI snapshot, README ve test/kurulum raporunun güncel olması.

Kanıtlar: `docs/backend/verification.md`.

## Gerçek servis yayını öncesi

- Tamamlandı (development): Supabase DB/Auth/private `inputs`, RLS/Flyway, gerçek JWT/refresh/izolasyon ve hesap silme. Production projesi ve production rol/yedek kontrolleri hâlâ açık.
- Apple, Google ve email OTP sağlayıcılarının Supabase Auth ayarları; SMTP/domain kullanıcı kararıyla ertelendi.
- Tamamlandı (development): Gemini Flash-Lite gerçek model erişimi ve sentetik opener/reply/style, replay/kredi/Storage temizliği. Temsili safety/quality benchmark, gerçek içerik için veri/policy şartları ve production kota/maliyet kabulü hâlâ açık.
- RevenueCat `plus` entitlement'ı, App Store sandbox purchase/restore/expiry ve public HTTPS webhook adresi.
- Gerçek hesap silme ile Auth/session/Storage/DB temizliğinin tekrar doğrulanması.
- Secret'ların yalnız sunucu ortamında tutulması, TLS/JDBC ve mobil HTTPS ayarları.
- Container OS paketi taraması ve Maven audit değerlendirmesinin güncellenmesi. Spring 3.x üzerinde raporlanan ancak kullanılmayan MVC özelliklerine ilişkin regresyon testi korunmalı; bu özellikler eklenirse sürüm/mitigation yeniden değerlendirilir.
- Kalıcı sunucu, yedekleme/geri yükleme denemesi, izleme ve webhook başarısızlık takibi.

## iOS yayın ekibi

Apple ile giriş, yaş/AI onayı, hesap silme, report, privacy manifest/App Privacy yanıtları, abonelik fiyat/dönem/trial açıklamaları, Restore Purchases, Terms/Privacy bağlantıları, erişilebilirlik, TestFlight ve App Store inceleme materyalleri iOS yayın kapsamındadır. Bu backend görevi Xcode projesini değiştirmez.
