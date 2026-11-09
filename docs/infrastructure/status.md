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
| Testler | 36 test, 0 failure/error/skipped; Supabase anahtar ve kapalı sağlayıcı regresyonları dahil Maven verify başarılı |
| E2E / restart | Gerçek Compose servisine karşı smoke ve DB/API restart testi geçti; smoke hesapları silindi |
| CI | Backend verify, Docker/HTTP smoke ve cleanup workflow'u hazır; PR oluşturulduğunda GitHub Actions sonucu ayrıca izlenecek |
| Supabase bulut projesi | `mycoach-dev`, ref `yeoqhvqfwiheemxufuru`, Free/Nano, Frankfurt `eu-central-1`, dashboard Healthy |
| Gemini / RevenueCat canlı erişim | Gemini Flash-Lite canlı opener/reply/style doğrulandı; RevenueCat kapalı ve canlı doğrulanmadı |

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
- iOS `Config/Local.xcconfig` (0600, Git dışında) yalnız proje URL'si ve publishable key içeriyor. Debug yerel backend akışında kaldı. Yerel mod Supabase istemcisi oluşturmaz; local/cloud oturumları birbirine karışmaz.
- Gerçek proje Auth API kontrolü: JWKS HTTP 200, bir ES256 anahtarı; publishable key ile `/auth/v1/settings` HTTP 200, email açık, Apple/Google kapalı. Kullanıcı oluşturulmadı veya e-posta gönderilmedi.
- Migration aracı ayrı, geçici PostgreSQL 17 üzerinde doğrulandı: 5 uygulama migration uygulandı ve validate geçti; 12 tabloda RLS, 7 built-in ton. Test container silindi; bulut verisine uygulanmış sayılmaz.
- Bulut PostgreSQL 17.11 bağlantısı TLS session pooler ile doğrulandı. Flyway 11.7.2 ile V1–V5 uygulandı, validate başarılı. Gerçek DB salt okunur sorguları: 5 başarılı / 0 başarısız migration, 12 RLS korumalı uygulama tablosu, 0 korumasız uygulama tablosu, 0 public schema policy, 7 built-in ton, `profiles → auth.users` foreign key mevcut. [Sayısal doğrulama kanıtı](supabase-verification.json). Daha sonra aşağıdaki gerçek runtime smoke ile iki sentetik hesap oluşturuldu ve silindi; gerçek kullanıcı e-postası/içeriği kullanılmadı.
- Mevcut opaque `sb_secret_...` anahtarlarını JWT Bearer olarak gönderen backend düzeltildi. Auth/Storage admin istekleri `apikey` kullanıyor; kullanıcı logout JWT'si ve legacy service-role JWT desteği korundu. Anahtar uyumluluğu ve kapalı entegrasyon regresyonları dahil 36 backend testi başarılı.
- Auth > Emails mevcut hesapta şablon düzenlemek için custom SMTP istiyor. Varsayılan Magic link or OTP şablonu yalnız bağlantı içeriyor; iOS kod girişi için SMTP sonrasında `{{ .Token }}` içeren şablon gerekli. E-posta gönderilmedi, SMTP veya Apple/Google sağlayıcıları yapılandırılmadı.

### Sıradaki adımlar

1. Tamamlandı: mevcut DB parolası özel `.env` dosyasında; gerçek bulut migration ve RLS doğrulaması başarılı.
2. Tamamlandı: Java 21 ile `python3 scripts/supabase_migrations.py info`, ardından `migrate` ve `validate` başarılı. Komut yalnız uygulamanın aynı Flyway sürümü/migration dosyalarını kullanır; Gemini/RevenueCat gerektirmez. `clean`/`baseline` açılmaz. SQL Editor'den DDL yapıştırma.
3. Tamamlandı: mevcut Supabase server secret özel `.env` dosyasında. Auth admin HTTP 200; private bucket, 5 MiB sınırı ve MIME listesi gerçek API yanıtıyla doğrulandı. Gerçek Java adaptörü üzerinden yükleme/sign/indirme/silme geçti. Sonraki runtime smoke gerçek Auth oluşturma, refresh, backend ban/logout/silme ve eski token reddini doğruladı.
4. SMTP/domain seçimi ve OTP şablonu, Apple/Google OAuth, Gemini ve RevenueCat ayarları ayrı ayrı tamamlanacak. SMTP/domain kullanıcı kararıyla ertelendi. AI ve billing anahtarları eksikken ilgili bayraklar açıkça `false`; bu endpoint'ler 503, billing webhook 401 döner. Diğer servisler gerçek Supabase ile çalışır; sahte anahtar veya bulut fixture kullanılmaz.

Ücretsiz AI seçenekleri ve veri/kota değerlendirmesi: [AI araştırması](../backend/ai-options.md). Gerçek sağlayıcı seçimi veya anahtarla inference testi henüz yapılmadı; ücretli plan açılmadı.

