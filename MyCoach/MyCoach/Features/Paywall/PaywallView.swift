import SwiftUI
import RevenueCat

struct PaywallView: View {
    @Environment(AppState.self) private var state
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    BrandMark(size: 72)
                    Text("A little more possibility.").font(.largeTitle.bold())
                    Text("MyCoach Plus").font(.title2.bold())
                    if let reason = state.runtime?.paywallReason { Text(reason).foregroundStyle(Theme.warning) }
                    ForEach(["Up to 100 generations per day", "Continue Chat with text or screenshots", "Unlimited people and history", "My Style, custom tones and templates"], id: \.self) { Label($0, systemImage: "checkmark.circle.fill").foregroundStyle(Theme.label) }
                    if let runtime = state.runtime {
                        let billing = runtime.billing
                        if runtime.auth.isLocal {
                            Label("Local test entitlement · no purchase or charge", systemImage: "hammer").foregroundStyle(Theme.warning)
                            PrimaryButton(title: "Enable test Plus") { Task { do { try await billing.localEntitlement(true, runtime: runtime); dismiss() } catch { runtime.handle(error) } } }.accessibilityIdentifier("billing.testPlus")
                        } else {
                            if billing.isLoading { ProgressView("Loading subscription options…") }
                            ForEach(billing.packages, id: \.identifier) { package in
                                Button { Task { await billing.purchase(package, runtime: runtime) } } label: {
                                    VStack(alignment: .leading, spacing: 8) {
                                        Text(package.storeProduct.localizedTitle).font(.headline)
                                        Text("\(package.localizedPriceString) / \(package.storeProduct.subscriptionPeriod.map(period) ?? "subscription")")
                                        if let intro = package.storeProduct.introductoryDiscount {
                                            Text("If eligible: \(intro.paymentMode == .freeTrial ? "free" : intro.localizedPriceString) for \(intro.numberOfPeriods) × \(period(intro.subscriptionPeriod)), then the regular price above.").font(.footnote)
                                        }
                                    }.frame(maxWidth: .infinity, alignment: .leading).surfaceCard()
                                }.buttonStyle(.plain).disabled(billing.isLoading)
                            }
                            if billing.packages.isEmpty && !billing.isLoading { Text("No subscription options are currently available.").foregroundStyle(Theme.muted); Button("Try again") { Task { await billing.load(runtime: runtime) } } }
                            Button("Restore Purchases") { Task { await billing.restore(runtime: runtime) } }.disabled(billing.isLoading)
                        }
                        if let error = billing.error { Text(error).foregroundStyle(Theme.destructive) }
                    }
                    Text("Payment is charged to your Apple account at confirmation. Subscriptions automatically renew unless canceled at least 24 hours before the current period ends. Manage or cancel in your Apple subscriptions. Any eligible introductory offer and its terms are confirmed by the App Store before purchase.").font(.footnote).foregroundStyle(Theme.muted)
                    HStack { NavigationLink("Terms") { PolicyView(privacy: false) }; Spacer(); NavigationLink("Privacy") { PolicyView(privacy: true) } }
                }.padding(24)
            }.background(Theme.background).navigationTitle("MyCoach Plus").navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Close") { dismiss() }.accessibilityIdentifier("paywall.close") } }
                .task { if let runtime = state.runtime { runtime.track(.paywallViewed); await runtime.billing.load(runtime: runtime) } }
        }.tint(Theme.primary)
    }
    private func period(_ value: SubscriptionPeriod) -> String {
        let unit: String
        switch value.unit { case .day: unit = "day"; case .week: unit = "week"; case .month: unit = "month"; case .year: unit = "year"; @unknown default: unit = "period" }
        return "\(value.value) \(unit)\(value.value == 1 ? "" : "s")"
    }
}

struct SubscriptionView: View {
    @Environment(AppState.self) private var state
    @Environment(\.openURL) private var openURL
    var body: some View {
        List {
            if let runtime = state.runtime {
                Section("Your plan") {
                    Text(runtime.profile?.isPlus == true ? "MyCoach Plus" : "Free").font(.title2.bold())
                    if let usage = runtime.usage { Text("\(usage.remainingToday) of \(usage.dailyLimit) credits remaining today"); Text("Resets: \(usage.resetsAt ?? "your local midnight")").font(.footnote).foregroundStyle(Theme.muted) }
                    Button("View Plus options") { runtime.showPaywall = true }
                    Button("Refresh plan") { Task { do { try await runtime.billing.sync(runtime: runtime); runtime.toast = "Plan refreshed" } catch { runtime.handle(error) } } }
                }
                if runtime.auth.isLocal {
                    Section("Local development only") {
                        Text("This changes a test entitlement. It does not purchase an App Store subscription.")
                        Button(runtime.profile?.isPlus == true ? "Switch to test Free" : "Enable test Plus") { Task { do { try await runtime.billing.localEntitlement(runtime.profile?.isPlus != true, runtime: runtime) } catch { runtime.handle(error) } } }.accessibilityIdentifier("billing.toggleTestPlan")
                    }
                } else {
                    Button("Restore Purchases") { Task { await runtime.billing.restore(runtime: runtime) } }
                    Button("Manage Subscription") { openURL(URL(string: "https://apps.apple.com/account/subscriptions")!) }
                }
                if let error = runtime.billing.error { Text(error).foregroundStyle(Theme.destructive) }
            }
        }.featureListStyle().navigationTitle("Subscription")
    }
}

struct PolicyView: View {
    let privacy: Bool
    private var publishedURL: URL? {
        guard let raw = Bundle.main.object(forInfoDictionaryKey: privacy ? "MyCoachPrivacyURL" : "MyCoachTermsURL") as? String, let url = URL(string: raw), url.scheme == "https", url.host != nil, url.user == nil, url.password == nil else { return nil }; return url
    }
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                Text(privacy ? "Privacy and your data" : "Using MyCoach").font(.title.bold())
                Text(privacy ? "MyCoach processes the account details, selected photos, conversation text, notes and feedback you submit to provide its features. Production sign-in uses Supabase Auth, generated suggestions use Google Gemini, and subscriptions use Apple and RevenueCat. Local development uses separate test providers. We do not track you across apps or websites." : "MyCoach is for adults 18 and over. Use only content you have permission to share. Keep messages respectful and PG-13. AI suggestions may be inaccurate; review and edit them before sending. No reply or relationship outcome is guaranteed.")
                Text(privacy ? "Photos are not retained by default. The Store images setting controls optional retention; selected content is still processed to generate suggestions. You can delete conversation history, saved content, style samples and your account in the app. Deleting your account requests removal of its backend records and stored images. Clipboard copying happens only when you tap Copy." : "Subscription prices and periods come from the App Store. Subscriptions renew automatically until canceled in Apple account settings. Account deletion does not cancel an Apple subscription. You can restore eligible purchases from Subscription.")
                if let publishedURL { Link("Read the published policy", destination: publishedURL) }
                if publishedURL == nil { Text("Development build: the publisher's final legal identity, support contact and hosted policy URLs must be configured and reviewed before App Store submission.").font(.footnote).foregroundStyle(Theme.muted) }
            }.padding(24)
        }.background(Theme.background).navigationTitle(privacy ? "Privacy" : "Terms").navigationBarTitleDisplayMode(.inline)
    }
}
