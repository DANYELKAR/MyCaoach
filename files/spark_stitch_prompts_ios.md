# Spark (iOS) — Google Stitch Prompt Dosyası

Bu dosya sadece Google Stitch içindir. Tüm prompt'lar İngilizce, çünkü uygulama içeriği İngilizce olacak.

## Kullanım

1. Stitch'te yeni bir **mobil (iOS)** proje aç, cihaz tipini telefon seç.
2. Aşağıdaki **Design System bloğunu** her prompt'un başındaki `[DESIGN SYSTEM BLOCK]` yerine yapıştır. Böylece tüm ekranlar aynı görsel dili kullanır.
3. Her prompt **tek bir ekran** üretir. Ekranları sırayla üret.
4. Sonuçları küçük, tek değişiklikli komutlarla iyileştir (ör. "Make the primary button larger").
5. Ekran kodları (A1, B3...) sadece takip içindir, Stitch'e yazma. Aynı kodları Codex dosyasında da kullanıyoruz.

**Ekran haritası:** A Onboarding (A1–A6) · B Opener ve Continue Chat (B1–B9) · C People (C1–C3) · D History (D1–D2) · E Saved (E1–E2) · F Tones (F1–F2) · G My Style (G1–G2) · H Profile ve Ayarlar (H1–H5) · I Sistem durumları (I1–I3)

**Navigasyon:** Alt tab bar'da 5 sekme: Home, People, History, Saved, Profile. Tones ve My Style, Profile sekmesinden açılır. Her özellik kendi ayrı sayfasındadır.

**iOS notları:** Sign in with Apple zorunlu olduğu için A5'te ilk sırada. A4'te Gemini'ye veri paylaşımı için açık onay var. A6 paywall'ı abonelik şartlarını (fiyat, süre, deneme, restore) gösterir. App Review bunları kontrol eder.

---

## Design System Bloğu (her prompt'un başına ekle)

```
Design an iOS mobile app screen (iPhone 15 Pro, portrait, 393x852pt, respecting the status bar, safe areas and home indicator) for 'Spark', an AI icebreaker assistant that helps adults (18+) write great first messages. Follow Apple Human Interface Guidelines and native iOS patterns.
Visual style: modern, warm, confident, slightly playful. Dark-first theme.
Colors: background #0F0D14, surface #1A1722, elevated surface #252233, primary #FF4D7D (rose), secondary #8B5CF6 (violet), primary gradient from #FF4D7D to #8B5CF6 used only for the main CTA and hero accents, label primary #F5F3FA, label secondary #A09BB0, success #34D399, warning #FBBF24, destructive #F87171, separator #3A3650.
Typography: SF Pro (system font), Dynamic Type friendly. Large Title 34 bold, Title 22 semibold, Headline 17 semibold, Body 17 regular, Subheadline 15, Footnote 13, Caption 12.
Shape and spacing: 8pt grid, 20pt continuous-corner cards, capsule buttons 52pt high, 16-20pt horizontal margins, minimum 44x44pt touch targets, generous whitespace.
Components: native iOS navigation bar with back chevron and large titles, bottom tab bar with 5 tabs (Home, People, History, Saved, Profile) on main tab screens only, capsule gradient primary buttons, bordered secondary buttons, capsule chips, segmented controls, native toggles, sheets with a grabber and detents, context menus, toast banners for feedback. No floating action buttons: use a navigation bar plus button or a bottom capsule button.
Icons: SF Symbols style, rounded, medium weight. No stock photos of real people; use abstract avatar placeholders with initials or soft gradient shapes.
Content rules: all text in natural, friendly American English. Short labels. No lorem ipsum. Accessibility: contrast 4.5:1 or higher, never rely on color alone.
```

---

## A — Onboarding

Best practice: önce değeri göster, akışı kısa tut, kişiselleştirmeyi atlanabilir yap, hesabı değer gösterildikten sonra iste, paywall'ı kapatılabilir tut.

