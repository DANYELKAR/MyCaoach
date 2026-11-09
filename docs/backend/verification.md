# Yerel backend doğrulama raporu

7 Kasım 2026, Europe/Istanbul. Kapsam: `docs/backend/plan.md` ve ürün brief'indeki **backend** modüllerinin Docker Desktop üzerinde yerel çalışması. iOS geliştirmesi, production barındırma ve gerçek sağlayıcı hesaplarındaki canlı doğrulama bu raporda tamamlandı olarak gösterilmez.

## Son sonuç

- Java 21 / Spring Boot 3.5.16 / PostgreSQL 17.11.
- Maven verify: **36 test, 0 failure, 0 error, 0 skipped**.
- Format kontrolü başarılı; kapalı entegrasyonlar ve webhook reddi için regresyon testleri eklendi.
- Docker image build ve `docker compose up --build -d --wait` başarılı.
- `spark-local-api-1`, `spark-local-postgres-1`: healthy.
- Health, readiness ve Swagger UI HTTP 200.
- Çalışan container'a karşı smoke + DB/API restart testi başarılı; oluşturulan smoke hesaplarının kalan sayısı 0.
- OpenAPI snapshot: 26 path; bütün beklenen backend endpoint grupları mevcut.
- Flyway: V0, V0.1, V1–V5 başarılı. 12 iş/kontrol tablosunda RLS açık olduğu doğrudan PostgreSQL sorgusuyla doğrulandı. Yedi built-in ton seed'i entegrasyon testinde doğrulandı.

## Gereksinim / kanıt eşlemesi

| Gereksinim | Kanıt |
| --- | --- |
| Java 21, Spring Boot, Maven, Docker, JPA/Flyway | `pom.xml`, `Dockerfile`, Compose; son verify/image build ve canlı health |
| Temiz DB şeması, UUID/FK/CHECK, seed, RLS | `FoundationIntegrationTest`, çalışan DB'deki Flyway/RLS sorguları |
| JWT imza, issuer, audience, expiry, legacy HS256 | `JwtValidationTest`; yerel login/restart smoke ve `SecurityConfigurationTest` |
| İlk çağrıda profil, onaylar, timezone ve tercihler | `FoundationIntegrationTest`; HTTP smoke PATCH/GET |
| IANA yerel gün / DST / plan süre sonu | `UsageServiceTest`, `MemoryIntegrationTest` timezone değiştirme engeli |
| Eşzamanlı kota, PENDING rezervasyon, eski denemeyi engelleme | `GenerationIntegrationTest.concurrentRequestsCannotOverspend`, `expiredAttemptCannotCommitOrFailANewerReservation` |
| Idempotency replay/farklı içerik conflict; AI başarısız/blocked kredi tüketmez | `GenerationIntegrationTest.replayConflictAndFailureCreditRules`; canlı HTTP replay |
| Opener 5, reply 4, 220 karakter, unique öneriler | `OutputValidatorTest`, generation ve Plus HTTP testleri, çalışan backend smoke |
| Versiyonlu prompt, kişi/geçmiş/feedback/stil bağlamı | `PromptBuilder`, `OutputValidatorTest` prompt kontrolü, DB context sorgusu |
| Gemini REST schema/safety/image, tek 5xx retry ve 4xx hata saklama | `RemoteAdapterTest`; timeout kaynakta 25s olarak ayarlı |
| Kişi/favori/klasör CRUD ve kullanıcı izolasyonu | `MemoryIntegrationTest.peopleFoldersSavedFeedbackAndOwnership`; HTTP smoke |
| Free kişi/klasör limitleri ve 7 günlük geçmiş görünürlüğü | `MemoryIntegrationTest.freeLimitsAndHistoryDeletionCannotRefillCredits`, `oldHistoryRequiresPlus` |
| Timeline, feedback, sent/outcome, report | `MemoryIntegrationTest`, HTTP smoke |
| Geçmiş silerek kredi yenileyememe | `MemoryIntegrationTest`; restart smoke sonrası DELETE /threads ve değişmeyen usage |
| Plus gates, custom tones/templates, style analyze/delete | `MemoryIntegrationTest.plusUnlocksReplyStyleCustomTonesAndTemplates`; HTTP smoke |
| RevenueCat canonical entitlement, expiry ve server secret | `RemoteAdapterTest`, `MemoryIntegrationTest.billingReplayOrderingAndCancellation` |
| Webhook tekrar/sıralama ve cancellation dönem sonu | Billing entegrasyon testi; local canonical entitlement DB'de kalıcı |
| Görsel dosya doğrulama, private saklama/erişim | Generation multipart testi; account/image entegrasyon testi; canlı PNG smoke |
| Signed URL 60s, kullanıcı sahipliği, no-store | `RemoteAdapterTest.supabaseSignsPrivateImageAccessForOnlySixtySeconds`; canlı owner/non-owner image istekleri |
| İşlem sırasında saklama tercihi kapatılırsa dosya silme | `GenerationIntegrationTest.withdrawalOfImageRetentionDuringAiCallDeletesTheUploadedFile` |
| Hesap silme: DB/Auth/Storage ve eski JWT reddi | `MemoryIntegrationTest.accountDeletionRemovesAuthRowsAndFilesAndRejectsStaleToken`; HTTP smoke |
| Yarım kalan silmeyi yeniden deneme | `MemoryIntegrationTest.interruptedAccountDeletionCanBeRetriedWithTheSameToken`; retry job kodu |
| 24 saatlik sahipsiz obje temizliği | `MemoryIntegrationTest.cleanupRemovesOnlyOldUnreferencedObjects`; DB advisory lock kullanan cleanup servisi |
| Kullanıcı başına Bucket4j rate limit | `RateLimitServiceTest` |
| Request ID ve standart hata modeli | `SecurityConfigurationTest`, API entegrasyon/smoke hata istekleri |
| İç kalite metrikleri | `QualityMetricsTest`, anonim aggregate SQL ve zamanlanmış log görevi |
| Veri / görsel / yerel abonelik kalıcılığı | PostgreSQL ve API restart sonrasında yeni login, contact/image/entitlement HTTP kontrolleri |
| OpenAPI / kullanım belgeleri / CI tanımı | `docs/api/openapi.json`, `README.md`, `.github/workflows/backend.yml` |
| Dependency audit | `dependency-audit.json`: 103 runtime Maven bağımlılığı, iki değerlendirilmiş MVC duyurusu |

