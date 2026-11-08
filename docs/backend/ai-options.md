# MyCoach için ücretsiz AI API araştırması

Kontrol: 8 Kasım 2026. Resmî fiyat, kota ve veri kullanım belgeleri incelendi. Hesap açılıp API anahtarıyla gerçek çağrı yapılmadı; hız, kalite ve güvenlik çıktıları henüz karşılaştırılmadı. Ücretli plan veya faturalandırma etkinleştirilmedi.

## Sonuç ve önerilen sıra

Gemini Developer API'nin ücretsiz katmanı mevcut. MyCoach'un mevcut Spring Boot `AiClient` arayüzü ve Gemini adaptörü sayesinde, yapay/kişisel olmayan test verileriyle denemeye en yakın seçenek bu. Gerçek profil fotoğrafları ve özel sohbetler için ücretsiz sağlayıcı seçiminde veri koşulları belirleyici olmalı.

Öneri: önce Supabase development projesini ve bağlantısını tamamla; sonra ücretsiz bulut adayı olarak Groq'u, mevcut entegrasyonun karşılığı olarak Gemini'yi aynı anonim/yapay test setinde ölç. Tamamen yerel kullanım istenirse Ollama ayrı bir seçenek. Bu bir kalite benchmark sonucu değildir.

## Karşılaştırma

| Seçenek | Ücretsiz kapsam | MyCoach'a uyumu / sınırı |
|---|---|---|
| Gemini Developer API | Gemini 3.8 Flash ve 3.5 Flash-Lite için ücretsiz giriş/çıkış token katmanı var; etkin kota proje/model/hesaba göre AI Studio'da görülür. | Mevcut adaptör hazır; fotoğraf + metin ve JSON yanıtları. Standart unpaid veri şartları gerçek kişisel içerikle kullanım için uygun bir varsayılan değil. |
| Groq Cloud | `qwen/qwen3.8-27b` için yayımlanan Free tabanı: 30 istek/dakika, 1.000 istek/gün, 8.000 token/dakika, 200.000 token/gün. Kesin hak hesabın Limits ekranında doğrulanır. | Vision ve JSON mode; en çok 3 görsel. Her görsel 2.048 giriş token'ı sayılıyor. Token limiti, 1.000 isteğe ulaşmadan önce dolabilir. Yeni backend adaptörü ve çıktı/safety doğrulaması gerekir. |
| OpenRouter `:free` | Başlangıçta toplam 50 istek/gün ve 20 istek/dakika. 1.000/gün yükselişi geçmiş kredi alımı gerektirir; sıfır bütçe varsayımına dahil değil. | Görsel destekli ücretsiz model ayrıca seçilmeli. Model/sağlayıcı erişimi ve veri politikası değişebilir; ilk production varsayılanı olarak önermiyorum. |
| Ollama, yerel model | Yerel inference için sağlayıcıya çağrı başına ücret yok; Mac donanımı/elektrik kaynakları kullanılır. | Vision model + JSON schema ile yerel HTTP API mümkün. Mac açık kalır; paylaşılan RAM, hız, çıktı kalitesi ve model lisansı ölçülür. Bulut modeliyle karıştırılmaz. |

Kotalar uygulama kullanıcısı başına ek hak değildir. Gemini proje, Groq organizasyon, OpenRouter hesap kapsamındaki ortak limitler uygulanır. MyCoach'un günlük kullanıcı kredisi ile sağlayıcının token/istek kotası ayrı hesaplanır; stil analizi de sağlayıcı çağrısıdır.

## Veri koşulları

