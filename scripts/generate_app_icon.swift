// Native vector artwork for the app icon. Run with: swift scripts/generate_app_icon.swift
import AppKit
import CoreGraphics
import Foundation

let size = 1024
let colorSpace = CGColorSpaceCreateDeviceRGB()
let context = CGContext(data: nil, width: size, height: size, bitsPerComponent: 8, bytesPerRow: size * 4,
                        space: colorSpace, bitmapInfo: CGImageAlphaInfo.noneSkipLast.rawValue)!
context.setFillColor(CGColor(red: 15/255, green: 13/255, blue: 20/255, alpha: 1))
context.fill(CGRect(x: 0, y: 0, width: size, height: size))
let bubble = CGMutablePath()
bubble.addRoundedRect(in: CGRect(x: 170, y: 260, width: 684, height: 570), cornerWidth: 156, cornerHeight: 156)
bubble.move(to: CGPoint(x: 270, y: 340)); bubble.addLine(to: CGPoint(x: 250, y: 155))
bubble.addQuadCurve(to: CGPoint(x: 490, y: 285), control: CGPoint(x: 350, y: 175)); bubble.closeSubpath()
context.saveGState(); context.addPath(bubble); context.clip()
let gradient = CGGradient(colorsSpace: colorSpace, colors: [CGColor(red: 1,green: 77/255,blue: 125/255,alpha: 1), CGColor(red: 139/255,green: 92/255,blue: 246/255,alpha: 1)] as CFArray, locations: [0,1])!
context.drawLinearGradient(gradient, start: CGPoint(x: 200,y: 780), end: CGPoint(x: 850,y: 200), options: [.drawsBeforeStartLocation,.drawsAfterEndLocation]); context.restoreGState()
func sparkle(x: CGFloat, y: CGFloat, radius: CGFloat) {
    let path = CGMutablePath()
    path.move(to: CGPoint(x: x,y: y+radius))
    for (cx,cy,ex,ey): (CGFloat,CGFloat,CGFloat,CGFloat) in [(x+radius*0.12,y+radius*0.12,x+radius,y),(x+radius*0.12,y-radius*0.12,x,y-radius),(x-radius*0.12,y-radius*0.12,x-radius,y),(x-radius*0.12,y+radius*0.12,x,y+radius)] {
        path.addQuadCurve(to: CGPoint(x: ex,y: ey), control: CGPoint(x: cx,y: cy))
    }
    path.closeSubpath(); context.addPath(path); context.setFillColor(CGColor(red: 1,green: 1,blue: 1,alpha: 1)); context.fillPath()
}
sparkle(x: 490,y: 540,radius: 150); sparkle(x: 680,y: 690,radius: 58); sparkle(x: 688,y: 405,radius: 38)
let output = URL(fileURLWithPath: "MyCoach/MyCoach/Assets.xcassets/AppIcon.appiconset/AppIcon.png")
let representation = NSBitmapImageRep(cgImage: context.makeImage()!)
try representation.representation(using: .png, properties: [:])!.write(to: output)
print("Wrote opaque 1024×1024 MyCoach icon")
