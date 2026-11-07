# Kurulum durumu

7 Kasım 2026, Europe/Istanbul. Vercel kullanıcı kararıyla iptal edildi; Vercel projesi oluşturulmadı.

| Kaynak | Doğrulanmış durum |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5.16, Maven; bütün planlanan backend modülleri yerel profile bağlı |
| Docker Desktop / Compose | Daemon 29.1.3, Compose v5.0.0-desktop.1 |
| API container | `spark-local-api-1`: healthy, `127.0.0.1:8080` |
| DB container | `spark-local-postgres-1`: healthy; PostgreSQL 17.11, DB `spark` |
| Health / readiness | 200 / UP |
| Swagger / OpenAPI | Yerelde açık; snapshot 26 path |
| İş şeması | 7 başarılı Flyway migration; 12 tabloda RLS açık; 7 built-in ton |
| Storage | Private yerel volume; owner JWT ile erişim; restart sonrası dosya kalıcılığı doğrulandı |
| Testler | 31 test, 0 failure/error/skipped; format kontrolü başarılı |
| E2E / restart | Gerçek Compose servisine karşı smoke ve DB/API restart testi geçti; smoke hesapları silindi |
| CI | Backend verify, Docker/HTTP smoke ve cleanup workflow'u hazır; PR oluşturulduğunda GitHub Actions sonucu ayrıca izlenecek |
| Supabase bulut projesi | Bu görevde oluşturulmadı; gerçek proje ID/URL bağlantısı doğrulanmadı |
| Gemini / RevenueCat canlı erişim | Adaptör/sözleşme testleri hazır; canlı anahtarlarla doğrulanmadı |

Container'lar çalışır bırakıldı. Yerel hesaplar `/local/auth/register` ve `/local/auth/login` ile kullanılabilir. Yerel AI ve abonelik fixture'ları gerçek sağlayıcı sonucu/satın alımı değildir; ayrıntılar README'dedir. Auth signing key restart'ta yenilendiği için tekrar giriş gerekir, DB verileri korunur.

[Doğrulama raporu](../backend/verification.md) bütün backend gereksinimlerinin dosya/test/çalışan servis kanıtlarını ve kalan dış servis doğrulamasını listeler. Gerçek proje erişimi geldiğinde organizasyon/proje/kota kontrol edilir; bu uygulamaya ait proje yeniden kullanılır veya uygun bölgede `spark-dev` oluşturulur. Gerçek secret ve DB parolası bu belgeye yazılmaz.

GitHub repository: `DANYELKAR/MyCaoach`. Ürün adı MyCoach; önceki Spark adı yalnız tarihsel tasarım referansları ve teknik namespace/yerel veri kimliklerinde korunur. Başlangıç iOS/tasarım içeriği main, backend değişiklikleri `feat/mycoach-backend` branch'inde review edilir.
