import SwiftUI

struct PlusFeatureLink<Destination: View>: View {
    @Environment(AppState.self) private var state
    let title: String
    let symbol: String
    @ViewBuilder let destination: () -> Destination
    @State private var opening = false
    var body: some View {
        Button { state.runtime?.requirePlus { opening = true } } label: {
            HStack { Label(title, systemImage: symbol); Spacer(); PlusBadge() }.frame(minHeight: 44)
        }.navigationDestination(isPresented: $opening) { destination() }
    }
}
