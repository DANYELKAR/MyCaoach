# MyCoach iOS

- Ürün adı MyCoach. `../files/spark_codex_prompts_ios.md` ürün/API ve faz kaynağı, `../design_files/` salt okunur tasarım referansıdır.
- Mevcut `MyCoach.xcodeproj` ve SwiftUI uygulamasını geliştir; iPhone / iOS 17+, Swift 6, Observation/MVVM, sekme başına NavigationStack.
- Renk/typography/bileşenler `MyCoach/Core/DesignSystem` üzerinden; Dynamic Type, 44pt dokunma hedefi ve erişilebilir etiketleri koru.
- SDK sürümleri ve Package.resolved kilitlidir. Gerçek Auth, URLSession API ve StoreKit/RevenueCat akışları ilgili fazda tamamlanır; dependency wiring'i çalışan entegrasyon olarak raporlama.
- Mobil config'e yalnız public client key/client ID girer. Service-role, Gemini, RevenueCat server veya webhook secret ekleme. Local.xcconfig Git'e alınmaz.
- Xcode seçimi CLT'ye işaret ediyorsa global xcode-select ayarını değiştirmeden komutlarda `DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer` kullan.
- Shared MyCoach scheme'inde unit ve UI testlerini çalıştır. Sonuç/QA belgeleri `../docs/ios/` altındadır.
- AppState'in açık deinit'i Swift 6.2 / eski iOS runtime crash workaround'udur. Regression testi geçmeden kaldırma: https://github.com/swiftlang/swift/issues/88036
- Faz 2–8 yerel akışları bağlıdır. Debug Docker auth/AI/billing fixture kullanır; bunları canlı Supabase/Gemini/App Store doğrulaması olarak raporlama. Kullanıcı bulut hesaplarının henüz hazır olmadığını belirtti.
- Testleri `MYCOACH_RUN_LOCAL_E2E=1 bash scripts/test_ios.sh` ile monorepo kökünden veya README komutlarıyla çalıştır. Simulator Keychain testleri ad-hoc imza gerektirir; CODE_SIGNING_ALLOWED=NO kullanma.
- `-ui-testing` ve `-e2e` yalnız DEBUG test harness'ıdır. E2E yerel parola alanı, sistem AutoFill penceresinden bağımsız test için oneTimeCode content type kullanır. Normal uygulama parola alanı değişmez.
- Yayın kapıları ve imzasız arşiv sınırı `../docs/ios/release-checklist.md` içinde. Gerçek key, hosted policy ve signing olmadan TestFlight hazır deme.
