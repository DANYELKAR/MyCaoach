import Foundation
import Observation
import RevenueCat

@MainActor @Observable
final class BillingService {
    var packages: [Package] = []
    var isLoading = false
    var error: String?
    deinit {}
    func load(runtime: AppRuntime) async {
        guard !runtime.auth.isLocal else { return }
        isLoading = true; error = nil
        defer { isLoading = false }
        do { try await configure(runtime: runtime); packages = try await Purchases.shared.offerings().current?.availablePackages ?? [] }
        catch { self.error = "Subscription options are unavailable. Please try again." }
    }
    private func configure(runtime: AppRuntime) async throws {
        guard let key = runtime.configuration.revenueCatPublicKey, key.hasPrefix("appl_"), let id = runtime.profile?.id else {
            throw APIError(code: "CONFIGURATION", message: "Subscriptions aren't configured for this build.", requestId: nil)
        }
        let user = id.uuidString.lowercased()
        if !Purchases.isConfigured { Purchases.logLevel = .error; Purchases.configure(withAPIKey: key, appUserID: user) }
        else if Purchases.shared.appUserID != user { _ = try await Purchases.shared.logIn(user) }
    }
    func purchase(_ package: Package, runtime: AppRuntime) async {
        guard !isLoading else { return }; isLoading = true; error = nil
        defer { isLoading = false }
        do {
            try await configure(runtime: runtime)
            let result = try await Purchases.shared.purchase(package: package)
            if result.userCancelled { return }
            try await sync(runtime: runtime)
            if runtime.profile?.isPlus == true { runtime.showPaywall = false }
            else { error = "Your purchase is processing. Use Refresh plan in Subscription in a moment." }
        } catch { self.error = "The purchase couldn't be completed. You can try again or restore an existing purchase." }
    }
    func restore(runtime: AppRuntime) async {
        guard !isLoading else { return }; isLoading = true; error = nil
        defer { isLoading = false }
        do {
            try await configure(runtime: runtime); _ = try await Purchases.shared.restorePurchases(); try await sync(runtime: runtime)
            if runtime.profile?.isPlus == true { runtime.showPaywall = false; runtime.toast = "Purchases restored" }
            else { error = "No active Plus subscription was found for this account." }
        } catch { self.error = "Purchases couldn't be restored. Please try again." }
    }
    func sync(runtime: AppRuntime) async throws {
        runtime.profile = try await runtime.api.request("v1/billing/sync", method: "POST")
        runtime.usage = try await runtime.api.request("v1/me/usage")
    }
    func localEntitlement(_ active: Bool, runtime: AppRuntime) async throws {
        #if DEBUG
        guard runtime.auth.isLocal else { return }
        let expiry = ISO8601DateFormatter().string(from: Date().addingTimeInterval(86400))
        runtime.profile = try await runtime.api.request("local/billing/entitlement", method: "PUT", body: ["active": .b(active), "expiresAt": active ? .s(expiry) : .null])
        runtime.usage = try await runtime.api.request("v1/me/usage")
        #endif
    }
    func signOut() async { packages = []; error = nil; if Purchases.isConfigured && !Purchases.shared.isAnonymous { _ = try? await Purchases.shared.logOut() } }
}