### A1 · Splash

```
[DESIGN SYSTEM BLOCK]
SCREEN: Splash screen. Centered Spark logo mark (a small sparkle shape inside a rounded square with a rose-to-violet gradient), wordmark 'Spark' below it, tagline 'Say the right thing, first.' in secondary label color. A soft violet radial glow behind the logo on the dark background. No buttons and no progress bar.
```

### A2 · Welcome carousel (3 ayrı ekran)

```
[DESIGN SYSTEM BLOCK]
SCREEN: Welcome slide 1 of 3. Top right: 'Skip' text button. Top 45%: abstract hero illustration of a photo card with a sparkle and floating message bubbles in rose and violet. Headline 'Photo in, first message out'. Body 'Upload a photo, pick a vibe, and get 5 message ideas in seconds.' Bottom: iOS page control dots with the first dot active, then a full-width gradient capsule button 'Next'.
```

```
[DESIGN SYSTEM BLOCK]
SCREEN: Welcome slide 2 of 3. Same layout as slide 1. Illustration: two chat bubbles with a matching writing style and a small waveform. Headline 'Sounds like you'. Body 'Spark learns your style, so suggestions feel natural, not robotic.' Second dot active. Button 'Next'.
```

```
[DESIGN SYSTEM BLOCK]
SCREEN: Welcome slide 3 of 3. Same layout. Illustration: a stack of abstract contact cards with initials and a small heart. Headline 'Remember every conversation'. Body 'Keep profiles, history and favorites for everyone you talk to.' Third dot active. Button 'Get started'. Hide the Skip button on this slide.
```

### A3 · Personalization (2 ekran)

```
[DESIGN SYSTEM BLOCK]
SCREEN: Personalization step 1 of 2. Top: back chevron, thin progress bar at 50%, 'Skip' text button. Headline 'What are you looking for?'. Subtitle 'Pick all that apply. You can change this later.' Four large selectable cards in a vertical list, each with an SF Symbol icon, title and one-line description: 'New dates' (Meet someone special), 'Making friends' (Start friendly chats), 'Networking' (Professional first impressions), 'Just practicing' (Build confidence). Show 'New dates' selected with a gradient outline and checkmark. Bottom: gradient capsule 'Continue' button.
```

```
[DESIGN SYSTEM BLOCK]
SCREEN: Personalization step 2 of 2. Progress bar at 100%. Headline 'Which vibes sound like you?'. Subtitle 'Choose up to 3 favorite tones.' Wrap of large capsule chips with emoji: Casual, Flirty, Playful, Witty, Romantic, Confident, Bold. Select Flirty and Witty. Below, a labeled slider 'Humor level' from 'Subtle' to 'Silly' set to the middle. Bottom: gradient capsule 'Continue' button.
```

### A4 · Yaş, gizlilik ve AI paylaşım onayı

```
[DESIGN SYSTEM BLOCK]
SCREEN: Age and privacy consent. Headline 'Before we start'. Card 1 with a badge icon: title 'Spark is for adults 18+', toggle row 'I confirm that I am 18 or older'. Card 2 with a shield icon: title 'Your privacy comes first', three short check-list lines: 'Photos are processed and deleted right away by default', 'We never sell your photos', 'Delete your data anytime in Settings'. Card 3 with a sparkle icon: title 'AI processing', text 'Your photos and notes are sent securely to Google's Gemini AI to generate suggestions.' and toggle row 'I agree to share my photos and notes for this purpose'. Below: text 'By continuing you agree to the Terms of Service and Privacy Policy' with two tappable links in the primary color. Bottom: gradient capsule 'Continue' button shown disabled until all toggles are on.
```

### A5 · Giriş (Sign in with Apple zorunlu)

