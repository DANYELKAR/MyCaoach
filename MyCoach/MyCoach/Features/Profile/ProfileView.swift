import SwiftUI

struct ProfileView: View {
    @Environment(AppState.self) private var state
    @Environment(\.dynamicTypeSize) private var dynamicTypeSize
    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 24) {
                HStack(spacing: 16) {
                    BrandMark(size: 64)
                    VStack(alignment: .leading, spacing: 5) {
                        Text("Make it your own").font(.title3.weight(.bold)).foregroundStyle(Theme.label)
                        Text("A voice that feels like you.").font(.subheadline).foregroundStyle(Theme.muted)
                    }
                }.frame(maxWidth: .infinity, alignment: .leading).surfaceCard()
                if state.runtime != nil {
                    NavigationLink("Tone library") { ToneLibraryView() }.frame(minHeight: 44).accessibilityIdentifier("profile.tones")
                    PlusFeatureLink(title: "My Style", symbol: "text.quote") { StyleView() }.accessibilityIdentifier("profile.style")
                }
                if state.runtime != nil { NavigationLink("Subscription") { SubscriptionView() }.font(.headline).frame(minHeight: 44).surfaceCard() }
                VStack(alignment: .leading, spacing: 16) {
                    Text("YOUR DEFAULT TONE").font(.caption.weight(.semibold)).tracking(1.2).foregroundStyle(Theme.muted)
                    ForEach(BuiltInTone.allCases) { tone in
                        Button { state.defaultTone = tone } label: {
                            HStack(spacing: 14) {
                                if !dynamicTypeSize.isAccessibilitySize {
                                    Image(systemName: tone.symbol).font(.system(size: 22)).foregroundStyle(Theme.primary).frame(width: 28).accessibilityHidden(true)
                                }
                                Text(tone.title).foregroundStyle(Theme.label).fixedSize(horizontal: false, vertical: true)
                                Spacer()
                                if state.defaultTone == tone { Image(systemName: "checkmark.circle.fill").font(.system(size: 22)).foregroundStyle(Theme.success).accessibilityHidden(true) }
                            }.font(.body).frame(minHeight: 44)
                        }.buttonStyle(.plain).accessibilityLabel(tone.title).accessibilityAddTraits(state.defaultTone == tone ? .isSelected : [])
                            .accessibilityIdentifier("profile.tone.\(tone.rawValue)")
                    }
                }.surfaceCard()
                Label {
                    Text("MyCoach is for adults 18+. You stay in control of what you share and send.")
                        .font(.footnote).foregroundStyle(Theme.muted)
                } icon: { Image(systemName: "lock.shield").foregroundStyle(Theme.muted) }
                Text("MyCoach · Version 1.0").font(.caption).foregroundStyle(Theme.muted).frame(maxWidth: .infinity)
            }.padding(20)
        }.background(Theme.background).navigationTitle("Profile")
    }
}
