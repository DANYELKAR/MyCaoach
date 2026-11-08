import SwiftUI
import UIKit

enum Theme {
    static let background = adaptive(dark: 0x0F0D14, light: 0xF6F3FA)
    static let surface = adaptive(dark: 0x1A1722, light: 0xFFFFFF)
    static let elevated = adaptive(dark: 0x252233, light: 0xEEE8F5)
    static let primary = adaptive(dark: 0xFF4D7D, light: 0xB91F50)
    static let secondary = adaptive(dark: 0x8B5CF6, light: 0x6841C5)
    static let label = adaptive(dark: 0xF5F3FA, light: 0x201A2D)
    static let muted = adaptive(dark: 0xA09BB0, light: 0x665D74)
    static let separator = adaptive(dark: 0x3A3650, light: 0xCDC5D8)
    static let success = adaptive(dark: 0x34D399, light: 0x08734C)
    static let warning = adaptive(dark: 0xFBBF24, light: 0x7A4D00)
    static let destructive = adaptive(dark: 0xF87171, light: 0xB32938)
    static let gradient = LinearGradient(colors: [primary, secondary], startPoint: .topLeading, endPoint: .bottomTrailing)
    // Darker action stops keep light button text above 4.5:1 contrast.
    static let actionStops: [UInt] = [0xC72658, 0x6841C5]
    static let actionGradient = LinearGradient(colors: actionStops.map { Color(hex: $0) }, startPoint: .leading, endPoint: .trailing)
    static let actionLabel = Color(hex: 0xF5F3FA)
    static let plusLabel = adaptive(dark: 0xDCCBFF, light: 0x4C267D)
    static let cardRadius: CGFloat = 20
    static let spacing: CGFloat = 8
    private static func adaptive(dark: UInt, light: UInt) -> Color {
        Color(uiColor: UIColor { traits in
            let hex = traits.userInterfaceStyle == .light ? light : dark
            return UIColor(red: CGFloat((hex >> 16) & 255) / 255, green: CGFloat((hex >> 8) & 255) / 255, blue: CGFloat(hex & 255) / 255, alpha: 1)
        })
    }

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