```
[DESIGN SYSTEM BLOCK]
SCREEN: Sign in. Small Spark logo at top. Headline 'Create your account'. Subtitle 'Save your people, history and favorites securely.' Three stacked buttons of equal width and height: 1) white 'Sign in with Apple' button with Apple logo following Apple's button guidelines, 2) 'Continue with Google' button with Google logo, 3) bordered 'Continue with email' button. Helper text under the email button: 'We'll send you a one-time code.' Bottom small legal text: 'By continuing you agree to our Terms and Privacy Policy.'
```

### A6 · Paywall (kapatılabilir, App Store uyumlu)

```
[DESIGN SYSTEM BLOCK]
SCREEN: Paywall. Clearly visible close (X) button top left. Hero sparkle icon with a violet glow. Headline 'Unlock Spark Plus'. Check list: 'Up to 100 generations a day', 'Unlimited people and history', 'Continue any chat', 'Your personal writing style', 'Custom tones'. Three plan cards stacked: Yearly selected with a gradient outline, badge 'Best value · 3-day free trial', price '$49.99/year', small text '$0.96 per week'; Monthly '$12.99/month'; Weekly '$4.99/week'. Gradient capsule button 'Start free trial'. Below, small legal text: 'After the free trial, $49.99/year. Subscription renews automatically until cancelled. Cancel anytime in your Apple ID settings.' Footer text buttons: 'Restore Purchases', 'Terms of Use', 'Privacy Policy'. Also a 'Maybe later' text button.
```

---

## B — Opener Generator ve Continue Chat

### B1 · Home

```
[DESIGN SYSTEM BLOCK]
SCREEN: Home tab. Header: left greeting 'Hey, Alex' with subtitle 'Ready to start a conversation?'; right a capsule pill with a sparkle icon and '3 left today'. Hero card with a gradient outline: title 'Create an opener', text 'Upload a photo and get 5 message ideas', gradient capsule 'Start' button. Below, two side-by-side cards: 'Continue a chat' (chat icon, 'Paste or upload a conversation', small PLUS badge) and 'My Style' (pen icon, 'Make Spark sound like you', small PLUS badge). Section 'Quick tones': horizontally scrolling capsule chips. Section 'Recent people' with 'See all': horizontal avatars with initials and names. Section 'Recent suggestions': 3 vertical cards, each with a two-line message preview, tone chip, relative time and a copy icon. Bottom tab bar with Home selected.
```

### B2 · Opener adım 1: Foto

```
[DESIGN SYSTEM BLOCK]
SCREEN: New opener, step 1 of 3 (Upload photo). Navigation bar with back chevron, title 'New opener' and a 3-step progress indicator. Large rounded dashed upload area with an image icon, title 'Add a photo', subtitle 'A profile photo or a screenshot of their profile'. Two buttons: filled gradient 'Choose from library' and bordered 'Take a photo'. A tips card: 'Better results with a clear photo, one person, and a visible activity or place.' A lock-icon note: 'Photos are deleted after processing.' Bottom capsule 'Next' button in disabled state. Also generate a second variant with a photo selected: preview with a remove (X) button and a 'Change photo' link, and 'Next' enabled.
```

### B3 · Opener adım 2: Ton ve kişi

```
[DESIGN SYSTEM BLOCK]
SCREEN: New opener, step 2 of 3 (Pick a vibe). Title 'Pick a vibe'. Two-column grid of tone cards, each with emoji, name and one-line description: Casual 'Easygoing and natural', Flirty 'Light and charming', Playful 'Fun and teasing', Witty 'Clever and quick', Romantic 'Warm and sincere', Confident 'Direct and self-assured', Bold 'Daring, still PG-13'. Show Flirty selected with a gradient outline and checkmark. Below: section 'Who is this?' with an iOS menu-style picker row 'Link to a person (optional)' and a '+ New person' text button. A text button 'Manage tones'. Bottom bar: bordered 'Back' and gradient 'Next'.
```

### B4 · Opener adım 3: Kişisel yorum

