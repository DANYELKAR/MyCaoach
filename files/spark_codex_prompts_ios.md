# Spark (iOS) — Codex Prompt Dosyası

Bu dosya Codex içindir: iOS uygulaması (SwiftUI) + Spring Boot backend + Supabase. Tasarım Stitch dosyasından gelir, burada sadece kod ve mimari var.

## Kullanım

1. **Bölüm 1'i** repo kökünde `AGENTS.md` olarak kaydet. Codex her görevde bunu okur.
2. **Bölüm 2'deki faz prompt'larını** sırayla, birer birer Codex'e ver. Bir faz bitip test edilmeden diğerine geçme.
3. Stitch'ten çıkan ekran görüntülerini/HTML'lerini `docs/design/` klasörüne koy. Faz prompt'larında ekran kodları (A1, B6...) Stitch dosyasıyla aynıdır.
4. Gizli anahtarları (Gemini, Supabase service key) asla repoya koyma, `.env` kullan.

---

# BÖLÜM 1 — AGENTS.md

```markdown
# Spark — Project Brief for Codex

## 1. Product
Spark is an iOS app (English-only UI) that helps adults (18+) write great first messages. The user uploads a photo of a person, picks a tone, and optionally adds a personal note. The backend calls the Gemini API and returns 5 message suggestions. The app is more than a one-shot generator: it remembers people, keeps history, supports continuing a chat, learns the user's writing style, and manages favorites and custom tones.

Out of scope (do NOT build): reminders/push notifications, date or awkward-situation coaching, gamification (streaks, badges, leaderboards).

## 2. Plans and limits
- Free: 3 generations per day (1 generation = 5 suggestions = 1 credit), max 3 people, 7 days of history, built-in tones only, max 3 folders.
- Plus (subscription): up to 100 generations per day (fair use), unlimited people and history, Continue Chat, My Style, custom tones, templates.
- Pricing (hypothesis, configurable in RevenueCat/App Store Connect): weekly $4.99, monthly $12.99, yearly $49.99 with a 3-day trial.
- Daily limit resets at local midnight using the user's IANA timezone stored in `profiles.timezone`. Remaining credits are computed from SUCCEEDED generations since the start of the user's local day. There is no balance column.

## 3. Tones
Built-in: Casual, Flirty, Playful, Witty, Romantic, Confident, Bold. Bold is daring but strictly PG-13 (App Store policy). Custom tones (Plus) have a name, emoji, description, free-text style instruction and length/boldness/humor sliders.

## 4. Tech stack
iOS app:
- Swift 5.10+/6, Xcode 16+, SwiftUI, minimum iOS 17, iPhone only for v1.
- Architecture: MVVM with the Observation framework (@Observable), Swift Concurrency (async/await), NavigationStack, TabView with 5 tabs (Home, People, History, Saved, Profile).
- Packages (Swift Package Manager): supabase-swift (Auth), GoogleSignIn-iOS, RevenueCat purchases-ios (StoreKit 2 under the hood). Networking with URLSession + Codable. No Alamofire.
- Photos: PhotosUI PhotosPicker and AVFoundation camera. Downscale to max 1024 px on the long edge and JPEG quality 0.8 before upload.
- Storage: Keychain for tokens, UserDefaults/AppStorage for light preferences, no local database in v1 (optional in-memory cache).
- Localization: English only via String Catalog (Localizable.xcstrings).
- Privacy manifest (PrivacyInfo.xcprivacy) and App Tracking: do not track users, no ATT prompt.

Backend:
- Java 21, Spring Boot 3.x, Maven.
- Spring Web, Spring Security OAuth2 Resource Server, Spring Data JPA, Flyway, Bean Validation, springdoc-openapi, Bucket4j (rate limiting), Caffeine (cache), Testcontainers (integration tests), Lombok optional.
- Supabase: PostgreSQL (primary DB), Auth (Apple, Google, email OTP), Storage (private bucket `inputs`).
- Gemini via the Google Gen AI Java SDK (verify the current artifact and version) or plain REST. Model name must come from config (`GEMINI_MODEL`), default to a current Flash-tier model.
- Hosting target: container (Dockerfile) deployable to Cloud Run, Railway or Render.

## 5. Repository layout
/backend            Spring Boot service (package com.spark.api)
/ios                Xcode project "Spark"
/docs/design        Stitch exports (read-only reference)
/docs/api           OpenAPI snapshot
AGENTS.md
README.md

## 6. Database (Flyway V1__init.sql)
Use text columns with CHECK constraints instead of Postgres enums (easier with JPA). All ids are uuid with default gen_random_uuid(). All tables have created_at timestamptz default now(); mutable tables have updated_at. Enable RLS on every table and add no policies for anon/authenticated roles (the backend connects with a privileged role and enforces ownership by user_id in the service layer).

profiles: id uuid PK references auth.users(id) on delete cascade, display_name text, age_confirmed_at timestamptz, ai_consent_at timestamptz, goals text[] default '{}', humor_level int default 50, default_tone_id uuid, plan text default 'FREE' check in ('FREE','PLUS'), plan_expires_at timestamptz, timezone text default 'UTC', store_images boolean default false, analytics_opt_in boolean default true.

tones: id, user_id uuid null references profiles(id) on delete cascade (null = built-in), name text, emoji text, description text, prompt_fragment text, length_level int, boldness_level int, humor_level int, sort_order int, is_active boolean default true.

contacts: id, user_id, name text not null, source text check in ('HINGE','TINDER','BUMBLE','INSTAGRAM','IN_PERSON','OTHER'), status text default 'NEW' check in ('NEW','TALKING','DATE_PLANNED','ENDED'), interests text[] default '{}', notes text, photo_path text.

threads: id, user_id, contact_id uuid null references contacts(id) on delete set null, title text, last_activity_at timestamptz.

generations: id, user_id, thread_id, contact_id, type text check in ('OPENER','REPLY'), tone_id, user_note text, input_text text, input_image_paths text[] default '{}', status text default 'PENDING' check in ('PENDING','SUCCEEDED','FAILED','BLOCKED'), model text, tokens_in int, tokens_out int, idempotency_key text, unique (user_id, idempotency_key).

suggestions: id, generation_id references generations(id) on delete cascade, user_id, text text not null, rank int, is_top_pick boolean default false, feedback text null check in ('LIKE','DISLIKE'), feedback_reason text, sent_status text default 'NOT_SENT' check in ('NOT_SENT','SENT'), outcome text default 'NONE' check in ('NONE','REPLIED','NO_REPLY').

folders: id, user_id, name text.
saved_items: id, user_id, folder_id null, kind text check in ('SUGGESTION','TEMPLATE'), suggestion_id null, title text, text text not null, tags text[] default '{}'.
style_profiles: user_id PK references profiles(id) on delete cascade, sample_messages text[] default '{}', summary jsonb, enabled boolean default true.
reports: id, user_id, suggestion_id, reason text, details text.

Indexes: (user_id, created_at desc) on generations, suggestions, contacts, threads(user_id, last_activity_at desc), saved_items(user_id, folder_id).
Seed the 7 built-in tones in V2__seed_tones.sql with prompt_fragment text for each.

## 7. API contract (base path /v1, JSON, JWT bearer)
Error body for all errors: { "code": "STRING_CODE", "message": "Human readable", "requestId": "uuid" }.
Codes: UNAUTHORIZED 401, FORBIDDEN_PLAN 403, OUT_OF_CREDITS 402, NOT_FOUND 404, VALIDATION_ERROR 422, CONTENT_BLOCKED 422, RATE_LIMITED 429, AI_UNAVAILABLE 503.

Profile: GET /me, PATCH /me, DELETE /me (hard-deletes everything including storage objects), GET /me/usage -> { "remainingToday": 2, "dailyLimit": 3, "resetsAt": "ISO-8601" }
Generations: POST /generations/opener (multipart: image file, toneId, note, contactId?, idempotencyKey), POST /generations/reply (multipart: images[] up to 3 OR text, toneId, note?, contactId?, idempotencyKey), GET /generations, GET /generations/{id}
Response of generation endpoints: { "generation": {id, type, toneId, contactId, threadId, createdAt}, "suggestions": [{id, text, rank, isTopPick}], "usage": {remainingToday, dailyLimit} }
Suggestions: PATCH /suggestions/{id} body {feedback?, feedbackReason?, sentStatus?, outcome?}, POST /suggestions/{id}/report body {reason, details?}
Contacts: GET /contacts?status=&q=, POST /contacts, GET /contacts/{id}, PATCH /contacts/{id}, DELETE /contacts/{id}
Threads: GET /threads?type=, GET /threads/{id} (includes generations and suggestions timeline), DELETE /threads/{id}, DELETE /threads (delete all history)
Saved: GET/POST /saved, PATCH/DELETE /saved/{id}, GET/POST /folders, PATCH/DELETE /folders/{id}
Tones: GET /tones (built-in + user's), POST/PATCH/DELETE /tones/{id} (custom only, Plus only)
Style: GET /style-profile, PUT /style-profile (sample messages), POST /style-profile/analyze, DELETE /style-profile
Billing: POST /billing/webhook (RevenueCat webhook, verify authorization header), POST /billing/sync (client asks backend to refresh entitlement)
Plan checks happen server-side. Return FORBIDDEN_PLAN for Plus features on Free.

## 8. Generation flow (must follow)
1. Validate JWT and request. Check idempotencyKey; if already SUCCEEDED, return the stored result.
2. Check remaining daily credits. If none, return OUT_OF_CREDITS before calling Gemini.
3. Create generation row with PENDING. Upload image to Supabase Storage `inputs/{userId}/{generationId}/` only if needed; otherwise keep the bytes in memory.
4. Build prompt: system prompt + tone fragment + user note + contact context (name, interests, notes) + last 5 suggestions for this contact + style summary if enabled and Plus.
5. Call Gemini with the image inline and a response JSON schema. Timeout 25 s, one retry on 5xx.
6. Parse and validate: exactly 5 suggestions (4 for replies), each max 220 chars, no duplicates. If Gemini blocks or returns invalid output, mark BLOCKED or FAILED and do NOT consume a credit (credits are derived from SUCCEEDED rows only).
7. Save suggestions, mark SUCCEEDED, update thread.last_activity_at.
8. Delete the stored image immediately unless `profiles.store_images` is true. Never log image bytes or full message text; log ids, durations and token counts.

Gemini system prompt (store as a resource file, versioned):
"You write short, natural first messages for a texting app used by adults. Use only what is visible in the image (setting, activity, clothing, objects) and what the user wrote. Never guess or mention the person's identity, ethnicity, religion, health, sexuality, age or any sensitive trait. Never comment on body parts in a sexual way. Keep every message respectful, specific and PG-13, even in the Bold tone. No harassment, pressure, manipulation or deception. Match the requested tone and the optional user style summary. Each message is under 220 characters, sounds like a real person, and ends with something easy to reply to. Return JSON only that matches the schema."
Response schema: { "suggestions": [ { "text": string } ], "blocked": boolean, "blockedReason": string|null }.
Configure Gemini safety settings strictly. Mark the first suggestion as the top pick.

## 9. Auth
- iOS signs in through Supabase Auth: Sign in with Apple (required by App Store guideline 4.8 when offering Google), Google, email OTP.
- Backend validates the Supabase access token as an OAuth2 resource server using the project's JWKS endpoint (https://<project-ref>.supabase.co/auth/v1/.well-known/jwks.json). If the project still uses the legacy HS256 secret, support it via config. Subject claim = user id.
- On first authenticated call, upsert the profile row.
- Database connection: Supabase pooler. If using the transaction pooler, add `prepareThreshold=0` to the JDBC URL.

## 10. Billing
- RevenueCat handles StoreKit 2. Entitlement id: `plus`. The app uses the RevenueCat app user id = Supabase user id.
- Backend receives RevenueCat webhooks, updates profiles.plan and plan_expires_at, and exposes /billing/sync for a manual refresh.
- Paywall must show price, period, trial terms, auto-renew notice, Restore Purchases, Terms and Privacy links, and a visible close button.

## 11. iOS structure
Spark/
  App/ (SparkApp.swift, AppState.swift, RootView.swift, Router)
  Core/ (Networking, Auth, Keychain, DesignSystem, Models, Utilities, Config)
  Features/ Onboarding, Home, Opener, ContinueChat, People, History, Saved, Tones, Style, Profile, Paywall, Components
  Resources/ (Assets, Localizable.xcstrings, PrivacyInfo.xcprivacy)
SparkTests/, SparkUITests/

Design tokens (Core/DesignSystem): background #0F0D14, surface #1A1722, elevated #252233, primary #FF4D7D, secondary #8B5CF6, gradient primary->secondary, label primary #F5F3FA, label secondary #A09BB0, success #34D399, warning #FBBF24, destructive #F87171, separator #3A3650. SF Pro system fonts with Dynamic Type. 20pt card radius, 52pt capsule buttons, 8pt grid.
Reusable components: PrimaryButton (gradient capsule), SecondaryButton, ToneChip, SuggestionCard, PersonRow, StatusChip, EmptyStateView, ErrorStateView, ToastBanner, PlusBadge, CreditsPill, StepProgress.

Navigation: AppState decides Splash -> Onboarding (A2..A6) -> Main TabView. Each tab has its own NavigationStack. Opener flow B2->B3->B4->B5->B6 is a pushed stack with a shared OpenerFlowViewModel. Sheets for B7a, B7b, C3, E2, F2, I1.

Screen codes (match docs/design and the Stitch file): A1 Splash, A2 Welcome x3, A3 Personalization x2, A4 Consent, A5 Sign in, A6 Paywall, B1 Home, B2-B4 Opener steps, B5 Generating, B6 Results, B7a Feedback sheet, B7b Report sheet, B8 Continue input, B9 Reply results, C1-C3 People, D1-D2 History, E1-E2 Saved, F1-F2 Tones, G1-G2 My Style, H1-H5 Profile/Settings, I1-I3 System states.

## 12. App Store compliance (engineering checklist)
- Sign in with Apple offered alongside Google.
- In-app account deletion (H5) that calls DELETE /me and also removes the Supabase auth user.
- Explicit consent before sending photos/notes to a third-party AI (A4) and a note in Privacy and Data (H3). Record `ai_consent_at`.
- Age gate 18+ (A4), `age_confirmed_at` saved.
- Report mechanism on every AI result (B7b) writing to `reports`.
- Subscription disclosures, Restore Purchases and Manage Subscription (H2).
- Privacy manifest and accurate App Privacy answers. No tracking.
- Keep Bold tone PG-13. Never show raw model errors to users.

## 13. Engineering rules
- Small, reviewable commits per task. Explain assumptions in the PR/commit message instead of asking many questions.
- Backend: controller -> service -> repository layering, DTOs separate from entities, constructor injection, global @ControllerAdvice for errors, request id filter, structured JSON logs without PII.
- Tests: unit tests for services (credit logic, prompt builder, plan checks), Testcontainers integration tests for repositories and the generation endpoint with a mocked Gemini client. iOS: unit tests for view models and networking with a mock URLProtocol, a few UI tests for onboarding and the opener flow.
- Config via environment variables (`.env.example` committed, `.env` ignored): SUPABASE_URL, SUPABASE_JWKS_URL, SUPABASE_SERVICE_KEY, DATABASE_URL, GEMINI_API_KEY, GEMINI_MODEL, REVENUECAT_WEBHOOK_SECRET.
- Provide Dockerfile, docker-compose for local Postgres, GitHub Actions for backend build/test and iOS build/test.
- Accessibility: VoiceOver labels, Dynamic Type, minimum 44pt targets, contrast 4.5:1.
- Definition of done for each phase: builds, tests pass, README updated, OpenAPI updated, no TODOs left in the phase scope.
```

