import Foundation
import ImageIO
import UniformTypeIdentifiers

struct ImageProcessor {
    /// ImageIO thumbnail decoding prevents full-resolution raster allocation and runs off the UI actor.
    static func jpeg(_ data: Data) async throws -> Data {
        try await Task.detached(priority: .userInitiated) {
            guard data.count <= 30_000_000, let source = CGImageSourceCreateWithData(data as CFData, nil),
                  let image = CGImageSourceCreateThumbnailAtIndex(source, 0, [kCGImageSourceCreateThumbnailFromImageAlways: true, kCGImageSourceThumbnailMaxPixelSize: 1024, kCGImageSourceCreateThumbnailWithTransform: true] as CFDictionary) else { throw ImageError.invalid }
            let output = NSMutableData()
            guard let destination = CGImageDestinationCreateWithData(output, UTType.jpeg.identifier as CFString, 1, nil) else { throw ImageError.invalid }
            CGImageDestinationAddImage(destination, image, [kCGImageDestinationLossyCompressionQuality: 0.8] as CFDictionary)
            guard CGImageDestinationFinalize(destination) else { throw ImageError.invalid }
            return output as Data
        }.value
    }
    enum ImageError: LocalizedError { case invalid; var errorDescription: String? { "Choose a valid image smaller than 30 MB." } }
}
