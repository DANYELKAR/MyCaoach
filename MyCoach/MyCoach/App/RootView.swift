import SwiftUI

struct RootView: View {
    @Environment(AppState.self) private var state
    var body: some View {
        ZStack {
            if let runtime = state.runtime {
                if runtime.isRestoring { ProgressView("Opening MyCoach…").frame(maxWidth: .infinity, maxHeight: .infinity).background(Theme.background) }
                else if !runtime.hasSession || !runtime.onboardingComplete { OnboardingView(runtime: runtime) }
                else { MainTabView() }
            } else { MainTabView() }
        }
        .task { await state.runtime?.start() }
        .sheet(isPresented: Binding(get: { state.runtime?.showPaywall == true }, set: { state.runtime?.showPaywall = $0 })) { PaywallView() }
        .alert("MyCoach", isPresented: Binding(get: { state.runtime?.errorMessage != nil }, set: { if !$0 { state.runtime?.errorMessage = nil } })) {
            Button("OK") { state.runtime?.errorMessage = nil }
        } message: { Text(state.runtime?.errorMessage ?? "") }
        .overlay(alignment: .bottom) {
            if let message = state.runtime?.toast { ToastBanner(message: LocalizedStringKey(message)).padding(.bottom, 88).padding(.horizontal).task(id: message) { try? await Task.sleep(for: .seconds(2)); if state.runtime?.toast == message { state.runtime?.toast = nil } }.allowsHitTesting(false) }
        }
        .preferredColorScheme(.dark)
    }
}

struct MainTabView: View {
    @Environment(AppState.self) private var state
    var body: some View {
        @Bindable var state = state
        TabView(selection: $state.selectedTab) {
            NavigationStack { HomeView() }
                .tabItem { Label("Home", systemImage: AppTab.home.symbol) }.tag(AppTab.home)
            NavigationStack {
                if state.runtime != nil { PeopleView() } else { EmptyFeatureView(symbol: "person.2", title: "Every connection has a story.", message: "Keep the little details, interests, and conversations together.")
                    .navigationTitle("People") }
            }.tabItem { Label("People", systemImage: AppTab.people.symbol) }.tag(AppTab.people)
            NavigationStack {
                if state.runtime != nil { HistoryView() } else { EmptyFeatureView(symbol: "clock.arrow.circlepath", title: "Room for your next hello.", message: "Your message history will live here, ready when you need it.")
                    .navigationTitle("History") }
            }.tabItem { Label("History", systemImage: AppTab.history.symbol) }.tag(AppTab.history)
            NavigationStack {
                if state.runtime != nil { SavedView() } else { EmptyFeatureView(symbol: "heart", title: "Keep the ones you love.", message: "Your favorite messages, in one little collection.")
                    .navigationTitle("Saved") }
            }.tabItem { Label("Saved", systemImage: AppTab.saved.symbol) }.tag(AppTab.saved)
            NavigationStack { ProfileView() }
                .tabItem { Label("Profile", systemImage: AppTab.profile.symbol) }.tag(AppTab.profile)
        }.tint(Theme.primary).toolbarBackground(Theme.surface, for: .tabBar)
            .toolbarBackground(.visible, for: .tabBar)
            .preferredColorScheme(.dark)
    }
}

private struct EmptyFeatureView: View {
    let symbol: String
    let title: LocalizedStringKey
    let message: LocalizedStringKey
    var body: some View {
        ScrollView {
            EmptyStateView(symbol: symbol, title: title, message: message).padding(.top, 80)
        }.frame(maxWidth: .infinity, maxHeight: .infinity).background(Theme.background)
    }
}