---

# BÖLÜM 2 — Faz Prompt'ları

Her prompt'u ayrı bir Codex görevi olarak ver. Başına şunu ekleyebilirsin: "Read AGENTS.md first."

## Faz 0 — İskelet

```
Read AGENTS.md first. Phase 0: scaffolding.
1. Create the monorepo layout from AGENTS.md section 5.
2. Backend: Spring Boot 3 (Java 21, Maven) project `com.spark.api` with dependencies listed in AGENTS.md, Dockerfile, docker-compose for local Postgres, application.yml reading env vars, Flyway enabled, a /actuator/health endpoint, global error handler using the error body format, request id filter, and springdoc.
3. iOS: Xcode project "Spark" (iOS 17, SwiftUI) with the folder structure from AGENTS.md section 11, SPM dependencies (supabase-swift, GoogleSignIn-iOS, purchases-ios), Config layer reading base URL and keys from xcconfig, DesignSystem tokens and the reusable components (PrimaryButton, SecondaryButton, ToneChip, PlusBadge, ToastBanner, EmptyStateView, ErrorStateView), and a placeholder RootView with the 5-tab TabView.
4. Add .env.example, .gitignore, README with setup steps, and GitHub Actions workflows for backend and iOS builds.
Acceptance: `mvn verify` passes, the app builds and launches to the empty tab bar, and CI is green.
```

