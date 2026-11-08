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
| Testler | 33 test, 0 failure/error/skipped; 8 Kasım Supabase anahtar uyumluluğu dahil Maven verify başarılı |
| E2E / restart | Gerçek Compose servisine karşı smoke ve DB/API restart testi geçti; smoke hesapları silindi |
| CI | Backend verify, Docker/HTTP smoke ve cleanup workflow'u hazır; PR oluşturulduğunda GitHub Actions sonucu ayrıca izlenecek |
| Supabase bulut projesi | `mycoach-dev`, ref `yeoqhvqfwiheemxufuru`, Free/Nano, Frankfurt `eu-central-1`, dashboard Healthy |
| Gemini / RevenueCat canlı erişim | Adaptör/sözleşme testleri hazır; canlı anahtarlarla doğrulanmadı |

Container'lar çalışır bırakıldı. Yerel hesaplar `/local/auth/register` ve `/local/auth/login` ile kullanılabilir. Yerel AI ve abonelik fixture'ları gerçek sağlayıcı sonucu/satın alımı değildir; ayrıntılar README'dedir. Auth signing key restart'ta yenilendiği için tekrar giriş gerekir, DB verileri korunur.

[Doğrulama raporu](../backend/verification.md) bütün backend gereksinimlerinin dosya/test/çalışan servis kanıtlarını ve kalan dış servis doğrulamasını listeler. Supabase geliştirme projesi oluşturuldu; bulut migration ve canlı entegrasyon doğrulaması aşağıdaki eksik ayarları bekliyor. Gerçek secret ve DB parolası bu belgeye yazılmaz.

GitHub repository: `DANYELKAR/MyCaoach`. Ürün adı MyCoach; önceki Spark adı yalnız tarihsel tasarım referansları ve teknik namespace/yerel veri kimliklerinde korunur. Başlangıç iOS/tasarım içeriği main, backend değişiklikleri `feat/mycoach-backend` branch'inde review edilir.

## 8 Kasım 2026 — Supabase geliştirme projesi

Kullanıcı projeyi oluşturdu. Dashboard üzerinden doğrulanan bilgiler:

