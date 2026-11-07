# MyCoach iOS

Native SwiftUI uygulaması; mevcut Xcode projesi `MyCoach/MyCoach.xcodeproj` altında korunur. Ürün adı MyCoach, tasarımlardaki Spark önceki çalışma adıdır.

## Faz 0: uygulama temeli

- iPhone, minimum iOS 17, Swift 6, MVVM/Observation için AppState ve sekme bazlı NavigationStack.
- Home, People, History, Saved, Profile sekmeleri. İlk fazda People/History/Saved boş durumları gösterir; hesap ve sunucu verisi bağlı değildir.
- Warm Electric renkleri, Dynamic Type, SF Symbols, kartlar, butonlar, ton chip'leri ve boş/hata/toast bileşenleri.
- Yedi ton arasından seçilen varsayılan ton UserDefaults ile cihazda saklanır. Henüz backend profil tercihi olarak senkronize edilmez.
- Supabase, Google Sign-In ve RevenueCat Swift Package Manager bağımlılıkları; config ve istemci factory temeli. Paketlerin linklenmesi gerçek giriş/satın alma akışının tamamlandığı anlamına gelmez.
- PrivacyInfo.xcprivacy ve English String Catalog. Bu fazda analytics, tracking, fotoğraf yükleme veya AI çağrısı yoktur.
- Ortak Xcode scheme, unit/UI test target'ları ve macOS CI workflow.

## Çalıştırma

Xcode 16.3+ (Swift 6.1 paket çözümlemesi için), önerilen Xcode 26.2. Projeyi Xcode'da açıp MyCoach scheme'iyle bir iPhone Simulator seç.

Komut satırında sistem Xcode seçimi farklıysa global ayarı değiştirmeden:

```sh
export DEVELOPER_DIR=/Applications/Xcode.app/Contents/Developer
xcodebuild -resolvePackageDependencies -project MyCoach/MyCoach.xcodeproj -scheme MyCoach
xcodebuild test -project MyCoach/MyCoach.xcodeproj -scheme MyCoach \
  -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath /tmp/mycoach-ios-derived CODE_SIGNING_ALLOWED=NO
```

Gerçek cihaz derlemesi için Xcode'da kişisel signing team seçilmelidir. Simulator code signing gerektirmez.

## Yapılandırma

`MyCoach/Config/Debug.xcconfig` yerel HTTP localhost API adresini, Release HTTPS adresini kullanır. Release'teki `api.mycoach.example` yayın adresi değildir; gerçek HTTPS endpoint ile değiştirilmelidir. `Local.xcconfig.example` → `Local.xcconfig` kopyalanarak yerel override yapılabilir; gerçek override dosyası Git'e alınmaz.

Yalnız publishable/anon Supabase anahtarı, Google client ID ve RevenueCat public SDK anahtarı mobil config'e girebilir. Supabase secret/service_role, Gemini, RevenueCat server veya webhook secret'ı iOS uygulamasına eklenmez. Config parser, localhost dışındaki HTTP adreslerini ve Supabase server anahtarlarını reddeder. Debug ATS izni yalnız yerel ağ içindir; Release'te HTTP izni yoktur.

SDK pinleri: Supabase 2.55.6, GoogleSignIn 10.1.0, RevenueCat 5.102.3. Transitive sürümler `Package.resolved` ile sabitlenir.

## Sonraki fazlar

Önce bu temel derleme/test/Simulator ile doğrulanır. Sonra onboarding/yaş ve AI onayı, Keychain/session, Supabase Apple/Google/OTP ve yalnız Debug için mevcut yerel backend auth adaptörü bağlanır. Ardından PhotosPicker/kamera, JPEG küçültme, URLSession API istemcisi ve Opener sonuçları gelir. Satın alma/stil/bellek ekranları ürün brief'indeki sırayla ilerler.

[Doğrulama raporu](verification.md): 9 unit + 3 UI test ve Debug/Release Simulator build kontrolleri başarılı.
