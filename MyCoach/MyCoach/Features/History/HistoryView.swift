import SwiftUI

struct HistoryView: View {
    @Environment(AppState.self) private var state
    @State private var data = RemoteCollection<ThreadDTO>()
    @State private var pendingDelete: ThreadDTO?
    @State private var filter = "ALL"
    var body: some View {
        List {
            if state.runtime?.profile?.isPlus == false { PlanNotice(text: "Free shows the last 7 days of history.") }
            Picker("Type", selection: $filter) { Text("All").tag("ALL"); Text("Openers").tag("OPENER"); Text("Replies").tag("REPLY") }
            ForEach(data.items) { thread in
                NavigationLink { TimelineView(id: thread.id) } label: {
                    VStack(alignment: .leading, spacing: 6) { Text(thread.title ?? "Conversation").font(.headline); Text(thread.lastActivityAt.prefix(10)).font(.caption).foregroundStyle(Theme.muted) }.padding(.vertical, 8)
                }.swipeActions { Button("Delete", role: .destructive) { pendingDelete = thread } }
            }
            if data.items.isEmpty && !data.isLoading && data.error == nil { EmptyStateView(symbol: "clock.arrow.circlepath", title: "Room for your next hello.", message: "Your conversations will appear here after you create a message.") }
            CollectionFooter(collection: data) { await load(reset: data.error != nil) }
        }.featureListStyle().navigationTitle("History").task(id: filter) { await load() }.refreshable { await load() }
            .confirmationDialog("Delete this conversation?", isPresented: Binding(get: { pendingDelete != nil }, set: { if !$0 { pendingDelete = nil } }), titleVisibility: .visible) {
                Button("Delete conversation", role: .destructive) { if let id = pendingDelete?.id { Task { do { try await state.runtime?.api.delete("v1/threads/\(id)"); await load() } catch { state.runtime?.handle(error) } } }; pendingDelete = nil }
            }
    }
    private func load(reset: Bool = true) async { if let api = state.runtime?.api { await data.load(api, path: "v1/threads" + (filter == "ALL" ? "" : "?type=\(filter)"), reset: reset) } }
}

struct TimelineView: View {
    @Environment(AppState.self) private var state
    let id: UUID
    @State private var timeline: TimelineDTO?
    @State private var error: String?
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                if let timeline {
                    ForEach(timeline.generations, id: \.generation.id) { result in
                        Text(result.generation.type == "REPLY" ? "Replies" : "Opener").font(.headline)
                        ResultsView(result: result)
                    }
                } else if let error { Text(error); Button("Try again") { Task { await load() } } }
                else { ProgressView("Loading conversation…") }
            }.padding(20)
        }.background(Theme.background).navigationTitle(timeline?.thread.title ?? "Conversation").navigationBarTitleDisplayMode(.inline).task { await load() }.refreshable { await load() }
    }
    private func load() async { do { if let api = state.runtime?.api { timeline = try await api.request("v1/threads/\(id)"); error = nil } } catch { if !Task.isCancelled { self.error = error.localizedDescription } } }
}
