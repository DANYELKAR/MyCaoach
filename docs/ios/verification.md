# iOS Faz 0 doğrulaması

## Kapsam

Hello World projesi, MyCoach markalı SwiftUI uygulama temeline dönüştürüldü. Bu faz Home / People / History / Saved / Profile navigasyonu, yeniden kullanılabilir bileşenler, ton tercihi, SDK/config ve test altyapısını kapsar. People/History/Saved boş durum ekranlarıdır; gerçek hesap, sunucu listeleri ve mesaj üretimi bağlı değildir.

## Geçen kontroller

- Xcode 26.2 / Swift 6.2.3; iPhone 17 Pro, iOS 26.2 Simulator.
- Minimum deployment target iOS 17 ve iPhone cihaz ailesi.
- Üç doğrudan SDK pin'i ve transitive bağımlılıkların Package.resolved dosyası çözüldü; bütün SDK ürünleri app target'ına linklendi.
- Son test bundle: `mycoach-ios-phase0-final.xcresult`.
- **9 unit + 3 UI = 12 test başarılı**, 0 hata, 0 atlanan test.
- Debug Simulator test/build ve Release generic Simulator build başarılı. Signing kapalı; gerçek cihaz/App Store archive doğrulaması değildir.
- Derlenmiş Info.plist değerleri kontrol edildi: Debug localhost + yalnız local networking ATS; Release HTTPS ve HTTP istisnası yok. Her iki build MyCoach display name ve iOS 17 minimum değerini içerir.
- Unit kontroller: config URL/mode, public key eşleşmesi, Supabase secret/service-role reddi, tercih kalıcılığı/bozuk değer fallback, 7 ton/5 tab, SF Symbol varlığı ve 4.5:1 text kontrastı.
- UI kontroller: 5 tab geçişi, Home→People navigasyonu, Home/Profile ton senkronu, ton rehberi aç/kapat, accessibility XXXL yazı boyutunda tab erişimi.
- Normal Home ve accessibility Profile screenshot'ları görsel olarak incelendi. Büyük fontta dekoratif ton ikonları kaldırılarak satır çakışması giderildi; normal ikon/check ölçüleri sabitlendi.
- English String Catalog 49 kaynak anahtarı ve UserDefaults gerekçeli privacy manifest eklendi. Tracking/analytics/AI çağrısı bu fazda yoktur.

## Yakalanan runtime sorunu

Ton tercihi testinde AppState nesnesi kapanırken Swift 6.2 implicit MainActor deinit / TaskLocal runtime crash'i tekrarlandı. Açık boş deinit ile giderildi; task-local bağlamında 25 oluşturma/serbest bırakma döngüsü içeren regresyon testi eklendi. Bu test artık geçer.

Kaynak: [Swift runtime issue #88036](https://github.com/swiftlang/swift/issues/88036). Çözüm yalnız test atlama/async'e taşıma ile gizlenmedi; uygulama modelinde uygulanmıştır.

## Artifacts

- [Test özeti](test-results.json)
- [Home](screenshots/home.png)
- [Büyük yazı boyutu Profile](screenshots/profile-accessibility.png)
- [Çalıştırma ve config](README.md)

CI workflow macOS üzerinde Xcode seçer, pinned SDK grafiğiyle aynı shared scheme'i test eder ve xcresult artifact'ını kaydeder. Bu yerel rapor, GitHub Actions sonucunun yerine geçmez; PR check durumu ayrıca izlenir.

## Sonraki geliştirme

Onboarding, yaş/AI onayı, Keychain/session, Apple/Google/OTP ve Debug local-backend giriş akışı; ardından PhotosPicker/kamera, küçültme, URLSession, Opener ve sonuç ekranları. Release API domain'i `.example` placeholder'ıdır ve gerçek servis adresi değildir. Gerçek sağlayıcı anahtarları veya satın alma yapılandırması bu fazda eklenmedi.
