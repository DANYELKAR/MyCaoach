import SwiftUI

struct RootView: View {
    @Environment(AppState.self) private var state
    var body: some View {
        @Bindable var state = state
        TabView(selection: $state.selectedTab) {
            NavigationStack { HomeView() }
                .tabItem { Label("Home", systemImage: AppTab.home.symbol) }.tag(AppTab.home)
            NavigationStack {
                EmptyFeatureView(symbol: "person.2", title: "Every connection has a story.", message: "Keep the little details, interests, and conversations together.")
                    .navigationTitle("People")
            }.tabItem { Label("People", systemImage: AppTab.people.symbol) }.tag(AppTab.people)
            NavigationStack {
                EmptyFeatureView(symbol: "clock.arrow.circlepath", title: "Room for your next hello.", message: "Your message history will live here, ready when you need it.")
                    .navigationTitle("History")
            }.tabItem { Label("History", systemImage: AppTab.history.symbol) }.tag(AppTab.history)
            NavigationStack {
                EmptyFeatureView(symbol: "heart", title: "Keep the ones you love.", message: "Your favorite messages, in one little collection.")
                    .navigationTitle("Saved")
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