- Gemini: standart ücretsiz kullanımda gönderilen içerik ve yanıtlar ürün geliştirmede kullanılabilir; insan incelemesi mümkündür. Google unpaid servislere kişisel/gizli/hassas bilgi gönderilmemesini ister. EEA/İsviçre/UK için ayrı hükümler vardır; hesap bölgesi doğrulanmadan bunlara dayanılmaz. Paid veri koşulları farklıdır; billing açmak ücretsizliği koruyan genel bir çözüm olarak sunulmaz.
- Groq: inference girdileri/çıktıları varsayılan olarak saklanmaz; reliability/abuse istisnaları ve metadata vardır. Belgelerine göre bütün müşteriler Data Controls üzerinden Zero Data Retention açabilir. Bu ayar, hesap erişimi ve kullanım koşulları doğrulanmadan “hiç veri tutulmuyor” denmez.
- OpenRouter: OpenRouter ve seçilen sağlayıcının veri politikaları ayrı incelenir; training/logging ve ZDR routing seçenekleri kontrol edilir. Salt `:free` etiketi privacy garantisi değildir.
- Ollama: yerel model kullanılırsa inference verisi Mac üzerinde kalır. Cloud özellikleri `OLLAMA_NO_CLOUD=1` ile kapatılabilir; bulut modelleri bu yerel varsayıma dahil değildir.

## Uygulama planı

1. Ücretsiz Supabase projesi: `mycoach-dev`, mevcut `danyelkar` Free organizasyonu, Frankfurt. Form hazır; parola ve son oluşturma adımı kullanıcıya devredildi. Henüz proje ID'si veya bulut DB bağlantısı doğrulanmadı.
2. AI servis hesabı ve public/private config sınırı: gerçek AI anahtarı yalnız backend'in Git'e alınmayan `.env` dosyasında tutulur. iOS'a konmaz, loglanmaz, sohbette paylaşılmaz.
3. Test seti: kişisel olmayan tek fotoğraf, üç ekran görüntüsü yerine yapay sohbet görselleri, pasted text ve 5 stil örneği. Opener 5 / reply 4 farklı, en çok 220 karakterlik sonuç; JSON schema/validation, PG-13, hassas özellik çıkarsamama, block/timeout/429 ve tekrar denemede kredi/idempotency davranışı ölçülür.
4. Groq veya Ollama seçilirse `AiClient` arkasına ayrı adaptör eklenir; Spring Boot korunur. Otomatik ücretli fallback açılmaz. Kota aşımı kullanıcıya doğru hata olarak dönmeli; sağlayıcı kotası başarısız çağrı ve retry davranışlarıyla birlikte kontrol edilir.
5. Sağlayıcı değiştirilirse iOS AI onayı ve Privacy/Terms metinleri güncellenir. Mevcut metinler Google Gemini'yi adlandırıyor; yeni sağlayıcıya sessizce gerçek içerik gönderilmez.

Mevcut Mac'te 16 GB RAM görüldü. Küçük bir vision modelini yerelde denemek mümkün bir adaydır; Docker/Xcode/Simulator ile birlikte performans ve bellek ölçümü yapılmadan kapasite veya gecikme sözü verilmiyor. Ollama/model kurulmadı veya indirilmedi.

## Resmî kaynaklar

- [Gemini pricing](https://ai.google.dev/gemini-api/docs/pricing)
- [Gemini rate limits](https://ai.google.dev/gemini-api/docs/rate-limits)
- [Gemini additional terms](https://ai.google.dev/gemini-api/terms)
- [Groq Free model limits](https://console.groq.com/docs/rate-limits)
- [Groq vision ve JSON mode](https://console.groq.com/docs/vision)
- [Groq data controls / ZDR](https://console.groq.com/docs/your-data)
- [OpenRouter limits](https://openrouter.ai/docs/api/reference/limits)
- [OpenRouter data collection](https://openrouter.ai/docs/guides/privacy/data-collection)
- [Ollama vision](https://docs.ollama.com/capabilities/vision)
- [Ollama structured outputs](https://docs.ollama.com/capabilities/structured-outputs)
- [Ollama official FAQ / local-only privacy](https://github.com/ollama/ollama/blob/main/docs/faq.mdx)
