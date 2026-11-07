# Spark (iOS) — Ekranlar Genel Özeti ve Bilgi Mimarisi & Akış Haritası

Bu doküman, **Spark (iOS) — AI Dating Wingman** uygulamasının tasarlanmış olan tüm 21 ekranının mimarisini, kullanıcı akışlarını, ekranlar arası veri transferini ve tasarım standartlarını özetler.

---

## 1. Genel Tasarım Sistemi ve Görsel Kimlik
- **Tasarım Sistemi:** Warm Electric (`{{DATA:DESIGN_SYSTEM:DESIGN_SYSTEM_1}}`)
- **Renk Modu:** Derin Karanlık Mod (`#141219` Surface, `#0f0d14` Container Lowest, `#ff4d7d` Rose/Neon Vurgu Degradeleri)
- **Tipografi & Standartlar:** SF Pro / Inter font hiyerarşisi, 8pt köşe yuvarlatma ritmi (Round Eight), Apple HIG uyumlu 44pt/52pt dokunma hedefleri ve iOS Dynamic Island / Safe Area boşlukları.

---

## 2. Projedeki Tüm Ekranların Özeti (21 Ekran)

### Grup A: Karşılama, Onboarding ve Aktivasyon (A1 – A6)
1. **A1 · Splash (`{{DATA:SCREEN:SCREEN_42}}`)**
   - **Kabuk:** `mobile_blank`
   - **İçerik:** Neon degrade logomark, "Spark" marka başlığı, "Say the right thing. Every time." sloganı ve gizlilik rozeti.
2. **A2 · Welcome 1/3 (`{{DATA:SCREEN:SCREEN_40}}`)**
   - **Kabuk:** `mobile_blank`
   - **İçerik:** Carousel slayt 1 — "Upload Any Profile Snap". OCR ve görsel analiz özelliğinin tanıtımı, progress dots ve Continue CTA'i.
3. **A2 · Welcome 2/3 (`{{DATA:SCREEN:SCREEN_38}}`)**
   - **Kabuk:** `mobile_blank`
   - **İçerik:** Carousel slayt 2 — "Smart, Playful Banter". 4 farklı vibe/ton motoru ve yapay zeka kalibrasyonu tanıtımı.
4. **A2 · Welcome 3/3 (`{{DATA:SCREEN:SCREEN_36}}`)**
   - **Kabuk:** `mobile_blank`
   - **İçerik:** Carousel slayt 3 — "Turn Chats Into Dates". Yüksek geri dönüş oranları ve güvenli flört asistanı vurgusu.
5. **A3 · Personalization 1/2 (`{{DATA:SCREEN:SCREEN_34}}`)**
   - **Kabuk:** `mobile_blank`
   - **İçerik:** Kullanıcının flört tarzı / ton seçimi (Witty & Banter, Flirty & Direct, Casual & Warm, Deep & Curious).
6. **A3 · Personalization 2/2 (`{{DATA:SCREEN:SCREEN_32}}`)**
   - **Kabuk:** `mobile_blank`
   - **İçerik:** Hedef platformlar (Hinge, Tinder, Bumble, Instagram) ve birincil flört hedefleri seçimi.
7. **A4 · Age & Privacy Consent (`{{DATA:SCREEN:SCREEN_30}}`)**
   - **Kabuk:** `mobile_blank`
   - **İçerik:** 18+ yaş doğrulaması, on-device işleme garantisi ve biyometrik veri güvenliği onay kutuları.
8. **A5 · Sign in (`{{DATA:SCREEN:SCREEN_28}}`)**
   - **Kabuk:** `mobile_stack`
   - **İçerik:** Apple HIG uyumlu birincil "Sign in with Apple" butonu, alternatif giriş yolları ve gizlilik şartları.
9. **A6 · Paywall (`{{DATA:SCREEN:SCREEN_26}}`)**
   - **Kabuk:** `mobile_stack` (kapatılabilir modal çarpısı ile)
   - **İçerik:** "Unlock Spark Plus", sınırsız ekran görüntüsü analizi, öncelikli GPU yanıt süresi, yıllık/haftalık paket seçimi ve App Store yasal onayları.

---

### Grup B: Ana Akış, Üretim ve Yanıt Motoru (B1 – B6)
10. **B1 · Home (`{{DATA:SCREEN:SCREEN_24}}`)**
    - **Kabuk:** `mobile_tab` (5 Sekmeli Tab Bar)
    - **İçerik:** Hızlı aksiyonlar ("New Opener" ve "Continue Chat"), son eşleşmeler karuseli, haftalık momentum grafiği ve son önerilen kancalar.
11. **B2 · Match Detail (`{{DATA:SCREEN:SCREEN_22}}`)**
    - **Kabuk:** `mobile_stack`
    - **İçerik:** Yüklenen profil ekran görüntüsünün görsel/metin analizi, profilden çıkarılan 4 mikro kanca ve hızlı öneriler.
12. **B3 · Opener Generator (`{{DATA:SCREEN:SCREEN_20}}`)**
    - **Kabuk:** `mobile_stack`
    - **İçerik:** Seçilen eşleşme için farklı tonlarda üretilen 5 açılış mesajı alternatifi, tek dokunuşla kopyalama ve favorileme.
13. **B4 · Continue a Chat (`{{DATA:SCREEN:SCREEN_18}}`)**
    - **Kabuk:** `mobile_stack`
    - **İçerik:** Devam eden bir sohbetin ekran görüntüsünü yükleme veya son mesajı yapıştırma alanı, bağlam/vibe seçicisi.
14. **B5 · AI Analyzing (`{{DATA:SCREEN:SCREEN_16}}`)**
    - **Kabuk:** `mobile_stack`
    - **İçerik:** Ekran görüntüsü OCR çözümlemesi, vibe eşleme ve yanıt üretim yükleme/bekleme animasyonu ekranı.