## Faz 1 — Backend temeli: Auth, Profile, Tones, Şema

```
Read AGENTS.md first. Phase 1: backend foundation.
1. Write Flyway V1__init.sql exactly per AGENTS.md section 6 and V2__seed_tones.sql with the 7 built-in tones and good prompt_fragment texts.
2. Implement JWT validation as OAuth2 resource server (JWKS, with a config flag for legacy HS256). Upsert the profile on first call.
3. Implement GET/PATCH /me, GET /me/usage (credits computed from SUCCEEDED generations since the user's local midnight), GET /tones.
4. Implement the plan service (FREE vs PLUS limits) used across the app.
5. Add Testcontainers integration tests and unit tests for the credit calculation across timezones.
Acceptance: migrations run on a clean Postgres, tests pass, OpenAPI lists the endpoints, and a request without a valid token returns 401 in the standard error format.
```

## Faz 2 — Opener akışı (backend + iOS)

```
Read AGENTS.md first. Phase 2: the core Opener flow.
Backend: implement POST /generations/opener following AGENTS.md section 8 exactly: idempotency, credit check before Gemini, prompt builder (versioned system prompt resource), Gemini client with response schema, validation of output, FAILED/BLOCKED handling without consuming credit, immediate image deletion, rate limiting with Bucket4j (per user), structured logs with token counts. Mock the Gemini client in tests. Also implement GET /generations and GET /generations/{id}.
iOS: build screens A1 to A5 (splash, welcome carousel, personalization, consent with ai_consent_at and age_confirmed_at, sign in with Apple/Google/email OTP via supabase-swift), then B1 Home, B2 to B4 (photo, tone, note), B5 generating, and B6 results (copy to clipboard with toast, heart to save is stubbed, thumbs and Mark as sent are stubbed until Phase 3). Implement the networking layer (URLSession, Codable, automatic token refresh, typed errors mapped to ErrorStateView and the I1 sheet for OUT_OF_CREDITS), image downscale to 1024 px, and the OpenerFlowViewModel. Follow docs/design for layout.
Acceptance: a signed-in user can upload a photo, pick a tone, add a note and see 5 suggestions from the real backend; out-of-credits and blocked-content cases show the right UI; unit tests cover the view model and the API client.
```

