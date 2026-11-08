# MyCoach iOS doğrulama

8 Kasım 2026, Europe/Istanbul. Kullanıcı kararıyla bütün kalan **yerel** iOS fazları tamamlandı. Development Supabase backend DB/Auth/Storage doğrulandı; native Apple/Google/OTP ve AI/App Store doğrulaması hâlâ bekliyor. SMTP/domain kullanıcı kararıyla ertelendi.

## Sonuç

| Kontrol | Kanıt |
|---|---|
| iOS birim testleri | **27 geçti**, 0 failure/skip |
| iOS arayüz testleri | **6 geçti**, 0 failure/skip; ikisi çalışan Docker API'sine karşı |
| Son test paketi | `/tmp/mycoach-remaining-ios-fixed.xcresult`; sonuç: Passed |
| Ortam | Xcode 26.2 / Swift 6.2.3; iPhone 17 Pro / iOS 26.2 Simulator / arm64 |
| Bitiş | 2026-11-08T22:39:03.032000+03:00 |
| Release cihaz arşivi | `/tmp/MyCoach-cloud-verified.xcarchive`; **ARCHIVE SUCCEEDED**, iPhoneOS / iOS 17 minimum; **imzasız** |
| Derlenmiş Release metadata | MyCoach adı, privacy manifest ve app icon var; HTTP/ATS istisnası yok |
| Yayın preflight | Placeholder API ve eksik canlı ayarlar nedeniyle **beklenen şekilde başarısız**; [alan listesi](release-preflight.json) |
| Backend tekrar kontrolü | Java 21 + Maven `fmt:check verify`: **36 test geçti**, 0 failure/error/skip |
| Çalışan API HTTP smoke | Auth/profile/ownership, opener/replay, saved/feedback/report, timeline, billing, reply, style/tones/template/history: geçti; OpenAPI 26 path; test hesapları silindi/eski JWT reddedildi |
| SPM advisory sorgusu | 15 kilitli SwiftURL sürümü, OSV: **0 advisory eşleşmesi**; [kapsam ve sürümler](dependency-audit.json). Bu sorgu zafiyet bulunmadığının ispatı değildir. |

Makine tarafından okunabilir özet: [test-results.json](test-results.json). Xcode sonuç ve arşiv klasörleri Git'e eklenmez.

## Test kapsamı

Birim testleri: config/public key ve HTTPS sınırları; legacy service-role/RevenueCat server anahtarı reddi; bearer JSON API, tek token refresh/retry, typed quota hata ve boş DELETE; multipart tekrar eden image alanları; 1024 px resim işleme ve bozuk dosya reddi; Keychain yaz/güncelle/sil; ton tercihi; pagination/query; Plus gate ve server billing; üretimde aynı idempotency key ile retry, 5 öneri ve kredi sonucu, hatalı response ve iptal; Free'ye düşünce özel tonun kalkması; consent'e bağlı analytics; koyu/açık kontrast ve Swift 6.2 TaskLocal yaşam döngüsü regresyonu.

Docker E2E UI akışı: welcome/personalization → 18+/AI onayı → benzersiz yerel hesap → Free paywall aç/kapat → fotoğraf/ton/opener → 5 sonuç → kaydet/kopyala → kişi ekle → geçmiş → Saved ve klasör → sunucudan test Plus → metinle Continue Chat ve 4 sonuç → özel ton → 5 örnekle stil analizi ve sunucudaki özet → test Free'ye geri dön → Plus gate yeniden kilitli → privacy tercihi → DELETE onayıyla hesap silme → welcome. Ayrı ikinci test privacy toggle ve hesap silmeyi tek başına doğrular.

Temel UI testleri beş sekmeyi, ton senkronizasyonunu, rehber kapatmayı, en büyük Dynamic Type ve light appearance erişimini doğrular. E2E harness yalnız DEBUG'ta erişilir; test photos fixture'dır, üretim ve kayıtlar gerçek HTTP/backend/PostgreSQL yolunu kullanır. Sistem parola AutoFill penceresi E2E fixture'ında bastırılır; normal yerel parola alanı bu davranışı korur.

CI, macOS üzerinde mock/birim + temel UI testlerini çalıştırır; Docker erişimi olmayan CI'da iki E2E testi açıkça skip olur. Tam yerel 33 test için `MYCOACH_RUN_LOCAL_E2E=1 bash scripts/test_ios.sh` kullan. Simulator Keychain testleri ad-hoc imzalanır; Apple hesabı gerekmez.