```
[DESIGN SYSTEM BLOCK]
SCREEN: New opener, step 3 of 3 (Add your take). Title 'Add your take'. Multiline text field with label 'Your thoughts (optional)', helper text 'Mention what caught your eye or what you want to say.', placeholder 'She loves hiking, I want to ask about her favorite trail.' and a character counter 0/300. Under the field, suggestion chips: 'Mention a shared interest', 'Keep it short', 'Ask a question', 'Avoid emojis'. A summary card with photo thumbnail, tone chip 'Flirty' and person chip 'Emma'. Bottom: gradient capsule button 'Generate 5 openers' with small text 'Uses 1 credit'.
```

### B5 · Yükleniyor

```
[DESIGN SYSTEM BLOCK]
SCREEN: Generating. No navigation bar. Centered photo thumbnail inside an animated rose-to-violet gradient ring with small sparkles around it. Rotating status text below: 'Reading the vibe...'. At the bottom, three shimmer skeleton message cards. A small 'Cancel' text button at the very bottom.
```

### B6 · Sonuçlar

```
[DESIGN SYSTEM BLOCK]
SCREEN: Opener results. Navigation bar: back chevron, title 'Your openers', and an ellipsis menu button. Summary row with a small photo thumbnail, tone chip 'Flirty' and person chip 'Emma'. Five message cards stacked. Card 1 has a 'Top pick' badge. Messages: 1 'That trail photo looks incredible. Where was it taken, and is it worth the hike?' 2 'Okay, I have to know: is the dog yours or did you borrow him for the photo?' 3 'You look like someone who has strong opinions about the best coffee in town. Am I right?' 4 'Your sunset shot is dangerously good. Please tell me you took it yourself.' 5 'Quick question: mountains or beach? Choose wisely, this is a very serious interview.' Each card has icon buttons: copy, heart (save), thumbs up, thumbs down, and a text button 'Mark as sent'. Bottom bar: bordered 'Regenerate' with '1 credit' and tinted 'Change tone'. A toast banner 'Copied to clipboard'. Small footer line: 'AI-generated content' with a flag icon 'Report'.
```

### B7a · Sonuç değerlendirme sheet

```
[DESIGN SYSTEM BLOCK]
SCREEN: Medium-detent sheet with a grabber over the results screen, titled 'How did it go?'. A quote card showing the selected message. Selectable list: 'I sent it', 'They replied', 'No reply yet'. Section 'Was this suggestion helpful?' with thumbs up and thumbs down buttons. When thumbs down is selected, show chips: 'Too long', 'Not my style', 'Too cheesy', 'Too bold'. Buttons: gradient 'Save' and text 'Cancel'.
```

### B7b · Rapor sheet

```
[DESIGN SYSTEM BLOCK]
SCREEN: Medium-detent sheet with a grabber over the results screen, titled 'Report this suggestion'. Selectable list: 'Offensive or inappropriate', 'Sexual content', 'Harassing or manipulative', 'Inaccurate or irrelevant', 'Other'. Optional multiline field 'Tell us more'. Buttons: gradient 'Submit report' and text 'Cancel'. Helper text: 'Reports help us improve Spark's safety.'
```

### B8 · Continue Chat: Girdi

```
[DESIGN SYSTEM BLOCK]
SCREEN: Continue a chat. Navigation bar with back chevron, title 'Continue a chat', small PLUS badge. Segmented control: 'Screenshot' selected, 'Paste text'. Upload area 'Upload a chat screenshot' with helper 'Up to 3 images' and buttons 'Choose from library' and 'Take a photo'. Menu picker row 'Who are you talking to? (optional)'. Horizontal tone chips (Casual, Flirty, Playful, Witty...). Multiline field 'What do you want to say? (optional)' with placeholder 'Suggest a smooth way to ask them out.' Bottom: gradient capsule 'Suggest replies' with 'Uses 1 credit'.
```

### B9 · Continue Chat: Sonuçlar