## Faz 3 — People, History, Saved

```
Read AGENTS.md first. Phase 3: memory features.
Backend: CRUD for contacts, threads (with timeline in GET /threads/{id}), saved items and folders, PATCH /suggestions/{id} (feedback, sentStatus, outcome), POST /suggestions/{id}/report. Link generations to a thread and contact; when a contact is provided, include contact context and the last 5 suggestions in the prompt. Enforce Free limits (3 people, 7-day history visibility, 3 folders) server-side with FORBIDDEN_PLAN.
iOS: build C1 to C3 (People list, detail, add/edit sheet), D1 to D2 (History list with swipe to delete, conversation timeline), E1 to E2 (Saved with folders and templates), B7a and B7b sheets, and wire heart/thumbs/Mark as sent/Report in B6. Add the free-plan banners.
Acceptance: end-to-end flows work against the backend, ownership is enforced (add a test that user A cannot read user B's data), and empty states (I3) render correctly.
```

## Faz 4 — Kredi, abonelik ve paywall

```
Read AGENTS.md first. Phase 4: monetization.
Backend: RevenueCat webhook endpoint with secret verification, mapping events (initial purchase, renewal, cancellation, expiration, billing issue) to profiles.plan and plan_expires_at, plus POST /billing/sync. Make plan checks consistent everywhere.
iOS: integrate RevenueCat with app user id = Supabase user id. Build A6 paywall (offerings from RevenueCat, price/period/trial text from StoreKit product data, close button, Restore Purchases, Terms and Privacy links, auto-renew notice), H2 Subscription (free and subscribed variants, Manage Subscription opens Apple's subscription management), and gate Plus features with a reusable PlusGate that presents the paywall. Show the credits pill on Home and the I1 sheet when out of credits.
Acceptance: sandbox purchase flips the plan within a few seconds via webhook or sync, Restore works, and Plus features unlock/lock correctly.
```

