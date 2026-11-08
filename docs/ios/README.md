# MyCoach iOS

Swift 6 / SwiftUI / Observation, iPhone, iOS 17+. Mevcut proje `MyCoach/MyCoach.xcodeproj` altında. Tasarım referansları `design_files/` içinde salt okunur; Spark eski ürün adıdır.

## Yerelde çalışan akışlar

- Welcome/personalization, 18+ ve üçüncü taraf AI onayı; onaylar backend profilinde saklanır.
- Debug yerel hesap açma/giriş, Keychain oturumu, çıkış ve yeniden giriş. URLSession + Codable API, typed errors, Supabase token refresh/retry adaptörü.
- Fotoğraf seçimi/kamera adaptörü, arka planda ImageIO ile en fazla 1024 px / JPEG 0.8; Opener için 5, Continue Chat için 4 öneri. Kopyalama, kaydetme, feedback, gönderildi/yanıt durumu ve raporlama.
- People CRUD/detay, filtre/search, geçmiş ve timeline, Saved/klasörler/template düzenleme. Sayfalama, yenileme, boş/hata/yükleme durumları.
- Sunucudan kredi ve plan, Plus kapıları, paywall ve abonelik ekranı. Yerel test Plus/Free geçişi ücret oluşturmaz.
- Özel ton oluşturma/düzenleme/silme ve varsayılan ton; My Style örnekleri, analiz, kullanım toggle'ı ve silme.
- Privacy and Data tercihleri, geçmiş silme, görünüm/default tone/timezone ayarı, hesap silme ve temiz oturum dönüşü.
- Açık/koyu/sistem görünümü, Dynamic Type, erişilebilir etiketler, English String Catalog, privacy manifest, consent'e bağlı no-op analytics.

PR #2, backend PR #1 üzerine sıralanır; iOS branch checkout'u gerekli backend dosyalarını da içerir. Review sırası: backend #1 → iOS #2 → Supabase #3 → sonraki runtime düzeltmeleri. PR'lar otomatik birleştirilmez.

## Çalıştırma

Xcode 26.2 önerilir. Docker Desktop açıkken proje kökünde:

```sh
docker compose up -d
open MyCoach/MyCoach.xcodeproj
```

MyCoach scheme'i ve bir iPhone Simulator seç. Debug localhost:8080 API'sine bağlanır. Onboarding sonunda **local development** hesabı oluştur; parola 10–72 UTF-8 byte olmalıdır. Local AI çıktıları deterministik test verisidir. Subscription ekranındaki test Plus bir App Store satın alımı değildir. Simulator Mac'in localhost API'sini kullanır; gerçek cihaz HTTP/LAN erişimi bu yapılandırmada açılmaz.

Testler, Simulator'da Keychain için ad-hoc imza kullanır; Apple hesabı gerekmez:

```sh
bash scripts/test_ios.sh
MYCOACH_RUN_LOCAL_E2E=1 bash scripts/test_ios.sh
```

İlk komut mock ağ/birim ve temel UI testlerini çalıştırır; iki Docker E2E testi açıkça skip olur. İkinci komut çalışan Docker API'sine karşı hesap/üretim/bellek/Plus/stil/gizlilik/silme akışlarını da çalıştırır. Testler benzersiz hesaplarını temizler. Sonuç paketi `/tmp/mycoach-tests-*.xcresult` altında. Global `xcode-select` değiştirilmez; script gerekirse `DEVELOPER_DIR` seçer.

## SDK ve canlı yapılandırma

SPM kilidi: Supabase **2.55.6**, GoogleSignIn **10.1.0**, RevenueCat **5.102.3**. Auth, Supabase session'ını özel Keychain storage ile saklar; Apple nonce/SHA-256, Google ID token ve email OTP kod akışları uygulanmıştır. RevenueCat kimliği, backend ile aynı küçük harfli Supabase UUID'sidir; plan backend `/billing/sync` yanıtından gelir. Otomatik attribution device identifier toplama kapalıdır.

`MyCoach/Config/Local.xcconfig.example` → `Local.xcconfig` kopyala; gerçek dosya Git'e alınmaz. Yalnız public Supabase key, Google iOS client ID / reversed scheme ve RevenueCat `appl_` public SDK key'i girilir. Sunucu `.env` dosyasına Gemini, service-role ve RevenueCat server/webhook anahtarları girer; mobil uygulamaya eklenmez.

Release varsayılan domain'i `api.mycoach.example` yayın adresi değildir. Release config eksikse uygulama güvenli biçimde yapılandırma ekranında durur; HTTP/local login üretime açılmaz. Gerçek HTTPS API, Supabase/Google/Apple ayarları, publisher support ve hosted Terms/Privacy URL'leri ayrıca gereklidir.

**Kullanıcı kararı: bulut projeleri henüz hazır değil.** Gerçek Apple/Google/OTP teslimatı, Gemini, RevenueCat/App Store sandbox purchase/restore ve imzalı cihaz/TestFlight doğrulaması tamamlandı olarak sunulmaz. Kamera gerçek iPhone üzerinde ayrıca denenmelidir. Yerel kabuller bunların yerine geçmez.

[Doğrulama](verification.md) · [Yayın kontrol listesi](release-checklist.md)


Development Supabase backend ayrı `127.0.0.1:8081` üzerinde gerçek DB/Auth/Storage ile doğrulandı; AI ve billing açıkça kapalıyken 503 döner. iOS Debug, tam yerel fixture akışında `8080` kullanır. Local.xcconfig içindeki public Supabase ayarları yerel oturuma dokunmaz. Bu backend testleri native Apple/Google/OTP veya gerçek satın alma testinin yerine geçmez. SMTP/domain ertelendi; [güncel altyapı durumu](../infrastructure/status.md).
