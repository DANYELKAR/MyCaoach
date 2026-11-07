# MyCoach çalışma kuralları

Ürün ve API brief'i: `files/spark_codex_prompts_ios.md`. İlgili faza başlamadan önce bu dosyayı ve `docs/backend/plan.md` belgesini oku. Tasarım referansları `design_files/` altında, salt okunur kaynaktır.

## Kullanıcının son kararları

- Ürün adı MyCoach. Spark tasarım belgelerindeki eski çalışma adıdır; kullanıcıya görünen yeni metinlerde MyCoach kullan.

- Backend Java 21 + Spring Boot 3.x + Maven olacak; paket `com.spark.api`.
- Backend Docker Desktop üzerinde çalışacak. Vercel kullanılmayacak.
- Hedef veritabanı, Auth ve Storage sağlayıcısı Supabase. Yerel Compose PostgreSQL'i yalnız geliştirme/smoke test içindir.
- Mevcut iOS projesi `MyCoach/` altında; backend görevi kapsamında yeniden adlandırma veya taşıma yapma.
- Son kapsam: plandaki tüm backend modüllerini yerelde çalıştır ve unit/entegrasyon testleriyle doğrula. Fazları doğrulayarak sırayla ilerle.

## Uygulama kuralları

- Controller → service → repository; DTO/entity ayrımı, constructor injection, Bean Validation.
- İş endpoint'leri `/v1`; hata modeli `{code, message, requestId}`. İş API'leri gerçek JWT yapılandırılmadan erişime açılmaz.
- Kullanıcı sahipliği bütün sorgularda ve bağlı kayıt atamalarında doğrulanır. Plan/kota kararları backend'de verilir.
- Şemayı Flyway yönetir; Hibernate validate. Aynı tablolar için ikinci bir migration otoritesi kurma.
- Supabase RLS açık, mobil istemciye uygulama tablolarını doğrudan açan policy yok. Gizli anahtarlar yalnız backend ortamında.
- Görselleri, kullanıcı mesaj/notlarını, token ve secret değerlerini loglama veya repoya ekleme. `.env` gizlidir; örnek değerler `.env.example` içindir.
- Her fazda Maven verify ve ilgili Docker/entegrasyon kontrolleri; ürün iş kuralları eklendiğinde brief'teki servis/Testcontainers testleri.
- Çalışmayan, bağlanmamış veya yalnız tasarlanmış servisi tamamlandı olarak raporlama. Gerçek bulut proje ID'leri ve doğrulama durumunu `docs/infrastructure/status.md` içinde tut.
- Bildirim, gamification, date coaching kapsam dışında. Tasarım ile ürün brief'i çelişirse `docs/backend/plan.md` içindeki kararları uygula.