## Faz 5 — Continue Chat ve geri bildirim döngüsü

```
Read AGENTS.md first. Phase 5: Continue Chat and feedback.
Backend: implement POST /generations/reply (up to 3 screenshots or pasted text, Plus only). Extend the prompt builder with a reply mode that reads the conversation, understands the last message, and returns 4 replies. Include feedback signals (LIKE/DISLIKE, outcome) from previous suggestions of the same contact in the prompt as soft guidance. Add an aggregate query for quality metrics (like rate, sent rate, replied rate) exposed internally for logging.
iOS: build B8 (input with Screenshot/Paste text segmented control) and B9 (results with the collapsible conversation context card), reuse SuggestionCard, and add 'Continue this chat' from C2 and D2 with the contact preselected.
Acceptance: end-to-end reply generation works with screenshots and pasted text, non-Plus users see the paywall, and tests cover prompt building for both modes.
```

## Faz 6 — My Style ve özel tonlar

```
Read AGENTS.md first. Phase 6: personalization (Plus).
Backend: implement /style-profile endpoints. PUT stores 5 to 10 sample messages (validate length and count). POST /style-profile/analyze asks Gemini for a compact JSON summary (traits, formality, length, emoji usage, sample line) and stores it in style_profiles.summary. Inject the summary into prompts when enabled. DELETE removes all style data. Implement custom tone CRUD with validation and Plus gating, and include the user's sliders and free-text instruction in the prompt fragment safely (strip prompt-injection attempts, cap lengths).
iOS: build G1 and G2 (empty state, add messages, analyze, result, toggle, delete) and F1 and F2 (tone library, create/edit custom tone with live preview), and use custom tones in the B3 tone picker.
Acceptance: style summary changes the tone of generated suggestions in an observable way (add a test that the prompt contains the summary), and deleting style data removes it from the DB.
```