```
[DESIGN SYSTEM BLOCK]
SCREEN: Reply results. Same layout as the opener results screen with 4 reply cards. At the top, a collapsible 'Conversation context' card showing a small screenshot thumbnail, a quote bubble of their last message 'Haha no way. What are you up to this weekend?', and chips 'Playful' and 'Emma'. Reply cards have copy, heart, thumbs up/down and 'Mark as sent'. Bottom bar: 'Regenerate' and 'Change tone'. Footer: 'AI-generated content' with 'Report'.
```

---

## C — People

### C1 · People listesi

```
[DESIGN SYSTEM BLOCK]
SCREEN: People tab. Large title 'People' with a plus button in the navigation bar. Search bar below. Segmented filter chips: All, New, Talking, Date planned, Ended. List of person cards, each with an initials avatar, name, a source chip (Hinge, Tinder, Instagram, In person), a status chip, 'Last activity 2 days ago' and '4 suggestions'. A banner near the top for free users: '2 of 3 people used. Upgrade for unlimited.' Bottom tab bar with People selected.
```

### C2 · Kişi detayı

```
[DESIGN SYSTEM BLOCK]
SCREEN: Person detail. Navigation bar with back chevron, 'Edit' button and ellipsis menu. Header: large initials avatar, name 'Emma', chips 'Hinge' and 'Talking'. Two action buttons side by side: gradient 'New opener' and bordered 'Continue chat'. Section 'About': interest chips (Hiking, Jazz, Dogs) and a notes card 'Loves trail runs, works in design.' Section 'Photos': horizontal thumbnail row with a small note 'Saved with your permission'. Section 'Recent activity': timeline of the last 3 suggestions with status icons (sent, replied, no reply). Text link 'View full history'.
```

### C3 · Kişi ekle / düzenle

```
[DESIGN SYSTEM BLOCK]
SCREEN: Add person, presented as a modal sheet. Navigation bar: 'Cancel' on the left, title 'Add person', 'Save' on the right. Circular avatar picker with camera icon labeled 'Add photo (optional)'. Fields: 'Name' (required, show an error state 'Please enter a name'), 'Where did you meet?' as single-select chips (Hinge, Tinder, Bumble, Instagram, In person, Other), 'Status' as chips (New, Talking, Date planned, Ended), 'Interests' chip input with an add field, 'Notes' multiline with helper 'Notes help Spark write better messages.' Bottom: gradient capsule 'Save person' button.
```

---

## D — History

### D1 · History listesi

```
[DESIGN SYSTEM BLOCK]
SCREEN: History tab. Large title 'History'. Segmented control: All, Openers, Replies. Date group headers 'Today', 'Yesterday', 'Last week'. Conversation rows with an initials avatar, person name (or 'No person'), a two-line preview of the last suggestion, a tone chip, relative time, and outcome icons (sent check, replied double check in green, no reply in gray). Show one row in a swipe-to-delete state with a red Delete action. A free-plan banner: 'History keeps 7 days. Upgrade to keep everything.' Bottom tab bar with History selected.
```

### D2 · Konuşma detayı

```
[DESIGN SYSTEM BLOCK]
SCREEN: Conversation detail. Navigation bar with back chevron, title 'Emma', ellipsis menu. A vertical timeline with a left line and dots. Each event is a card: header 'Opener' or 'Reply' with tone chip and time, an input preview (photo thumbnail or quoted message), and the first 2 suggestions with status chips (Sent, Replied, No reply, Not used) and a 'Show all 5' expander. Bottom capsule button 'Continue this chat'.
```

---

## E — Saved

### E1 · Saved listesi

```
[DESIGN SYSTEM BLOCK]
SCREEN: Saved tab. Large title 'Saved' with search and 'New folder' buttons in the navigation bar. Segmented control: Favorites | Templates. Horizontal folder chips: All, Openers, Funny, Date ideas, '+ New folder'. List of saved cards, each with up to 3 lines of message text, tag chips, tone chip, a copy icon and an ellipsis context menu (Move, Edit, Delete). A free-plan banner 'Folders: 1 of 3 used'. On the Templates tab, a plus button in the navigation bar. Bottom tab bar with Saved selected.
```

