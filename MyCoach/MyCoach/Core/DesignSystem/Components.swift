import SwiftUI

struct BrandMark: View {
    var size: CGFloat = 48
    var body: some View {
        Image(systemName: "sparkles").font(.system(size: size * 0.47, weight: .semibold))
            .foregroundStyle(Theme.gradient).frame(width: size, height: size)
            .background(Theme.primary.opacity(0.09), in: RoundedRectangle(cornerRadius: size * 0.3))
            .accessibilityHidden(true)
    }
}

struct PrimaryButton: View {
    let title: LocalizedStringKey
    var symbol: String? = nil
    var isLoading = false
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            HStack(spacing: 8) {
                if isLoading { ProgressView().tint(Theme.actionLabel) }
                else if let symbol { Image(systemName: symbol) }
                Text(title).font(.headline)
            }.frame(maxWidth: .infinity).padding(.horizontal, 20).padding(.vertical, 14).frame(minHeight: 52)
        }
        .buttonStyle(.plain).foregroundStyle(Theme.actionLabel).background(Theme.actionGradient, in: Capsule())
        .disabled(isLoading).accessibilityLabel(Text(title))
    }
}

struct SecondaryButton: View {
    let title: LocalizedStringKey
    let action: () -> Void
    var body: some View {
        Button(action: action) { Text(title).font(.headline).frame(maxWidth: .infinity).padding(.horizontal, 20).padding(.vertical, 14).frame(minHeight: 52) }
            .buttonStyle(.plain).foregroundStyle(Theme.label)
            .overlay(Capsule().stroke(Theme.separator, lineWidth: 1.5))
    }
}

struct ToneChip: View {
    let tone: BuiltInTone
    let selected: Bool
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Label(tone.title, systemImage: tone.symbol).font(.subheadline.weight(.semibold))
                .padding(.horizontal, 16).padding(.vertical, 12).frame(minHeight: 44)
                .foregroundStyle(selected ? Theme.actionLabel : Theme.label)
                .background { if selected { Capsule().fill(Theme.actionGradient) } else { Capsule().fill(Theme.elevated) } }
        }.buttonStyle(.plain).accessibilityLabel(tone.title)
            .accessibilityAddTraits(selected ? .isSelected : [])
            .accessibilityIdentifier("tone.\(tone.rawValue)")
    }
}

struct PlusBadge: View {
    var body: some View {
        Text("PLUS").font(.caption2.weight(.heavy)).tracking(1.2)
            .padding(.horizontal, 9).padding(.vertical, 5)
            .foregroundStyle(Theme.plusLabel).background(Theme.secondary.opacity(0.2), in: Capsule())
            .accessibilityLabel("MyCoach Plus")
    }
}

struct ToastBanner: View {
    let message: LocalizedStringKey
    var body: some View {
        Label(message, systemImage: "checkmark.circle.fill").font(.subheadline.weight(.medium))
            .foregroundStyle(Theme.label).padding(16).background(Theme.elevated, in: Capsule())
            .accessibilityAddTraits(.updatesFrequently)
    }
}

struct EmptyStateView: View {
    let symbol: String
    let title: LocalizedStringKey
    let message: LocalizedStringKey
    var body: some View {
        VStack(spacing: 20) {
            Image(systemName: symbol).font(.system(size: 36, weight: .light))
                .foregroundStyle(Theme.gradient).frame(width: 88, height: 88)
                .background(Theme.primary.opacity(0.08), in: RoundedRectangle(cornerRadius: 28)).accessibilityHidden(true)
            VStack(spacing: 10) {
                Text(title).font(.title2.weight(.bold)).foregroundStyle(Theme.label)
                Text(message).font(.body).foregroundStyle(Theme.muted).multilineTextAlignment(.center)
                    .fixedSize(horizontal: false, vertical: true)
            }
        }.frame(maxWidth: .infinity).padding(28).accessibilityElement(children: .combine)
    }
}

struct ErrorStateView: View {
    let title: LocalizedStringKey
    let message: LocalizedStringKey
    let retry: () -> Void
    var body: some View {
        VStack(spacing: 24) {
            EmptyStateView(symbol: "wifi.exclamationmark", title: title, message: message)
            SecondaryButton(title: "Try again", action: retry)
        }.padding(20)
    }
}