## Faz 7 — Gizlilik, hesap silme ve profil

```
Read AGENTS.md first. Phase 7: privacy and account.
Backend: DELETE /me must hard-delete the user's rows in every table, remove storage objects under inputs/{userId}/, and delete the Supabase auth user using the service key. DELETE /threads deletes all history. Add PATCH /me support for store_images and analytics_opt_in. Add a nightly cleanup job that removes orphaned storage objects older than 24 hours.
iOS: build H1 Profile, H3 Privacy and Data (toggles bound to /me, delete history), H4 Settings (appearance override, default tone), H5 Delete account (type DELETE, calls the API, signs out, clears Keychain, returns to onboarding). Add Help and Feedback rows (mailto and URLs from config).
Acceptance: after account deletion no rows or files remain for that user (add an integration test), and the app returns to the welcome screen cleanly.
```

## Faz 8 — Cilalama, test ve yayın hazırlığı

```
Read AGENTS.md first. Phase 8: polish and release readiness.
1. Review every screen against docs/design for spacing, typography, dark/light appearance, Dynamic Type, VoiceOver labels and safe areas. Fix gaps.
2. Handle all states: loading skeletons, offline (I2), empty (I3), errors, and rate limiting (show a friendly message for 429).
3. Add analytics events through a small AnalyticsService protocol (activation: first generation, paywall viewed, trial started, feedback given) with a no-op default and a toggle respecting analytics_opt_in. Do not collect photos or message text.
4. Performance: image downscaling off the main thread, cancel in-flight requests on dismiss, cache the tones list.
5. Security review: no secrets in the client, ATS defaults, Keychain for tokens, request size limits and content-type checks on the backend, CORS closed, dependency audit.
6. Produce docs/release-checklist.md covering App Store Connect setup: age rating answers, App Privacy answers, subscription metadata and review notes, screenshots list, TestFlight plan, and the compliance checklist from AGENTS.md section 12.
Acceptance: all tests pass, release checklist is complete, and a TestFlight build can be archived.
```
