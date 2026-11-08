import SwiftUI

struct PeopleView: View {
    @Environment(AppState.self) private var state
    @State private var data = RemoteCollection<ContactDTO>()
    @State private var search = ""
    @State private var status = "ALL"
    @State private var adding = false
    private var filtered: [ContactDTO] { data.items.filter { (status == "ALL" || $0.status == status) && (search.isEmpty || $0.name.localizedCaseInsensitiveContains(search)) } }
    var body: some View {
        List {
            if state.runtime?.profile?.isPlus == false { PlanNotice(text: "Free includes up to 3 people.") }
            Picker("Status", selection: $status) { Text("All").tag("ALL"); ForEach(["NEW", "TALKING", "DATE_PLANNED", "ENDED"], id: \.self) { Text($0.replacingOccurrences(of: "_", with: " ").capitalized).tag($0) } }
            ForEach(filtered) { person in
                NavigationLink { PersonDetailView(person: person) } label: {
                    HStack(spacing: 14) {
                        Image(systemName: "person.crop.circle.fill").font(.title).foregroundStyle(Theme.primary).accessibilityHidden(true)
                        VStack(alignment: .leading, spacing: 5) { Text(person.name).font(.headline); Text(person.status.replacingOccurrences(of: "_", with: " ").capitalized).font(.caption).foregroundStyle(Theme.muted) }
                    }.padding(.vertical, 8)
                }.accessibilityIdentifier("person.\(person.name)")
            }
            if filtered.isEmpty && !data.isLoading && data.error == nil { EmptyStateView(symbol: "person.2", title: "Every connection has a story.", message: "Add a person to keep their interests and conversations together.") }
            CollectionFooter(collection: data) { await load(reset: data.error != nil) }
        }.featureListStyle().navigationTitle("People").searchable(text: $search, prompt: "Search people")
            .toolbar { Button("Add person", systemImage: "plus") { adding = true }.accessibilityIdentifier("people.add") }
            .sheet(isPresented: $adding, onDismiss: { Task { await load() } }) { ContactEditor() }
            .task { await load() }.refreshable { await load() }
    }
    private func load(reset: Bool = true) async { if let api = state.runtime?.api { await data.load(api, path: "v1/contacts", reset: reset) } }
}

struct ContactEditor: View {
    @Environment(AppState.self) private var state
    let person: ContactDTO?
    @State private var name: String
    @State private var source: String
    @State private var status: String
    @State private var interests: String
    @State private var notes: String
    init(person: ContactDTO? = nil) {
        self.person = person
        _name = State(initialValue: person?.name ?? ""); _source = State(initialValue: person?.source ?? "OTHER")
        _status = State(initialValue: person?.status ?? "NEW"); _interests = State(initialValue: person?.interests.joined(separator: ", ") ?? ""); _notes = State(initialValue: person?.notes ?? "")
    }
    var body: some View {
        EditorShell(title: person == nil ? "Add person" : "Edit person", valid: !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && name.utf16.count <= 100 && notes.utf16.count <= 3000 && interestValues.count <= 20 && interestValues.allSatisfy { $0.utf16.count <= 80 }, save: save) {
            Section("The little details") {
                TextField("Name", text: $name).accessibilityIdentifier("person.name")
                Picker("Source", selection: $source) { ForEach(["HINGE", "TINDER", "BUMBLE", "INSTAGRAM", "IN_PERSON", "OTHER"], id: \.self) { Text($0.replacingOccurrences(of: "_", with: " ").capitalized).tag($0) } }
                Picker("Status", selection: $status) { ForEach(["NEW", "TALKING", "DATE_PLANNED", "ENDED"], id: \.self) { Text($0.replacingOccurrences(of: "_", with: " ").capitalized).tag($0) } }
                TextField("Interests, separated by commas", text: $interests, axis: .vertical)
                TextField("Private notes", text: $notes, axis: .vertical).lineLimit(3...8)
            }
            Text("Person details are included when you generate a message for this person. Don't add sensitive information.").font(.footnote)
        }
    }
    private var interestValues: [String] { interests.split(separator: ",").map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty } }
    private func save() async throws {
        guard let api = state.runtime?.api else { return }
        let _: ContactDTO = try await api.request("v1/contacts" + (person.map { "/\($0.id)" } ?? ""), method: person == nil ? "POST" : "PATCH", body: ["name": .s(name), "source": .s(source), "status": .s(status), "interests": .a(interestValues.map(JSONValue.s)), "notes": .s(notes)])
    }
}

struct PersonDetailView: View {
    @Environment(AppState.self) private var state
    @Environment(\.dismiss) private var dismiss
    @State var person: ContactDTO
    @State private var editing = false
    @State private var deleting = false
    @State private var history = RemoteCollection<ThreadDTO>()
    var body: some View {
        List {
            Section {
                Text(person.name).font(.title.bold())
                Text(person.status.replacingOccurrences(of: "_", with: " ").capitalized).foregroundStyle(Theme.muted)
                NavigationLink("New opener") { GenerationView(contactID: person.id) }
            }
            Section("Interests") { if person.interests.isEmpty { Text("No interests added yet.").foregroundStyle(Theme.muted) }; ForEach(person.interests, id: \.self) { Text($0) } }
            Section("Your notes") { Text(person.notes?.isEmpty == false ? person.notes! : "No notes yet.") }
            Section("Conversations") {
                ForEach(history.items.filter { $0.contactId == person.id }) { thread in NavigationLink(thread.title ?? "Conversation") { TimelineView(id: thread.id) } }
                CollectionFooter(collection: history) { await loadHistory(reset: false) }
            }
            Button("Delete person", role: .destructive) { deleting = true }
        }.featureListStyle().navigationTitle("Person").navigationBarTitleDisplayMode(.inline)
            .toolbar { Button("Edit") { editing = true } }
            .sheet(isPresented: $editing, onDismiss: { Task { await refreshPerson() } }) { ContactEditor(person: person) }
            .confirmationDialog("Delete this person? Their conversation history will remain without a linked person.", isPresented: $deleting, titleVisibility: .visible) { Button("Delete person", role: .destructive) { Task { do { try await state.runtime?.api.delete("v1/contacts/\(person.id)"); dismiss() } catch { state.runtime?.handle(error) } } } }
            .task { await loadHistory() }
    }
    private func loadHistory(reset: Bool = true) async { if let api = state.runtime?.api { await history.load(api, path: "v1/threads", reset: reset) } }
    private func refreshPerson() async { do { if let api = state.runtime?.api { person = try await api.request("v1/contacts/\(person.id)") } } catch { state.runtime?.handle(error) } }
}