### E2 · Şablon oluştur / düzenle

```
[DESIGN SYSTEM BLOCK]
SCREEN: New template, presented as a modal sheet. Navigation bar: 'Cancel', title 'New template', 'Save'. Fields: 'Title', 'Message' multiline with helper 'Use {name} and {interest} as placeholders', 'Folder' menu picker, 'Tags' chip input. A preview card at the bottom: 'Preview with Emma, hiking' showing the filled message. Gradient capsule 'Save template' button.
```

---

## F — Tones

### F1 · Tones kütüphanesi

```
[DESIGN SYSTEM BLOCK]
SCREEN: Tones. Navigation bar with back chevron, large title 'Tones' and a plus button. Section 'Built-in' as a two-column grid of 7 tone cards (emoji, name, one-line description). Section 'My tones' with a PLUS badge: two custom tone cards with edit and delete icons ('Dry Humor', 'Smooth Talker'). Below, a dashed card 'Create your own tone' with a 'Create tone' button.
```

### F2 · Özel ton oluştur

```
[DESIGN SYSTEM BLOCK]
SCREEN: New tone, presented as a modal sheet. Navigation bar: 'Cancel', title 'New tone', 'Save'. Fields: emoji picker button, 'Name' (max 20 chars, counter), 'Short description', 'How should it sound?' multiline with placeholder 'Dry humor, short sentences, never uses emojis.' Three labeled sliders: Length (Short to Long), Boldness (Soft to Bold), Humor (Subtle to Silly). A 'Test it' card with a sample message preview and a bordered 'Preview' button. Gradient capsule 'Save tone' button.
```

---

## G — My Style

### G1 · My Style (boş durum)

```
[DESIGN SYSTEM BLOCK]
SCREEN: My Style empty state. Navigation bar with back chevron, title 'My Style', PLUS badge. Illustration of a pen and a waveform in rose and violet. Headline 'Make Spark sound like you'. Body 'Add a few of your real messages and Spark learns your voice.' A card with 3 numbered steps: 'Add 5 or more messages', 'Spark analyzes your style', 'Get suggestions in your voice'. Gradient capsule 'Add my messages' button. A lock-icon note: 'Your messages are only used to personalize suggestions. Delete them anytime.'
```

### G2 · My Style (mesaj ekle ve analiz)

```
[DESIGN SYSTEM BLOCK]
SCREEN: My Style analysis. Navigation bar 'My Style'. Section 'Your messages' with a counter '6 messages (5 minimum)' and a list of message chips each with a delete icon, plus an input field 'Paste a message' with an add button. Gradient capsule 'Analyze my style' button. Result card 'Your style': trait chips (Short sentences, Dry humor, Few emojis), three read-only bars (Formality, Length, Emoji use), and a quote 'Spark will write like this: Okay, that trail looks way too good. Where is it?'. A toggle 'Use my style in suggestions' turned on. Buttons: bordered 'Re-analyze' and red text button 'Delete style data'.
```

---

## H — Profile ve Ayarlar

### H1 · Profile

```
[DESIGN SYSTEM BLOCK]
SCREEN: Profile tab. Header with initials avatar, name 'Alex', email, and a 'Free' plan badge. A credits card: 'Credits today 2 of 3' with a progress bar, and a gradient capsule 'Upgrade to Plus' button. Inset grouped list rows with SF Symbols and chevrons. Personalize: My Style, Tones. Account: Subscription, Privacy and Data, Settings. Support: Help and Feedback. About: Terms of Service, Privacy Policy, 'Version 1.0.0'. A red text button 'Sign Out'. Bottom tab bar with Profile selected.
```

