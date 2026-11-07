import SwiftUI

enum Theme {
    static let background = Color(hex: 0x0F0D14)
    static let surface = Color(hex: 0x1A1722)
    static let elevated = Color(hex: 0x252233)
    static let primary = Color(hex: 0xFF4D7D)
    static let secondary = Color(hex: 0x8B5CF6)
    static let label = Color(hex: 0xF5F3FA)
    static let muted = Color(hex: 0xA09BB0)
    static let separator = Color(hex: 0x3A3650)
    static let success = Color(hex: 0x34D399)
    static let warning = Color(hex: 0xFBBF24)
    static let destructive = Color(hex: 0xF87171)
    static let gradient = LinearGradient(colors: [primary, secondary], startPoint: .topLeading, endPoint: .bottomTrailing)
    // Darker action stops keep light button text above 4.5:1 contrast.
    static let actionStops: [UInt] = [0xC72658, 0x6841C5]
    static let actionGradient = LinearGradient(colors: actionStops.map { Color(hex: $0) }, startPoint: .leading, endPoint: .trailing)
    static let cardRadius: CGFloat = 20
    static let spacing: CGFloat = 8

    static func contrast(_ first: UInt, _ second: UInt) -> Double {
        func luminance(_ rgb: UInt) -> Double {
            let channels = [16, 8, 0].map { shift -> Double in
                let value = Double((rgb >> shift) & 0xFF) / 255
                return value <= 0.04045 ? value / 12.92 : pow((value + 0.055) / 1.055, 2.4)
            }
            return 0.2126 * channels[0] + 0.7152 * channels[1] + 0.0722 * channels[2]
        }
        let a = luminance(first), b = luminance(second)
        return (max(a, b) + 0.05) / (min(a, b) + 0.05)
    }
}

extension Color {
    init(hex: UInt) {
        self.init(.sRGB, red: Double((hex >> 16) & 0xFF) / 255,
                  green: Double((hex >> 8) & 0xFF) / 255, blue: Double(hex & 0xFF) / 255, opacity: 1)
    }
}

struct SurfaceCard: ViewModifier {
    func body(content: Content) -> some View {
        content.padding(20).background(Theme.surface, in: RoundedRectangle(cornerRadius: Theme.cardRadius))
            .overlay(RoundedRectangle(cornerRadius: Theme.cardRadius).stroke(Theme.separator.opacity(0.55), lineWidth: 1))
    }
}
extension View {
    func surfaceCard() -> some View { modifier(SurfaceCard()) }
}