## Yeniden çalıştırılabilir komutlar

Proje kökünde:

```sh
./backend/mvnw -f backend/pom.xml -B --no-transfer-progress fmt:check verify
docker compose up --build -d --wait
SPARK_TEST_RESTART=1 python3 scripts/smoke_backend.py
python3 scripts/audit_dependencies.py
docker compose ps
```

Unit/integration testleri gerçek PostgreSQL Testcontainers üzerinde çalışır. HTTP sözleşme testleri yerel test sunucusu kullanır; hiçbir gerçek sağlayıcı anahtarı veya müşteri verisi gerekmez. Smoke script kendi hesaplarını temizler ve token/parola basmaz.

## Güvenlik taramasının sınırı

Boot BOM'dan gelen Tomcat, Jackson, Log4j, JDBC ve Commons Lang sürümleri uyumlu güvenlik sürümlerine yükseltildi. OSV taramasında kalan `GHSA-j9f9-w8pj-32f8` (SSE fragment) ve `GHSA-pc63-qcmh-9cmg` (XSLT implicit view) duyuruları Spring MVC 6.2.19 paketinde raporlanır. Mevcut uygulama bu özellikleri kullanmaz; `FoundationIntegrationTest.mvcDoesNotExposeSseFragmentsOrXsltViews` uygulama controller yüzeyini ve XSLT resolver yokluğunu kontrol eder. Bu, bağımlılığın yamalandığı iddiası değildir. Özellikler veya Spring sürümü değişirse değerlendirme yenilenmelidir. Kaynaklar audit JSON'dadır. Container OS paketleri ve test araçları bu Maven runtime taramasının kapsamında değildir.

## Gerçek sağlayıcılar için kalan dış doğrulama

Yerel çalışma `local` profilinde deterministik AI ve abonelik fixture'ları kullanır. Supabase DB/Auth/Storage ve Gemini Flash-Lite gerçek anahtarlarla sentetik development testlerinde doğrulandı; aşağıdaki güncellemeler ve altyapı kanıtları esas alınır. Apple/Google/SMTP, RevenueCat/App Store sandbox satın alma/restore ve gerçek cihaz kabulü ayrıca gerekir. GitHub Actions workflow hazırdır; repo `DANYELKAR/MyCaoach` olarak bağlandı. Yerel doğrulama sonucu ile PR üzerindeki GitHub Actions sonucu ayrı izlenir. HTTPS public webhook ve production deployment açılmadı.


## 8 Kasım — gerçek Supabase

Gerçek cloud migration/RLS, private Storage ve Java adapter testi, JWT/refresh/CRUD izolasyonu ve hesap silme [altyapı durumunda](../infrastructure/status.md) ve [runtime kanıtında](../infrastructure/supabase-runtime-verification.json) kayıtlıdır. AI ve billing açıkça kapalıyken core API çalışır; ilgili işlemler 503 döner, fixture sonuç/Plus verilmez. SMTP kullanıcı kararıyla ertelendi. Gerçek AI/RevenueCat/signed iOS yayını hâlâ ayrı kabul kapılarıdır.


## 9 Kasım — canlı Gemini

MyCoach Free tier anahtarı ile gerçek Java adaptörünün opener (5), reply (4) ve stil analizi sözleşmeleri geçti. Model varsayılanı, gerçek testte timeout sınırını aşan 3.8 Flash yerine `gemini-3.5-flash-lite` oldu. Gerçek Supabase JWT + Spring Boot opener, replay/conflict, tek kredi, private görsel ve hesap/Storage temizliği `scripts/smoke_gemini.py` ile doğrulandı. [Kanıt](../infrastructure/gemini-verification.json). Maven verify yeniden 36/36, 0 failure/error/skip. Gerçek kişisel içerik kullanılmadı, billing açılmadı.