15. **B6 · Reply Results (`{{DATA:SCREEN:SCREEN_14}}`)**
    - **Kabuk:** `mobile_stack`
    - **İçerik:** Sohbeti sürdürmek için üretilen 4 kişiselleştirilmiş yanıt alternatifi, kopyalama ve sohbeti randevuya bağlama taktikleri.

---

### Grup C: Eşleşmeler ve Kişi Yönetimi (C1 – C3)
16. **C1 · People (`{{DATA:SCREEN:SCREEN_12}}`)**
    - **Kabuk:** `mobile_tab`
    - **İçerik:** Kayıtlı eşleşmeler listesi, filtreler (Talking, Date Planned, New), arama ve `+ Add Person` girişi.
17. **C2 · Person Detail (`{{DATA:SCREEN:SCREEN_10}}`)**
    - **Kabuk:** `mobile_stack`
    - **İçerik:** Kişi kartı (Elena), durum seçici, çıkarılan ilgi kancaları, AI strateji notu, geçmiş kopyalanan mesajlar ve özel notlar.
18. **C3 · Add Person (`{{DATA:SCREEN:SCREEN_8}}`)**
    - **Kabuk:** `mobile_modal` (Bottom Sheet)
    - **İçerik:** Ekran görüntüsüyle otomatik ayrıştırma (OCR), profil formu, vibe kalibrasyonu ve özel notlar.

---

### Grup D, E, F: Koleksiyon, Geçmiş ve Hesap (D1 – F1)
19. **D1 · Saved / Favorites (`{{DATA:SCREEN:SCREEN_6}}`)**
    - **Kabuk:** `mobile_tab`
    - **İçerik:** En yüksek geri dönüş oranına sahip (%89+) favori mesajlar, vibe etiketleri, özel mesaj ekleme alanı.
20. **E1 · History (`{{DATA:SCREEN:SCREEN_4}}`)**
    - **Kabuk:** `mobile_tab`
    - **İçerik:** Kronolojik üretim akışı (Today, Yesterday, This Week), kopyalanma durumları, başarı rozetleri ve retention bilgisi.
21. **F1 · Profile / Settings (`{{DATA:SCREEN:SCREEN_2}}`)**
    - **Kabuk:** `mobile_tab`
    - **İçerik:** Kullanıcı profili (Alex Mercer), Spark Plus üyelik kartı, AI wingman ayarları, On-Device gizlilik toggle'ları ve Face ID kilidi.

---

## 3. Kullanıcı Akış Haritası (Flow Diagram)

```
[A1 Splash] 
    └──> [A2 Welcome 1/3] ──> [A2 Welcome 2/3] ──> [A2 Welcome 3/3]
              └──> [A3 Personalization 1/2] ──> [A3 Personalization 2/2]
                        └──> [A4 Age & Privacy Consent]
                                  └──> [A5 Sign in with Apple]
                                            └──> [A6 Paywall] ──(Geç / Satın Al)──> [ANA UYGULAMA TAB BAR]
```

### Ana Sekmeler ve Alt Sayfa Akışları

```
┌────────────────────────────────────────────────────────────────────────────────────────┐
│                                 5 SEKME (TAB BAR) KABUĞU                               │
├──────────────┬──────────────┬──────────────────────────┬──────────────┬────────────────┤
│   B1 Home    │  C1 People   │  [+] Hızlı Aksiyonlar    │   D1 Saved   │   E1 History   │   F1 Profile   │
└──────┬───────┴──────┬───────┴──────────────────────────┴──────┬───────┴───────┬────────┴───────┬────────┘
       │              │                                         │               │                │
       ├──> [New Opener Akışı]                                  │               │                │
       │       │                                                │               │                │
       │       ├──> [B2 Match Detail] ──> [B3 Opener Generator]─┘               │                │
       │                                                                        │                │
       ├──> [Continue Chat Akışı]                                               │                │
       │       │                                                                │                │
       │       ├──> [B4 Continue Chat] ──> [B5 AI Analyzing] ──> [B6 Reply] ────┘                │
       │                                                                                         │
       ├──> [Kişi Yönetimi Akışı]                                                                │
       │       │                                                                                 │
       │       ├──> [C3 Add Person (Modal Sheet)] ──> [B3 / C2]                                  │
       │       └──> [C2 Person Detail] ──> [B4 Sohbet Devam / B3 Yeni Açılış]                    │
       │                                                                                         │
       └──> [Profil & Ayarlar] ──────────────────────────────────────────────────────────────────┘
               │
               ├──> [A6 Paywall Yönetimi]
               └──> [Gizlilik, Face ID & Persona Kalibrasyonu]
```

---

## 4. Ekranlar Arası Veri Transferi ve Durum Yönetimi
- **OCR ve Vibe Analizi (B2 / C3):** Yüklenen ekran görüntülerinden elde edilen kancalar (Hooks) doğrudan `B3 Opener Generator` ve `C2 Person Detail` alanlarına aktarılır.
- **Sohbet Hafızası (B4 / B5 / B6):** Devam eden sohbet analizi, üretilen mesajları `B6 Reply Results` ekranına gönderirken aynı anda `E1 History` sekmesine kronolojik girdi olarak kaydeder.
- **Favori Senkronizasyonu:** `B3`, `B6` ve `C2` ekranlarında kalbe dokunulan her öneri anında `D1 Saved` sekmesindeki koleksiyona düşer.
- **Persona Eğitimi:** `F1 Settings` ve `A3 Personalization` tercihlerindeki ton seçimleri, tüm üretim motorlarının (B3, B6) varsayılan yanıt stilini belirler.
