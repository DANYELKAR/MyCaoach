import SwiftUI

struct PrivacyDataView: View {
    @Environment(AppState.self) private var state
    @State private var busy = false
    @State private var deleteHistory = false
    var body: some View {
        Form {
            if let runtime = state.runtime {
                Section("Your data") {
                    Toggle("Store images", isOn: Binding(get: { runtime.profile?.storeImages == true }, set: { update("storeImages", value: $0) })).disabled(busy).accessibilityIdentifier("privacy.storeImages")
                    Text("Off by default. Selected content is still sent to Google Gemini for generation. Turning this off affects future generations; delete history or your account to remove already stored inputs.").font(.footnote).foregroundStyle(Theme.muted)
                    Toggle("Allow anonymous product analytics", isOn: Binding(get: { runtime.profile?.analyticsOptIn == true }, set: { update("analyticsOptIn", value: $0) })).disabled(busy)
                    Text("The current build uses a no-op analytics provider. Photos, notes and message text are never analytics payloads.").font(.footnote).foregroundStyle(Theme.muted)
                }
                Section("Your controls") {
                    Button("Delete all history", role: .destructive) { deleteHistory = true }.disabled(busy)
                    Text("Deleting history removes conversation content and stored inputs. Usage records remain to enforce today's credit limit; saved message text remains until you delete it separately.").font(.footnote).foregroundStyle(Theme.muted)
                    NavigationLink("Delete account") { DeleteAccountView() }
                    NavigationLink("Privacy policy") { PolicyView(privacy: true) }
                }
            }
        }.featureListStyle().navigationTitle("Privacy and Data")
            .confirmationDialog("Permanently delete all conversation history?", isPresented: $deleteHistory, titleVisibility: .visible) {
                Button("Delete all history", role: .destructive) { busy = true; Task { do { try await state.runtime?.api.delete("v1/threads"); state.runtime?.toast = "History deleted" } catch { state.runtime?.handle(error) }; busy = false } }
            }
    }
    private func update(_ key: String, value: Bool) { busy = true; Task { do { try await state.runtime?.updateProfile([key: .b(value)]) } catch { state.runtime?.handle(error) }; busy = false } }
}

struct SettingsView: View {
    @Environment(AppState.self) private var state
    @AppStorage("appearance") private var appearance = "dark"
    @State private var busy = false
    var body: some View {
        Form {
            Picker("Appearance", selection: $appearance) { Text("System").tag("system"); Text("Dark").tag("dark"); Text("Light").tag("light") }
            if let runtime = state.runtime {
                Picker("Default tone", selection: Binding(get: { runtime.profile?.defaultToneId }, set: { id in
                    guard let id else { return }; busy = true
                    Task { do { try await runtime.updateProfile(["defaultToneId": .s(id.uuidString)]); if let name = runtime.tones.first(where: { $0.id == id })?.name, let tone = BuiltInTone(rawValue: name.lowercased()) { state.defaultTone = tone } } catch { runtime.handle(error) }; busy = false }
                })) { Text("Choose a tone").tag(nil as UUID?); ForEach(runtime.tones.filter { $0.builtIn || runtime.profile?.isPlus == true }) { Text($0.name).tag(Optional($0.id)) } }.disabled(busy)
                Text("Daily credits reset in \(runtime.profile?.timezone ?? TimeZone.current.identifier). Your timezone is recorded during setup.").font(.footnote).foregroundStyle(Theme.muted)
                Button("Use current timezone") { Task { do { try await runtime.updateProfile(["timezone": .s(TimeZone.current.identifier)]); runtime.usage = try await runtime.api.request("v1/me/usage") } catch { runtime.handle(error) } } }
            }
        }.featureListStyle().navigationTitle("Settings")
    }
}

struct DeleteAccountView: View {
    @Environment(AppState.self) private var state
    @State private var confirmation = ""
    @State private var busy = false
    @State private var error: String?
    var body: some View {
        Form {
            Section {
                Text("This permanently deletes your account, people, conversations, saved messages, style samples and stored images. This cannot be undone.")
                Text("Deleting your account does not cancel an Apple subscription. Manage any subscription in your Apple account first.").foregroundStyle(Theme.warning)
                TextField("Type DELETE to confirm", text: $confirmation).textInputAutocapitalization(.characters).autocorrectionDisabled().accessibilityIdentifier("account.confirmation")
                Button("Delete my account", role: .destructive) {
                    guard let runtime = state.runtime else { return }; UIApplication.shared.sendAction(#selector(UIResponder.resignFirstResponder), to: nil, from: nil, for: nil); busy = true; error = nil
                    Task {
                        do { try await runtime.api.delete("v1/me"); await runtime.signOut(); state.defaultTone = .casual; state.selectedTab = .home }
                        catch { self.error = error.localizedDescription }; busy = false
                    }
                }.disabled(confirmation != "DELETE" || busy).accessibilityIdentifier("account.delete")
            }
            if busy { ProgressView("Deleting account…") }
            if let error { Text(error).foregroundStyle(Theme.destructive) }
        }.featureListStyle().navigationTitle("Delete account").navigationBarTitleDisplayMode(.inline).interactiveDismissDisabled(busy)
    }
}

struct HelpView: View {
    @Environment(AppState.self) private var state
    private var supportEmail: String? { Bundle.main.object(forInfoDictionaryKey: "MyCoachSupportEmail") as? String }
    var body: some View {
        List {
            Text("MyCoach helps you draft respectful messages. Review every suggestion before sending. It can't predict replies or relationship outcomes.")
            NavigationLink("Privacy policy") { PolicyView(privacy: true) }
            NavigationLink("Terms of use") { PolicyView(privacy: false) }
            if let supportEmail, supportEmail.contains("@"), !supportEmail.contains("$("), let url = URL(string: "mailto:\(supportEmail)") { Link("Contact support", destination: url) }
            else { Text("Support contact hasn't been configured for this development build.").foregroundStyle(Theme.muted) }
            Text("MyCoach 1.0 · iOS 17+").font(.footnote).foregroundStyle(Theme.muted)
        }.featureListStyle().navigationTitle("Help and Feedback")
    }
}
