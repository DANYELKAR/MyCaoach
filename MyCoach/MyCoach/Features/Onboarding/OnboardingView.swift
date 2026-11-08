import SwiftUI
import AuthenticationServices
import CryptoKit
import Security

struct OnboardingView: View {
    @Environment(AppState.self) private var state
    let runtime: AppRuntime
    private enum Field: Hashable { case name, email, password, code }
    @FocusState private var focused: Field?
    @State private var step = 0
    @State private var age = false
    @State private var consent = false
    @State private var name = ""
    @State private var goal = "Start a conversation"
    @State private var humor = 50.0
    @State private var email = ""
    @State private var password = ""
    @State private var code = ""
    @State private var otpSent = false
    @State private var register = true
    @State private var busy = false
    @State private var nonce = ""
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    BrandMark(size: 72)
                    Text(title).font(.largeTitle.bold()).fixedSize(horizontal: false, vertical: true)
                    if step < 3 {
                        Text(["A thoughtful first message starts with something real. Choose your tone and make it your own.", "Remember the details. Save your favorite messages and keep each connection's story together.", "Bring your own voice. You're always in control of what you share and send."][step]).foregroundStyle(Theme.muted)
                        PrimaryButton(title: step == 2 ? "Make it yours" : "Continue") { step += 1 }.accessibilityIdentifier("onboarding.continue")
                    } else if step == 3 {
                        TextField("Your name (optional)", text: $name).textContentType(.givenName).focused($focused, equals: .name)
                        Picker("Your goal", selection: $goal) { ForEach(["Start a conversation", "Sound more like myself", "Keep a chat going"], id: \.self) { Text($0) } }
                        Text("A little humor: \(Int(humor))%")
                        Slider(value: $humor, in: 0...100, step: 1).accessibilityLabel("Humor level")
                        PrimaryButton(title: "Continue") { step += 1 }.accessibilityIdentifier("onboarding.continue")
                    } else if step == 4 {
                        Text("MyCoach sends the photos, conversation text and notes you choose to our backend and Google Gemini to generate suggestions. Only share content you have permission to use. Avoid sensitive information. AI may make mistakes.").foregroundStyle(Theme.muted)
                        Toggle("I am 18 or older", isOn: $age).accessibilityIdentifier("consent.age")
                        Toggle("I consent to sending my selected content to third-party AI", isOn: $consent).accessibilityIdentifier("consent.ai")
                        Text("Images aren't stored by default. You can change this in Privacy and Data. You can delete your history and account in the app.").font(.footnote).foregroundStyle(Theme.muted)
                        PrimaryButton(title: runtime.hasSession ? "Finish setup" : "Continue", isLoading: busy) {
                            if runtime.hasSession { run { try await saveConsent() } } else { step = 5 }
                        }.disabled(!age || !consent).accessibilityIdentifier("onboarding.consentContinue")
                    } else {
                        signIn
                    }
                }.padding(24).foregroundStyle(Theme.label)
            }.scrollDismissesKeyboard(.interactively).background(Theme.background).navigationTitle("MyCoach").navigationBarTitleDisplayMode(.inline)
                .toolbar { if step > 0 { ToolbarItem(placement: .cancellationAction) { Button("Back") { step -= 1 } } } }
                .onAppear { if runtime.hasSession { step = 4 } }
        }
    }
    private var title: String { ["Better hellos. More you.", "Every connection has a story.", "Your words. Your choice.", "A voice that feels like you.", "A little trust, first.", "Welcome to MyCoach"][step] }
    @ViewBuilder private var signIn: some View {
        if runtime.auth.isLocal {
            Label("Local development · test AI and billing", systemImage: "hammer").font(.footnote).foregroundStyle(Theme.warning)
            TextField("Email", text: $email).textContentType(.emailAddress).keyboardType(.emailAddress).textInputAutocapitalization(.never).autocorrectionDisabled().focused($focused, equals: .email).accessibilityIdentifier("auth.email")
            SecureField("Password (10–72 characters)", text: $password).textContentType(runtime.auth.localPasswordContentType).focused($focused, equals: .password).accessibilityIdentifier("auth.password")
            Toggle("Create a local account", isOn: $register)
            PrimaryButton(title: "Sign in", isLoading: busy) {
                run { try await runtime.auth.localSignIn(api: runtime.api, email: email, password: password, register: register); try await finish() }
            }.disabled(!email.contains("@") || password.count < 10 || password.utf8.count > 72).accessibilityIdentifier("auth.submit")
        } else {
            SignInWithAppleButton(.signIn) { request in
                var bytes = [UInt8](repeating: 0, count: 32)
                guard SecRandomCopyBytes(kSecRandomDefault, bytes.count, &bytes) == errSecSuccess else { runtime.errorMessage = "Unable to start secure sign-in."; return }
                nonce = Data(bytes).base64EncodedString()
                request.nonce = SHA256.hash(data: Data(nonce.utf8)).map { String(format: "%02x", $0) }.joined()
                request.requestedScopes = [.fullName, .email]
            } onCompletion: { result in
                run {
                    let value = try result.get()
                    guard let credential = value.credential as? ASAuthorizationAppleIDCredential, let data = credential.identityToken, let token = String(data: data, encoding: .utf8) else { throw URLError(.userAuthenticationRequired) }
                    try await runtime.auth.apple(idToken: token, nonce: nonce); try await finish()
                }
            }.signInWithAppleButtonStyle(.white).frame(height: 52).disabled(busy)
            SecondaryButton(title: "Continue with Google") {
                run {
                    guard let scene = UIApplication.shared.connectedScenes.first as? UIWindowScene, var controller = scene.windows.first(where: \.isKeyWindow)?.rootViewController else { return }
                    while let presented = controller.presentedViewController { controller = presented }
                    try await runtime.auth.google(presenting: controller); try await finish()
                }
            }.disabled(busy)
            TextField("Email", text: $email).keyboardType(.emailAddress).textInputAutocapitalization(.never).autocorrectionDisabled()
            if otpSent { TextField("Email verification code", text: $code).textContentType(.oneTimeCode).keyboardType(.numberPad) }
            PrimaryButton(title: otpSent ? "Verify code" : "Email me a code", isLoading: busy) {
                run {
                    if otpSent { try await runtime.auth.verifyOTP(email: email, code: code); try await finish() }
                    else { try await runtime.auth.sendOTP(email: email); otpSent = true }
                }
            }.disabled(!email.contains("@") || (otpSent && code.isEmpty))
            if otpSent { Button("Use a different email") { otpSent = false; code = "" } }
        }
    }
    private func finish() async throws { try await runtime.authenticated(); try await saveConsent() }
    private func saveConsent() async throws {
        try await runtime.updateProfile(["ageConfirmed": .b(age), "aiConsent": .b(consent), "displayName": .s(String(name.prefix(80))), "goals": .a([.s(goal)]), "humorLevel": .n(humor), "timezone": .s(TimeZone.current.identifier)])
    }
    private func run(_ action: @escaping @MainActor () async throws -> Void) {
        focused = nil
        UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil)
        busy = true
        Task { do { try await action() } catch { runtime.handle(error) }; busy = false }
    }
}
