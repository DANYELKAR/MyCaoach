import Foundation
import Supabase
import GoogleSignIn
import UIKit

@MainActor
final class AuthService {
    struct LocalSession: Codable { let userId: UUID; let accessToken: String; let expiresIn: Int }
    let configuration: AppConfiguration
    let supabase: SupabaseClient?
    private let keychain: KeychainStore
    private var localSession: LocalSession?
    var userID: UUID?
    var isLocal: Bool { configuration.backendMode == .local }
    deinit {}
    init(configuration: AppConfiguration, namespace: String = "com.danyelkar.mycoach.auth") {
        self.configuration = configuration; self.keychain = KeychainStore(namespace: namespace)
        if let url = configuration.supabaseURL, let key = configuration.supabasePublishableKey {
            supabase = SupabaseClient(supabaseURL: url, supabaseKey: key, options: .init(auth: .init(storage: KeychainStore(namespace: namespace), storageKey: "mycoach-session")))
        } else { supabase = nil }
        if isLocal, let data = try? keychain.retrieve(key: "local"), let value = try? JSONDecoder().decode(LocalSession.self, from: data) {
            localSession = value; userID = value.userId
        }
    }
    func restore() async {
        if let session = try? await supabase?.auth.session { userID = session.user.id }
    }
    func accessToken() async throws -> String? {
        if isLocal { return localSession?.accessToken }
        return try await supabase?.auth.session.accessToken
    }
    func refresh() async throws {
        guard let supabase else { throw APIError(code: "UNAUTHORIZED", message: "Please sign in again.", requestId: nil) }
        let session = try await supabase.auth.refreshSession(); userID = session.user.id
    }
    func localSignIn(api: APIClient, email: String, password: String, register: Bool) async throws {
        #if DEBUG
        guard isLocal else { return }
        let value: LocalSession = try await api.request("local/auth/" + (register ? "register" : "login"), method: "POST", body: ["email": .s(email), "password": .s(password)], authenticated: false)
        try keychain.store(key: "local", value: JSONEncoder().encode(value))
        localSession = value; userID = value.userId
        #else
        throw APIError(code: "UNAUTHORIZED", message: "Local sign-in is unavailable.", requestId: nil)
        #endif
    }
    func sendOTP(email: String) async throws { try await requiredClient().auth.signInWithOTP(email: email) }
    func verifyOTP(email: String, code: String) async throws {
        _ = try await requiredClient().auth.verifyOTP(email: email, token: code, type: .email)
        userID = try await requiredClient().auth.session.user.id
    }
    func apple(idToken: String, nonce: String) async throws {
        let session = try await requiredClient().auth.signInWithIdToken(credentials: .init(provider: .apple, idToken: idToken, nonce: nonce)); userID = session.user.id
    }
    func google(presenting: UIViewController) async throws {
        guard let id = configuration.googleClientID else { throw setupError }
        GIDSignIn.sharedInstance.configuration = GIDConfiguration(clientID: id)
        let result = try await GIDSignIn.sharedInstance.signIn(withPresenting: presenting)
        guard let token = result.user.idToken?.tokenString else { throw setupError }
        let session = try await requiredClient().auth.signInWithIdToken(credentials: .init(provider: .google, idToken: token, accessToken: result.user.accessToken.tokenString))
        userID = session.user.id
    }
    private var setupError: APIError { APIError(code: "CONFIGURATION", message: "Sign-in isn't configured for this build. Please contact support.", requestId: nil) }
    private func requiredClient() throws -> SupabaseClient { guard let supabase else { throw setupError }; return supabase }
    func signOut() async {
        // Local removal must succeed even if the remote session has already expired.
        try? await supabase?.auth.signOut(scope: .local)
        clearLocal()
        GIDSignIn.sharedInstance.signOut()
    }
    func clearLocal() { try? keychain.remove(key: "mycoach-session"); try? keychain.remove(key: "local"); localSession = nil; userID = nil }
}
