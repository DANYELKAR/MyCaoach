# MyCoach backend

Java 21, Spring Boot 3.5.16, Maven, PostgreSQL 17 ve Docker Desktop. iOS projesi `MyCoach/`, tasarımlar `design_files/`, backend `backend/` altındadır.

Ürün adı **MyCoach**. Tasarım/prompt dosyalarındaki Spark adı önceki çalışma adıdır. Mevcut `com.spark.api`, Maven artifact ve yerel Docker/DB kimlikleri veri ve kurulum uyumluluğu için teknik ad olarak korunmuştur.

## Yerelde başlatma

Docker Desktop açıkken proje kökünde:

```sh
docker compose up --build -d --wait
curl --fail http://localhost:8080/actuator/health
python3 scripts/smoke_backend.py
```

- API: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui/index.html`
- OpenAPI: `http://localhost:8080/v3/api-docs`
- API ve PostgreSQL container'ları healthcheck içerir. API yalnız `127.0.0.1` üzerinden yayınlanır; DB host portu açılmaz.
- PostgreSQL ve görseller ayrı Docker volume'larında saklanır. `docker compose down` verileri korur.

`local` profili gerçek RSA imzalı JWT, kalıcı kullanıcı/parola kaydı, PostgreSQL iş verileri ve private dosya deposu kullanır. **Gemini ve RevenueCat yerel adaptörleri test fixture'larıdır:** AI sonuçları deterministiktir; yerel abonelik değişimi App Store satın alımı değildir. Yerel auth/abonelik endpoint'leri `supabase` profilinde bulunmaz. Dış hesap anahtarı olmadan bütün yerel iş akışları çalışır.

## Yerel kullanıcı ve token

```sh
curl -X POST http://localhost:8080/local/auth/register \
  -H 'Content-Type: application/json' \
  -d '{"email":"developer@example.test","password":"replace-with-local-password"}'
```

Yanıt `userId`, `accessToken`, `expiresIn` içerir. Sonraki isteklere `Authorization: Bearer <accessToken>` ekle. Aynı bilgilerle `POST /local/auth/login` giriş yapar. Parola BCrypt ile saklanır; local token 1 saat geçerlidir. RSA anahtarı process belleğindedir; API yeniden başlatıldığında tekrar giriş gerekir. Kullanıcı, profil, mesaj ve yerel abonelik kayıtları restart sonrasında korunur.

İlk üretimden önce `PATCH /v1/me` ile `{"ageConfirmed":true,"aiConsent":true,"timezone":"Europe/Istanbul"}` gönder. Onay tarihlerini sunucu kaydeder. Profil üzerinden plan değiştirilemez.

Yerel Plus testi için JWT ile:

```text
PUT /local/billing/entitlement
{"active":true,"expiresAt":"2099-01-01T00:00:00Z"}
```

Yerel webhook Authorization değeri varsayılan `local-webhook-secret` olup yalnız geliştirme fixture'ıdır; gerçek secret değildir. `LOCAL_WEBHOOK_SECRET` ile değiştirilebilir. Production secret `REVENUECAT_WEBHOOK_SECRET` üzerinden zorunludur.

## API kapsamı

| Grup | Endpoint'ler |
| --- | --- |
| Profil/kota | `GET/PATCH/DELETE /v1/me`, `GET /v1/me/usage` |
| Üretim | `POST /v1/generations/opener`, `/reply`, `GET /v1/generations`, `GET /v1/generations/{id}` |
| Kişiler | `GET/POST /v1/contacts`, `GET/PATCH/DELETE /v1/contacts/{id}` |
| Geçmiş | `GET/DELETE /v1/threads`, `GET/DELETE /v1/threads/{id}` |
| Favoriler/klasörler | `GET/POST /v1/saved`, `PATCH/DELETE /v1/saved/{id}`, `GET/POST /v1/folders`, `PATCH/DELETE /v1/folders/{id}` |
| Sonuç geri bildirimi | `PATCH /v1/suggestions/{id}`, `POST /v1/suggestions/{id}/report` |
| Tonlar | `GET/POST /v1/tones`, `PATCH/DELETE /v1/tones/{id}` |
| Stil | `GET/PUT/DELETE /v1/style-profile`, `POST /v1/style-profile/analyze` |
| Abonelik | `POST /v1/billing/sync`, `POST /v1/billing/webhook` |

Opener multipart alanları: `image`, `toneId`, `note?`, `contactId?`, `idempotencyKey`. Reply alanları: `images` (1–3 dosya) **veya** `text`, `toneId`, `note?`, `contactId?`, `idempotencyKey`. JPEG/PNG, dosya başına 5 MB, toplam request 16 MB, kenar başına en fazla 4096 px. İstemci görseli 1024 px'e küçültmelidir. Opener 5, reply 4 öneri döndürür.

Saklanan görsele JWT ile `GET /v1/generations/{id}/images/{index}` üzerinden erişilir (index 0 tabanlı). Yerelde yetkili binary yanıt, Supabase profilinde 60 saniyelik signed URL'ye 303 yönlendirmesi döner; yanıt cache'lenmez.