### H2 · Subscription

```
[DESIGN SYSTEM BLOCK]
SCREEN: Subscription for a free user. Navigation bar 'Subscription'. Current plan card 'Free'. A comparison table Free vs Plus with rows: Daily generations (3 / Up to 100), People (3 / Unlimited), History (7 days / Unlimited), Continue chat (locked / included), My Style (locked / included), Custom tones (locked / included). Gradient capsule 'Upgrade to Plus' button. Text buttons 'Restore Purchases' and 'Manage Subscription' (opens Apple ID settings). Also generate a variant for a subscribed user: plan card 'Spark Plus · Yearly', 'Renews on March 12', benefits list, bordered 'Manage Subscription'.
```

### H3 · Privacy and Data

```
[DESIGN SYSTEM BLOCK]
SCREEN: Privacy and Data. Navigation bar with back chevron. Info card 'What we store' with 3 short lines: profile and people, history and saved messages, optional style data. Toggle rows with helper text: 'Delete photos after processing' (on, badge 'Recommended'), 'Save photos to person profiles' (off), 'Share anonymous usage analytics' (on). A short note: 'Photos and notes are processed by Google's Gemini AI to generate suggestions.' Buttons: bordered 'Delete all history' and a red text link 'Delete my account'.
```

### H4 · Settings

```
[DESIGN SYSTEM BLOCK]
SCREEN: Settings. Navigation bar with back chevron. Inset grouped list. Section Appearance: segmented control System, Dark, Light. Section Preferences: row 'Default tone' showing 'Flirty', row 'Language' showing 'English' (disabled). Section Help: rows 'FAQ', 'Contact Support', 'Send Feedback', 'Rate Spark'. Section About: 'Terms of Service', 'Privacy Policy', 'Open Source Licenses', 'Version 1.0.0'.
```

### H5 · Hesap silme

```
[DESIGN SYSTEM BLOCK]
SCREEN: Delete account confirmation. Navigation bar with back chevron. A warning icon in a red tinted circle. Headline 'Delete your account?'. Body list: 'Your profile and people', 'History, saved messages and style data', 'Uploaded photos'. Note card: 'Active subscriptions must be cancelled in your Apple ID settings.' Toggle row 'I understand this cannot be undone.' Text field 'Type DELETE to confirm'. Red filled capsule button 'Delete account' in disabled state, bordered 'Cancel'.
```

---

## I — Sistem Durumları

### I1 · Kredi bitti

```
[DESIGN SYSTEM BLOCK]
SCREEN: Medium-detent sheet with a grabber over the Home screen. Sparkle icon. Title 'You're out of credits for today'. Body 'Your free credits reset in 6h 24m.' Three short Plus highlights with checkmarks. Gradient capsule button 'Upgrade to Plus' and text button 'Maybe tomorrow'.
```

### I2 · Hata durumları (3 ayrı ekran)

```
[DESIGN SYSTEM BLOCK]
SCREEN: Error state, generate three separate screens with the same layout: centered soft illustration, title, body, primary button, text button. Screen 1: 'Something went wrong', 'We couldn't generate messages. You weren't charged a credit.', 'Try again', 'Go back'. Screen 2: 'No internet connection', 'Check your connection and try again.', 'Retry'. Screen 3: 'We couldn't use this photo', 'Try a clearer photo with one person.', 'Choose another photo'.
```

### I3 · Boş durumlar (3 ayrı ekran)

```
[DESIGN SYSTEM BLOCK]
SCREEN: Empty state, generate three separate tab screens with the bottom tab bar. Each has a soft illustration, title, one-line body and a gradient capsule button. People: 'No one here yet', 'Add someone to keep their notes and history.', 'Add person'. History: 'No conversations yet', 'Your openers and replies will show up here.', 'Create an opener'. Saved: 'Nothing saved yet', 'Tap the heart on a message to keep it here.', 'Browse openers'.
```
