import Foundation

struct AppConfiguration: Equatable {
    enum BackendMode: String { case local, supabase }
    enum ConfigurationError: LocalizedError, Equatable {
        case missingAPIURL, insecureURL, invalidBackendMode, invalidSupabaseConfiguration, serverKeyInClient
        var errorDescription: String? {
            switch self {
            case .missingAPIURL: "The API URL is missing or invalid."
            case .insecureURL: "An HTTPS URL is required outside local development."
            case .invalidBackendMode: "The backend mode is invalid."
            case .invalidSupabaseConfiguration: "Set both the Supabase URL and publishable key."
            case .serverKeyInClient: "Server credentials must never be included in the iOS app."
            }
        }
    }

    let apiBaseURL: URL
    let backendMode: BackendMode
    let supabaseURL: URL?
    let supabasePublishableKey: String?
    let googleClientID: String?
    let revenueCatPublicKey: String?

    static func load(bundle: Bundle = .main) throws -> Self {
        try parse(bundle.infoDictionary ?? [:])
    }

    static func parse(_ values: [String: Any]) throws -> Self {
        func value(_ key: String) -> String? {
            guard let raw = values[key] as? String else { return nil }
            let trimmed = raw.trimmingCharacters(in: .whitespacesAndNewlines)
            return trimmed.isEmpty || trimmed.contains("$(") ? nil : trimmed
        }
        guard let mode = value("MyCoachBackendMode").flatMap(BackendMode.init(rawValue:)) else {
            throw ConfigurationError.invalidBackendMode
        }
        guard let raw = value("MyCoachAPIBaseURL"), let url = URL(string: raw), url.host != nil,
              url.user == nil, url.password == nil, url.query == nil, url.fragment == nil else {
            throw ConfigurationError.missingAPIURL
        }
        let loopback = ["localhost", "127.0.0.1", "::1"].contains(url.host ?? "")
        guard url.scheme == "https" || (mode == .local && url.scheme == "http" && loopback) else {
            throw ConfigurationError.insecureURL
        }
        #if !DEBUG
        guard mode == .supabase, url.scheme == "https" else { throw ConfigurationError.insecureURL }
        #endif
        let supabaseRaw = value("MyCoachSupabaseURL")
        let key = value("MyCoachSupabasePublishableKey")
        guard (supabaseRaw == nil) == (key == nil) else { throw ConfigurationError.invalidSupabaseConfiguration }
        let supabaseURL = supabaseRaw.flatMap(URL.init(string:))
        if supabaseRaw != nil {
            guard let supabaseURL, supabaseURL.scheme == "https", supabaseURL.host != nil else {
                throw ConfigurationError.invalidSupabaseConfiguration
            }
        }
        if let key {
            guard !key.hasPrefix("sb_secret_") else { throw ConfigurationError.serverKeyInClient }
            if key.split(separator: ".").count == 3 {
                let segment = String(key.split(separator: ".")[1]).replacingOccurrences(of: "-", with: "+").replacingOccurrences(of: "_", with: "/")
                let padded = segment + String(repeating: "=", count: (4 - segment.count % 4) % 4)
                if let data = Data(base64Encoded: padded), let payload = try? JSONSerialization.jsonObject(with: data) as? [String: Any], payload["role"] as? String == "service_role" {
                    throw ConfigurationError.serverKeyInClient
                }
            }
        }
        return Self(apiBaseURL: url, backendMode: mode, supabaseURL: supabaseURL, supabasePublishableKey: key,
                    googleClientID: value("MyCoachGoogleClientID"), revenueCatPublicKey: value("MyCoachRevenueCatPublicKey"))
    }
}
