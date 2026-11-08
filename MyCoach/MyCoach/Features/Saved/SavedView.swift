import SwiftUI

struct SavedView: View {
    @Environment(AppState.self) private var state
    @State private var data = RemoteCollection<SavedDTO>()
    @State private var folders: [FolderDTO] = []
    @State private var folderID: UUID?
    @State private var selected: SavedDTO?
    @State private var adding = false
    var body: some View {
        List {
            Picker("Folder", selection: $folderID) { Text("All saved").tag(nil as UUID?); ForEach(folders) { Text($0.name).tag(Optional($0.id)) } }
            NavigationLink("Manage folders") { FoldersView() }
            ForEach(data.items) { item in
                VStack(alignment: .leading, spacing: 12) {
                    if let title = item.title, !title.isEmpty { Text(title).font(.headline) }
                    Text(item.text).textSelection(.enabled)
                    HStack { Button("Copy", systemImage: "doc.on.doc") { UIPasteboard.general.string = item.text; state.runtime?.toast = "Copied to clipboard" }; Spacer(); Button("Edit") { selected = item } }.buttonStyle(.borderless)
                }.padding(.vertical, 8).swipeActions { Button("Delete", role: .destructive) { Task { do { try await state.runtime?.api.delete("v1/saved/\(item.id)"); await load() } catch { state.runtime?.handle(error) } } } }
            }
            if data.items.isEmpty && !data.isLoading && data.error == nil { EmptyStateView(symbol: "heart", title: "Keep the ones you love.", message: "Save a suggestion with the heart button, or create a reusable template with Plus.") }
            CollectionFooter(collection: data) { await load(reset: data.error != nil) }
        }.featureListStyle().navigationTitle("Saved")
            .toolbar { Button("New template", systemImage: "plus") { state.runtime?.requirePlus { adding = true } } }
            .sheet(item: $selected, onDismiss: { Task { await load() } }) { SavedEditor(item: $0, folders: folders) }
            .sheet(isPresented: $adding, onDismiss: { Task { await load() } }) { SavedEditor(folders: folders) }
            .task(id: folderID) { await load(); await loadFolders() }.refreshable { await load(); await loadFolders() }
    }
    private func load(reset: Bool = true) async { if let api = state.runtime?.api { await data.load(api, path: "v1/saved" + (folderID.map { "?folderId=\($0)" } ?? ""), reset: reset) } }
    private func loadFolders() async { do { if let api = state.runtime?.api { folders = try await api.request("v1/folders"); if let folderID, !folders.contains(where: { $0.id == folderID }) { self.folderID = nil } } } catch { state.runtime?.handle(error) } }
}

struct SavedEditor: View {
    @Environment(AppState.self) private var state
    let item: SavedDTO?
    let folders: [FolderDTO]
    @State private var title: String
    @State private var text: String
    @State private var tags: String
    @State private var folderID: UUID?
    init(item: SavedDTO? = nil, folders: [FolderDTO]) {
        self.item = item; self.folders = folders; _title = State(initialValue: item?.title ?? ""); _text = State(initialValue: item?.text ?? ""); _tags = State(initialValue: item?.tags.joined(separator: ", ") ?? ""); _folderID = State(initialValue: item?.folderId)
    }
    var body: some View {
        EditorShell(title: item == nil ? "New template" : "Edit saved message", valid: !text.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && title.utf16.count <= 100 && text.utf16.count <= 2000 && tagValues.count <= 20 && tagValues.allSatisfy { $0.utf16.count <= 50 }, save: save) {
            TextField("Title", text: $title)
            TextField("Message", text: $text, axis: .vertical).lineLimit(4...10).disabled(item?.kind == "SUGGESTION")
            TextField("Tags, separated by commas", text: $tags)
            Picker("Folder", selection: $folderID) { Text("No folder").tag(nil as UUID?); ForEach(folders) { Text($0.name).tag(Optional($0.id)) } }
        }
    }
    private var tagValues: [String] { tags.split(separator: ",").map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty } }
    private func save() async throws {
        guard let api = state.runtime?.api else { return }
        var body: [String: JSONValue] = ["title": .s(title), "text": .s(text), "tags": .a(tagValues.map(JSONValue.s)), "clearFolder": .b(folderID == nil)]
        if let folderID { body["folderId"] = .s(folderID.uuidString) }
        if item == nil { body["kind"] = .s("TEMPLATE") }
        let _: SavedDTO = try await api.request("v1/saved" + (item.map { "/\($0.id)" } ?? ""), method: item == nil ? "POST" : "PATCH", body: body)
    }
}

struct FoldersView: View {
    @Environment(AppState.self) private var state
    @State private var folders: [FolderDTO] = []
    @State private var editing: FolderDTO?
    @State private var name = ""
    @State private var editor = false
    var body: some View {
        List {
            if state.runtime?.profile?.isPlus == false { PlanNotice(text: "Free includes up to 3 folders.") }
            ForEach(folders) { folder in
                Button(folder.name) { editing = folder; name = folder.name; editor = true }
                    .swipeActions { Button("Delete", role: .destructive) { Task { do { try await state.runtime?.api.delete("v1/folders/\(folder.id)"); await load() } catch { state.runtime?.handle(error) } } } }
            }
            if folders.isEmpty { Text("Create a folder to organize saved messages.").foregroundStyle(Theme.muted) }
        }.featureListStyle().navigationTitle("Folders").toolbar { Button("Add folder", systemImage: "plus") { editing = nil; name = ""; editor = true } }.task { await load() }
            .sheet(isPresented: $editor, onDismiss: { Task { await load() } }) {
                EditorShell(title: editing == nil ? "New folder" : "Rename folder", valid: !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty && name.utf16.count <= 80, save: {
                    let _: FolderDTO? = try await state.runtime?.api.request("v1/folders" + (editing.map { "/\($0.id)" } ?? ""), method: editing == nil ? "POST" : "PATCH", body: ["name": .s(name)])
                }) { TextField("Folder name", text: $name).accessibilityIdentifier("folder.name") }
            }
    }
    private func load() async { do { if let api = state.runtime?.api { folders = try await api.request("v1/folders") } } catch { state.runtime?.handle(error) } }
}