Liste endpoint'leri destekledikleri yerde `page=0&size=50` kullanır; `size` üst sınırı 100. Kişiler `status`, `q`; geçmiş `type=OPENER|REPLY`; favoriler `folderId` filtresi kabul eder. Tam request/response şekilleri OpenAPI snapshot'ındadır.

Free: günlük 3 başarılı üretim, 3 kişi, 3 klasör, 7 günlük geçmiş görünürlüğü. Plus: günlük 100 üretim ve premium özellikler. Kredi yalnız başarılı üretimle tüketilir; eşzamanlı çağrılar DB kilidi ve süreli rezervasyonlarla korunur. Aynı anahtarla aynı istek tekrarında sonuç yeniden üretilmez; farklı içerik 409 döndürür. Geçmiş silme kota kaydını silmez; mesaj içeriği silinir. Hesap silme ilişkili verileri ve dosyaları kaldırır; eski JWT'yi reddetmek için yalnız subject UUID'sinden oluşan silme işareti tutulur.

Timezone, kota aşma amacıyla değiştirilemesin diye üretimden sonraki 24 saat içinde ve son timezone değişiminden sonraki 24 saat içinde değiştirilemez. Yaz/kış saati geçişleri yerel gün başlangıcına göre hesaplanır.

## Test ve doğrulama

Docker Desktop ve Java 21 gerekir; Maven Wrapper pakette bulunur:

```sh
cd backend
./mvnw -B --no-transfer-progress fmt:check verify
```

İsteğe bağlı restart doğrulaması: `SPARK_TEST_RESTART=1 python3 scripts/smoke_backend.py`. Bu komut yerel PostgreSQL ve API container'larını yeniden başlatır, veri/görsel/abonelik kalıcılığını ve yeniden girişi kontrol eder.

Bağımlılık taraması: `python3 scripts/audit_dependencies.py`. Sonuç `docs/backend/dependency-audit.json` dosyasına yazılır. Container OS paketleri bu Maven taramasına dahil değildir.

Suite; unit testleri, temiz PostgreSQL Testcontainers entegrasyonlarını, eşzamanlı kota/idempotency testlerini ve dış servisler için HTTP sözleşme testlerini kapsar. Integration testler Docker bulunmadığında sessizce atlanmaz. Dockerfile imajı paketler; testler host/CI üzerinde çalışır, imaj içine Docker socket bağlanmaz.

Çalışan Compose servisini test etmek için proje kökünde `python3 scripts/smoke_backend.py` çalıştır. Script kendi test hesaplarını oluşturur/siler; hiçbir token/parola basmaz ve yalnız loopback hedeflerine izin verir. OpenAPI snapshot'ını `docs/api/openapi.json` olarak günceller.

```sh
docker compose ps
docker compose logs --tail=30 api
docker compose down
```

## Gerçek servis profili

`.env.example` → `.env` kopyala, gerçek değerleri doldur; `.env` repoya girmez:

```sh
docker compose down
docker compose -f compose.supabase.yaml up --build -d --wait
```

- Supabase DB host/kullanıcı adı panelin Session pooler JDBC bilgisinden alınır; TLS gerekir. `DATABASE_URL` bir JDBC URL'sidir; DB parolası ayrı alandadır.
- Uygulama migration'larını Flyway yönetir; Hibernate `validate`. Supabase Auth şeması uygulama tarafından oluşturulmaz. `db/local` migration'ları yalnız `local` profiline aittir.
- API JWT issuer/audience/imza/expiry doğrular. Asimetrik JWKS varsayılandır; legacy HS256 yalnız açık config ile desteklenir.
- Supabase projesinde private `inputs` bucket oluşturulmuş olmalı. Runtime JDBC rolü backend sahiplik kontrolleriyle RLS'i aşacak yetkiye ihtiyaç duyar; istemci anahtarlarıyla tablo erişimi açılmaz. RLS bütün iş tablolarında aktiftir.
- Gemini model adı `GEMINI_MODEL` ile değiştirilebilir; gerçek model erişimi ve anahtar hesap üzerinde ayrıca doğrulanır. Timeout 25s; 5xx için en fazla bir retry. Güvenlik filtreleri ve JSON schema gönderilir.
- RevenueCat `plus` entitlement'ı canonical subscriber API üzerinden doğrulanır. Webhook Authorization değeri secret ile birebir karşılaştırılır; event ID ve timestamp ile tekrar/sıralama korunur.
- Storage ve Auth admin secret'ları mobil istemciye verilmez. Hesap silme erişimi kapatır, session/refresh erişimini iptal eder, dosyaları ve Auth kaydını siler. Yarım kalan silmeler gece göreviyle tekrar denenir. Sahipsiz 24 saatlik objeler temizlenir.

Gerçek Supabase/Gemini/RevenueCat hesaplarına karşı canlı doğrulama yapılmadan production hazır iddiasında bulunulmaz. Docker Desktop internete açık deployment sağlamaz. iOS Simulator aynı Mac'te localhost kullanabilir; gerçek cihaz ve RevenueCat webhook için ayrıca erişilebilir HTTPS adresi gerekir.

## Belgeler

- [Backend planı](docs/backend/plan.md)
- [Kurulum durumu](docs/infrastructure/status.md)
- [Doğrulama raporu](docs/backend/verification.md)
- [OpenAPI snapshot](docs/api/openapi.json)