Flyway Maven kullanımı: [resmî dokümantasyon](https://documentation.red-gate.com/flyway/reference/usage/maven-goal). Supabase ücretsiz e-posta kısıtları ve SMTP: [Auth testleri](https://supabase.com/docs/guides/auth/testing-auth-flows), [SMTP](https://supabase.com/docs/guides/auth/auth-smtp).


## Gerçek Supabase runtime doğrulaması

`compose.supabase.yaml` container'ı `spark-supabase-api-1`, yalnız `127.0.0.1:8081` üzerinde healthy. Java servisinin gerçek Supabase DB/Auth/Storage bağlantısı çalışıyor. Yerel fixture API `8080` üzerinde ayrı kalır.

`AI_ENABLED=false`, `BILLING_ENABLED=false` özel `.env` içinde açıkça ayarlı. Gemini/RevenueCat çağrısı yapılmaz; bu modüller başarılı çıktı/abonelik üretmeden 503 döner. Özellikler varsayılan olarak etkin ve etkinse eksik config ile fail-fast davranışı korunur. Kapalı billing webhook, ortamda eski bir secret kalsa da 401 döner.

`python3 scripts/smoke_supabase.py --allow-test-accounts` gerçek ES256 Supabase oturumlarıyla geçti: eksik/bozuk JWT reddi, profil/ton/kota, kullanıcılar arası read/update/delete izolasyonu, Free kişi/klasör sınırları, kapalı billing'de Free planın korunması, kapalı AI isteği sonrası 3 kredinin ve boş Storage'ın korunması, gerçek refresh, backend üzerinden hesap silme/Auth kaydının kalkması, eski JWT/refresh reddi. İki sentetik hesap silindi; yeniden canlandırmayı engelleyen tombstone kayıtları tasarım gereği kalır. [Runtime kanıtı](supabase-runtime-verification.json).

Bu doğrulama iOS Apple/Google/email OTP teslimatı, gerçek AI veya App Store satın alımı değildir. SMTP/domain ertelendi; gerçek Gemini, RevenueCat/App Store ve Apple/Google hesap ayarları eksik. İnternete açık sunucu/HTTPS veya TestFlight yayını yapılmadı.

Son yerel regresyon: güncel Docker image build/recreate + HTTP smoke başarılı; backend 36/36, iOS 27 unit + 6 UI = 33/33, sıfır failure/skip. Release cihaz arşivi `/tmp/MyCoach-cloud-verified.xcarchive` başarılı (imzasız), sunucu secret/parola gömülmediği ve ATS koruması kontrol edildi. Yayın preflight eksik dış servis/yasal/HTTPS ayarlarını bilerek reddediyor; TestFlight hazır değildir.


## 9 Kasım 2026 — canlı Gemini

Kullanıcı MyCoach projesinin API anahtarını özel `.env` dosyasına ekledi. AI Studio'da `MyCoach` projesi (`gen-lang-client-0953333474`) ve `MyCoach Development` anahtarı **Free tier** olarak görüldü; billing veya ücretli plan açılmadı. Anahtarın kendisi okunabilir çıktıya, mobil uygulamaya veya Git'e konmadı.

Models list HTTP 200 ile erişilebilir. Önceki varsayılan `gemini-3.8-flash` iki küçük REST denemesinde 30 saniyede, gerçek Java adaptöründe 25 saniyede zaman aşımına uğradı. `gemini-3.5-flash-lite` sentetik probe'a 1.11s, gerçek Java opener adaptörüne 1.85s içinde yanıt verdi. Backend varsayılanı ve özel `.env` modeli Flash-Lite olarak değiştirildi. Bu küçük smoke ölçümü bir performans/kalite garantisi değildir.

Güncel private config: `AI_ENABLED=true`, `GEMINI_MODEL=gemini-3.5-flash-lite`, `BILLING_ENABLED=false`. `spark-supabase-api-1` yeniden oluşturuldu ve healthy; gerçek Gemini etkin, RevenueCat hâlâ kapalı. Önceki tarihli kapalı-AI runtime raporu tarihsel kanıt olarak korunur.

`python3 scripts/smoke_gemini.py --allow-test-account --free-tier-confirmed`: gerçek Supabase JWT + Spring Boot + Gemini + DB + private Storage üzerinden opener geçti. Beş farklı, en fazla 220 karakterlik öneri; yalnız bir kredi tüketimi; replay aynı sonucu döndürdü, değiştirilmiş içerik aynı idempotency key ile 409 aldı ve ek kredi harcamadı. Saklanan sentetik görsel byte eşitliğiyle indirildi. Test hesabı ve objeleri backend hesap silme akışıyla temizlendi; eski JWT reddedildi. Tam opener turu 4.82s. Ek olarak aynı gerçek Java adaptörünün reply (4 öneri) ve stil analizi alanları doğrulandı; bunlar Plus/RevenueCat satın alım testi değildir. [Gemini kanıtı](gemini-verification.json).

36 backend testi, 0 failure/error/skip; Docker image build/recreate başarılı. iOS kaynakları değişmedi; Debug uygulaması hâlâ yerel fixture API `8080` kullanıyor. Canlı Gemini `8081` Supabase API'sinde aktiftir. Native Apple/Google/OTP, SMTP/domain, App Store/RevenueCat, production HTTPS ve imzalı cihaz/TestFlight kapıları açık kalır.

Ücretsiz kullanım şartları nedeniyle yalnız oluşturulmuş düz PNG ve kurmaca yetişkin ilgi/mesaj örnekleri gönderildi; gerçek kişi fotoğrafı, özel sohbet veya kullanıcı içeriği gönderilmedi.
