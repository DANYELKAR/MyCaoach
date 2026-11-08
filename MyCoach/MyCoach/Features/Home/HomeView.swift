import SwiftUI

struct HomeView: View {
    @Environment(AppState.self) private var state
    @State private var showToneGuide = false
    @State private var showOpener = false

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 28) {
                HStack(spacing: 12) {
                    BrandMark()
                    VStack(alignment: .leading, spacing: 2) {
                        Text("MyCoach").font(.title2.weight(.bold)).foregroundStyle(Theme.label)
                        Text("A little more you.").font(.caption).foregroundStyle(Theme.muted)
                    }
                    Spacer()
                    Button { state.selectedTab = .profile } label: {
                        Image(systemName: "person.crop.circle").font(.title2).foregroundStyle(Theme.label)
                            .frame(width: 48, height: 48).background(Theme.elevated, in: Circle())
                    }.accessibilityLabel("Open profile")
                }
                VStack(alignment: .leading, spacing: 18) {
                    Label("START WITH SOMETHING REAL", systemImage: "sparkle")
                        .font(.caption.weight(.bold)).tracking(1.3).foregroundStyle(Color(hex: 0xFFAAC2))
                    Text("Better hellos.\nMore you.")
                        .font(.system(.largeTitle, design: .rounded).weight(.bold)).foregroundStyle(Theme.label)
                        .fixedSize(horizontal: false, vertical: true)
                    Text("A little context. A tone that feels like you. A first message worth sending.")
                        .font(.body).foregroundStyle(Theme.muted).fixedSize(horizontal: false, vertical: true)
                    HStack(spacing: 8) {
                        Image(systemName: "lock.shield").foregroundStyle(Theme.success)
                        Text("Your words. Your choice.").font(.footnote)
                    }.foregroundStyle(Theme.muted).padding(.top, 8)
                }.frame(maxWidth: .infinity, alignment: .leading).surfaceCard()
                    .overlay(alignment: .topTrailing) {
                        Circle().fill(Theme.secondary.opacity(0.08)).frame(width: 100, height: 100).blur(radius: 24).allowsHitTesting(false).accessibilityHidden(true)
                    }
                if let runtime = state.runtime {
                    PlusFeatureLink(title: "Continue Chat", symbol: "bubble.left.and.bubble.right") { GenerationView(reply: true) }.accessibilityIdentifier("home.reply")
                    if let usage = runtime.usage { Text("\(usage.remainingToday) of \(usage.dailyLimit) credits left today").font(.subheadline.bold()).foregroundStyle(Theme.label) }
                    Button { showOpener = true } label: { Label("Create an opener", systemImage: "sparkles").font(.headline).frame(maxWidth: .infinity, minHeight: 52).foregroundStyle(Theme.label).background(Theme.actionGradient, in: Capsule()) }.buttonStyle(.plain).accessibilityIdentifier("home.opener").accessibilityValue(showOpener ? "Opening" : "Ready")
                }
                VStack(alignment: .leading, spacing: 16) {
                    HStack {
                        Text("Find your tone").font(.title3.weight(.bold))
                        Spacer()
                        Button { showToneGuide = true } label: {
                            Image(systemName: "info.circle").frame(width: 44, height: 44)
                        }.foregroundStyle(Theme.muted).accessibilityLabel("About message tones")
                    }.foregroundStyle(Theme.label)
                    ScrollViewReader { proxy in
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                ForEach(BuiltInTone.allCases) { tone in
                                    ToneChip(tone: tone, selected: state.defaultTone == tone) { state.defaultTone = tone }
                                        .id(tone.id)
                                }
                            }.padding(.vertical, 2)
                        }
                        .onAppear { proxy.scrollTo(state.defaultTone.id, anchor: .center) }
                        .onChange(of: state.defaultTone) { _, tone in
                            withAnimation { proxy.scrollTo(tone.id, anchor: .center) }
                        }
                    }
                    Text(state.defaultTone.detail).font(.subheadline).foregroundStyle(Theme.muted)
                        .fixedSize(horizontal: false, vertical: true).accessibilityIdentifier("selected-tone-description")
                }
                VStack(alignment: .leading, spacing: 16) {
                    Text("Keep a little context").font(.title3.weight(.bold)).foregroundStyle(Theme.label)
                    Button { state.selectedTab = .people } label: {
                        HStack(spacing: 16) {
                            Image(systemName: "person.2").font(.title3).foregroundStyle(Color(hex: 0xC5AAFF)).frame(width: 44, height: 44)
                                .background(Theme.secondary.opacity(0.12), in: RoundedRectangle(cornerRadius: 14))
                            VStack(alignment: .leading, spacing: 4) {
                                Text("People, not just profiles").font(.headline).foregroundStyle(Theme.label)
                                Text("Make room for the details that matter.").font(.subheadline).foregroundStyle(Theme.muted)
                            }
                            Spacer(minLength: 0)
                            Image(systemName: "arrow.up.right").foregroundStyle(Theme.muted)
                        }.frame(maxWidth: .infinity, alignment: .leading).surfaceCard()
                    }.buttonStyle(.plain).accessibilityIdentifier("home.people")
                }
            }.padding(20).padding(.bottom, 16)
        }.background(Theme.background).toolbar(.hidden, for: .navigationBar)
            .navigationDestination(isPresented: $showOpener) { GenerationView() }
            .sheet(isPresented: $showToneGuide) { ToneGuideView() }
    }
}

struct ToneGuideView: View {
    @Environment(\.dismiss) private var dismiss
    var body: some View {
        NavigationStack {
            ScrollView {
                VStack(alignment: .leading, spacing: 20) {
                    Text("Sound like yourself.").font(.title2.weight(.bold)).foregroundStyle(Theme.label)
                    Text("Your tone is a starting point. Keep things respectful, specific, and easy to reply to.")
                        .foregroundStyle(Theme.muted)
                    ForEach(BuiltInTone.allCases) { tone in
                        HStack(alignment: .top, spacing: 14) {
                            Image(systemName: tone.symbol).foregroundStyle(Theme.primary).frame(width: 26, height: 26)
                            VStack(alignment: .leading, spacing: 4) {
                                Text(tone.title).font(.headline).foregroundStyle(Theme.label)
                                Text(tone.detail).font(.subheadline).foregroundStyle(Theme.muted)
                            }
                        }
                    }
                }.padding(24)
            }.background(Theme.background).navigationTitle("Your tone").navigationBarTitleDisplayMode(.inline)
                .toolbar { ToolbarItem(placement: .confirmationAction) { Button("Done") { dismiss() } } }
        }.presentationDragIndicator(.visible)
    }
}
