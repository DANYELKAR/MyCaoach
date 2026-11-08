# MyCoach iOS yayın hazırlığı

Yerel fazlar ile canlı yayını ayrı doğrula. Development Supabase DB/Auth/Storage backend üzerinden doğrulandı; aşağıdaki production ve iOS sağlayıcı kapıları açıktır. SMTP/domain kullanıcı kararıyla ertelendi.

## Derleme ve yerel kabul

- [x] iOS 17+, iPhone, SwiftUI; SDK sürümleri ve shared scheme kilitli.
- [x] Onboarding, yaş ve AI onayı, yerel oturum ve backend CRUD/generation akışları.
- [x] Keychain, typed API errors, idempotent retry, multipart ve resim küçültme testleri.
- [x] Kredi ve Plus kararları backend'de; yerel test aboneliği açıkça etiketli.
- [x] Report, privacy toggles, geçmiş ve hesap silme kullanıcı akışları.
- [x] No tracking; gerekli UserDefaults nedeni ve kullanıcı içeriği kategorileri privacy manifest'te.
- [x] Açık/koyu görünüm, Dynamic Type, 44pt aksiyonlar; normal ve büyük yazı görsel QA.

Son test/arşiv sonuçları [verification.md](verification.md) içinde kayıtlıdır. Arşiv komutu:

```sh
DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer xcodebuild archive \
  -project MyCoach/MyCoach.xcodeproj -scheme MyCoach -configuration Release \
  -destination 'generic/platform=iOS' -archivePath /tmp/MyCoach-verified.xcarchive \
  -clonedSourcePackagesDirPath /tmp/mycoach-spm -disableAutomaticPackageResolution \
  CODE_SIGNING_ALLOWED=NO
python3 scripts/check_ios_release.py /tmp/MyCoach-verified.xcarchive/Products/Applications/MyCoach.app
```

İmzasız arşiv, TestFlight'a yüklenebilir imzalı bir build değildir. Preflight varsayılan placeholder yapılandırmayı bilerek reddeder.

## Hesaplar ve güvenli yayın ayarları

- [x] Development Supabase: TLS/JDBC, V1–V5 Flyway, 12 RLS tablosu, private Storage, gerçek JWT/refresh/izolasyon ve backend hesap silme doğrulandı. [Kanıt](../infrastructure/supabase-runtime-verification.json).
- [ ] Production HTTPS Spring Boot sunucusu, ayrı production DB ve backup/restore. Docker Desktop tek başına production hosting değildir.
- [ ] Supabase development/production projeleri; Apple ve Google provider ayarları, email OTP template'i kod gösterecek şekilde, SMTP/domain teslimatı, JWT issuer/audience.
- [ ] Google iOS bundle ID ve reversed client ID scheme; Apple Developer takımında Sign in with Apple capability/provisioning. Uygulama entitlement dosyası hazırdır.
- [ ] Gerçek Gemini model/key, görsel ve mesaj onayı, blocked/timeouts, retry ve maliyet sınırları.
- [ ] RevenueCat `plus` entitlement, haftalık/aylık/yıllık ürünler ve offerings; App Store Connect ürünleri, trial eligibility ve fiyat/dönem açıklamaları. Fiyatlar uygulamada sabitlenmez.
- [ ] Sandbox purchase, restore, cancellation/expiry ve hesap değişimi; backend HTTPS webhook + secret, `/billing/sync`, Free'ye düşüşte kilitler.
- [ ] Publisher yasal kimliği, destek e-postası ve çalışan HTTPS Terms/Privacy URL'leri. Uygulamadaki development metinleri final politika yerine geçmez.
- [ ] Signed device test: kamera/PhotosPicker, Apple/Google/OTP, arka plan/foreground token refresh, ağ kesintisi, satın alma ve hesap silme.

## App Store Connect / TestFlight

- [ ] Bundle ID `com.danyelkar.mycoach.MyCoach` için doğru signing team, provisioning, distribution certificate ve App Store Connect uygulaması.
- [ ] Encryption/export compliance yanıtları kullanılan şifreleme ve SDK'lara göre yayıncı tarafından doğrulanır.
- [ ] Yaş derecelendirme formu gerçek içerik/özelliklere göre doldurulur; uygulamadaki 18+ kapısı mağaza derecelendirmesi yerine geçmez. Bold PG-13 kalır.
- [ ] App Privacy: hesap adı/e-posta/user ID, paylaşılan fotoğraf ve diğer kullanıcı içeriği, satın alma geçmişi; işlevsellik amacı, identity bağlantısı ve SDK'ların gerçek davranışıyla birlikte doğrulanır. No-op analytics kullanılır; analytics provider eklenirse beyanlar güncellenir. ATT/tracking yok.
- [ ] Subscription metadata: ad/açıklama, period/price/trial, grup/ürün lokalizasyonları, review screenshot, auto-renew disclosure, Restore ve Manage Subscription.
- [ ] Ekran görüntüleri: Welcome/consent, Home, Opener/results, People/detail, History, Saved, paywall/subscription, Style/Tones, Privacy. Simulator QA görselleri mağaza materyali olarak otomatik kullanılmaz.
- [ ] Review notes: test hesapları, AI paylaşımı ve report yolu, Plus test erişimi, silme yolu, backend erişimi. Yerel fixture ekranları mağazaya gönderilmez; Release'te local mode kapalıdır.
- [ ] Önce internal TestFlight; sonra kontrollü external test. Crash, purchase/restore, offline, accessibility, silme ve üretim kontrolleri yeniden yapılır. Kullanıcı verisi loglara eklenmez.

Kaynaklar: [Apple privacy manifest](https://developer.apple.com/documentation/BundleResources/describing-data-use-in-privacy-manifests), [Apple collected data types](https://developer.apple.com/documentation/bundleresources/app-privacy-configuration/nsprivacycollecteddatatypes/nsprivacycollecteddatatype), [Supabase native ID token](https://supabase.com/docs/reference/swift/auth-signinwithidtoken), [RevenueCat restore](https://www.revenuecat.com/docs/getting-started/restoring-purchases).