- Organizasyon: `danyelkar` (`ltwzrbxmjikxihtllloo`), Free.
- Proje: `mycoach-dev`, ref `yeoqhvqfwiheemxufuru`, Healthy, Nano.
- URL: `https://yeoqhvqfwiheemxufuru.supabase.co`.
- Bölge: Central EU (Frankfurt), `eu-central-1`.
- Session pooler: `aws-1-eu-central-1.pooler.supabase.com:5432`, veritabanı `postgres`, kullanıcı `postgres.yeoqhvqfwiheemxufuru`. JDBC TLS `sslmode=require`.
- Data API proje oluşturulunca açık gelmişti; kapatıldı, dashboard “Data API disabled” ile doğrulandı. Mobil istemciye tablo erişimi açılmadı.
- Private `inputs` bucket oluşturuldu; public kapalı, 5 MB, yalnız `image/jpeg` ve `image/png`, 0 policy. Gerçek backend `SupabaseAdminClient` ile sentetik 1×1 PNG yükleme, 60 saniyelik signed URL ve aynı byte içeriğiyle indirme başarılı; anonim public erişim reddedildi; test objesi silinip liste boşluğu doğrulandı.
- GitHub Supabase entegrasyonu açılmadı. Flyway tek uygulama şeması otoritesi.
- `.env` (0600, Git dışında) gerçek JDBC/URL/JWKS ayarlarıyla hazır. DB parolası kullanıcı tarafından eklendi; gerçek JDBC bağlantısı doğrulandı. Kullanıcı mevcut secret anahtarını `SUPABASE_SERVICE_KEY` alanına ekledi. Auth admin salt okunur erişimi ve Storage gerçek istekleri başarılı. Anahtar gösterilmedi, loglanmadı veya Git'e eklenmedi; yeni anahtar oluşturulmadı.
- iOS `Config/Local.xcconfig` (0600, Git dışında) yalnız proje URL'si ve publishable key içeriyor. Debug yerel backend akışında kaldı; bulut backend çalışıyor olarak sunulmuyor.
- Gerçek proje Auth API kontrolü: JWKS HTTP 200, bir ES256 anahtarı; publishable key ile `/auth/v1/settings` HTTP 200, email açık, Apple/Google kapalı. Kullanıcı oluşturulmadı veya e-posta gönderilmedi.
- Migration aracı ayrı, geçici PostgreSQL 17 üzerinde doğrulandı: 5 uygulama migration uygulandı ve validate geçti; 12 tabloda RLS, 7 built-in ton. Test container silindi; bulut verisine uygulanmış sayılmaz.
- Bulut PostgreSQL 17.11 bağlantısı TLS session pooler ile doğrulandı. Flyway 11.7.2 ile V1–V5 uygulandı, validate başarılı. Gerçek DB salt okunur sorguları: 5 başarılı / 0 başarısız migration, 12 RLS korumalı uygulama tablosu, 0 korumasız uygulama tablosu, 0 public schema policy, 7 built-in ton, `profiles → auth.users` foreign key mevcut. [Sayısal doğrulama kanıtı](supabase-verification.json). Kullanıcı hesabı oluşturulmadı; daha sonra yalnız sentetik Storage testi yapılıp temizlendi.
- Mevcut opaque `sb_secret_...` anahtarlarını JWT Bearer olarak gönderen backend düzeltildi. Auth/Storage admin istekleri `apikey` kullanıyor; kullanıcı logout JWT'si ve legacy service-role JWT desteği korundu. İki ek regresyon testi dahil 33 test başarılı.
- Auth > Emails mevcut hesapta şablon düzenlemek için custom SMTP istiyor. Varsayılan Magic link or OTP şablonu yalnız bağlantı içeriyor; iOS kod girişi için SMTP sonrasında `{{ .Token }}` içeren şablon gerekli. E-posta gönderilmedi, SMTP veya Apple/Google sağlayıcıları yapılandırılmadı.

### Sıradaki adımlar

1. Tamamlandı: mevcut DB parolası özel `.env` dosyasında; gerçek bulut migration ve RLS doğrulaması başarılı.
2. Tamamlandı: Java 21 ile `python3 scripts/supabase_migrations.py info`, ardından `migrate` ve `validate` başarılı. Komut yalnız uygulamanın aynı Flyway sürümü/migration dosyalarını kullanır; Gemini/RevenueCat gerektirmez. `clean`/`baseline` açılmaz. SQL Editor'den DDL yapıştırma.
3. Tamamlandı: mevcut Supabase server secret özel `.env` dosyasında. Auth admin HTTP 200; private bucket, 5 MiB sınırı ve MIME listesi gerçek API yanıtıyla doğrulandı. Gerçek Java adaptörü üzerinden yükleme/sign/indirme/silme geçti. Kullanıcı oluşturma/silme veya ban/logout canlı testi yapılmadı.
4. SMTP/domain seçimi ve OTP şablonu, Apple/Google OAuth, Gemini ve RevenueCat ayarları ayrı ayrı tamamlanacak. Bunlar hazır olmadan `supabase` profilini anahtar yerine sahte değerlerle başlatma.

Ücretsiz AI seçenekleri ve veri/kota değerlendirmesi: [AI araştırması](../backend/ai-options.md). Gerçek sağlayıcı seçimi veya anahtarla inference testi henüz yapılmadı; ücretli plan açılmadı.

Flyway Maven kullanımı: [resmî dokümantasyon](https://documentation.red-gate.com/flyway/reference/usage/maven-goal). Supabase ücretsiz e-posta kısıtları ve SMTP: [Auth testleri](https://supabase.com/docs/guides/auth/testing-auth-flows), [SMTP](https://supabase.com/docs/guides/auth/auth-smtp).
