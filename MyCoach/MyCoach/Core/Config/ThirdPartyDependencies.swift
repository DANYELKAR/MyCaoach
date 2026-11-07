import GoogleSignIn
import RevenueCat
import Supabase

/// Dependency wiring only. Auth and billing flows are implemented in their feature phases.
enum ThirdPartyDependencies {
    static func makeSupabaseClient(configuration: AppConfiguration) -> SupabaseClient? {
        guard let url = configuration.supabaseURL, let key = configuration.supabasePublishableKey else { return nil }
        return SupabaseClient(supabaseURL: url, supabaseKey: key)
    }
}