## Faz kanıtları ve bulunan sorunlar

Faz 2: onboarding ve Opener, gerçek yerel hesap ve 5 sonuçla doğrulandı. Faz 3: kişi/Saved/klasör/geçmiş akışı; Faz 4: server plan/usage ve test Plus; Faz 5: 4 reply; Faz 6: custom tone ve stil özeti; Faz 7: privacy ve hesap silme; Faz 8: tam regresyon, kontrast, advisory sorgusu ve cihaz arşivi.

Doğrulamada bulunan sorunlar giderildi: Keychain için imzasız Simulator testleri, klavye odağı, sonuç ekranının eski scroll konumunda açılması, küçük kart dokunma alanları, toast hit-testing, SwiftUI container accessibility ID'lerinin alt aksiyonları ezmesi, eski pagination yanıtının yeni filtreyi ezmesi, Free'ye dönüşte özel ton seçimi ve saved lookup/write yarışı. Üretim iptali eski sonucu yeni girdiye yazmaz; resim hazırlama ve ağ işleri UI'dan ayrıdır.

[Swift 6.2 actor-deinit runtime regresyonu](https://github.com/swiftlang/swift/issues/88036) için açık deinit ve TaskLocal yaşam döngüsü testi korunur.

## Görsel QA

Normal Home, sonuç kartları, Saved, paywall, stil ve privacy; ayrıca en büyük erişilebilir yazı boyutunda açık Home/Profile ve koyu Profile görüntüleri incelendi. Büyük yazıda içerik kaydırılır; sekmeler ulaşılabilir kalır. Launcher için yeniden üretilebilir native vector 1024×1024 opak icon eklendi (`scripts/generate_app_icon.swift`).

[Home](screenshots/home.png) · [Opener](screenshots/opener-results.png) · [Saved](screenshots/saved.png) · [Paywall](screenshots/paywall.png) · [Style](screenshots/style.png) · [Privacy](screenshots/privacy.png) · [Light/large Home](screenshots/home-light-accessibility.png) · [Light/large Profile](screenshots/profile-light-accessibility.png)

## Açık dış servis kapıları

Apple/Google/email OTP teslimatı, iOS native Supabase oturum akışı, gerçek Gemini, RevenueCat/App Store sandbox purchase/restore/webhook, fiziksel kamera ve imzalı cihaz/TestFlight yayını **doğrulanmadı**. SDK adaptörlerinin derlenmesi bu kontroller yerine geçmez. Yerel Plus/AI fixture'ları ücret oluşturmaz ve gerçek sağlayıcı sonucu değildir.

Yayıncı kimliği, destek e-postası, hosted Terms/Privacy ve gerçek HTTPS API henüz yapılandırılmadı. Release mevcut placeholder'larla kullanıma açılmaz. İmzasız arşiv TestFlight'a yüklenebilir build değildir. [Yayın kapıları](release-checklist.md) tamamlanmadan mağazaya gönderme.


## Supabase sonrası regresyon

Local.xcconfig public Supabase ayarları içerirken yerel AuthService artık bulut istemcisi oluşturmaz; yerel oturum cloud restore/refresh ile değişmez. Bu ayrımı kapsayan birim testi eklendi, bundle config testi public yerel override ile uyumlu hale getirildi. Son tam çalışma: 27 unit + 6 UI = 33 Passed, 0 failure/skip; iki UI testi gerçek yerel Docker API akışını çalıştırdı. Release cihaz arşivi tekrar başarılı; derlenmiş uygulama gerçek sunucu anahtar/parolaları açısından tarandı, hiçbiri bulunmadı. ATS/HTTPS koruması ve privacy manifest doğrulandı. Yayın preflight eksik HTTPS API/policy, billing, Google ve destek ayarlarını beklenen şekilde reddediyor; Supabase public alanları artık geçerli.

Gerçek development Supabase backend `8081` üzerinde ayrıca doğrulandı: gerçek JWT/refresh, CRUD sahipliği, Free sınırları, kapalı AI'da kredi/görsel temizliği ve Auth/DB/Storage hesap silme. [Runtime kanıtı](../infrastructure/supabase-runtime-verification.json). Bu, native iOS sağlayıcı veya TestFlight doğrulaması değildir.
