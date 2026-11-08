import GoogleSignIn
import RevenueCat
import Supabase

/// Public SDK factory. Application auth uses an explicit Keychain-backed AuthService.
enum ThirdPartyDependencies {
    static func makeSupabaseClient(configuration: AppConfiguration) -> SupabaseClient? {
        guard let url = configuration.supabaseURL, let key = configuration.supabasePublishableKey else { return nil }
        return SupabaseClient(supabaseURL: url, supabaseKey: key)
    }
}
