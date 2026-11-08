import XCTest
import UIKit
@testable import MyCoach

final class MockURLProtocol: URLProtocol, @unchecked Sendable {
    nonisolated(unsafe) static var handler: (@Sendable (URLRequest) throws -> (Int, Data))?
    override class func canInit(with request: URLRequest) -> Bool { true }
    override class func canonicalRequest(for request: URLRequest) -> URLRequest { request }
    override func startLoading() {
        do {
            guard let handler = Self.handler else { throw URLError(.badServerResponse) }
            let (status, data) = try handler(request)
            client?.urlProtocol(self, didReceive: HTTPURLResponse(url: request.url!, statusCode: status, httpVersion: nil, headerFields: nil)!, cacheStoragePolicy: .notAllowed)
            client?.urlProtocol(self, didLoad: data); client?.urlProtocolDidFinishLoading(self)
        } catch { client?.urlProtocol(self, didFailWithError: error) }
    }
    override func stopLoading() {}
}

@MainActor
final class NetworkingTests: XCTestCase {
    private func client() -> APIClient {
        let config = URLSessionConfiguration.ephemeral; config.protocolClasses = [MockURLProtocol.self]
        return APIClient(baseURL: URL(string: "https://api.example.test")!, session: URLSession(configuration: config))
    }
    override func tearDown() { MockURLProtocol.handler = nil }
    func testBearerAndJSONBodyRoundTrip() async throws {
        MockURLProtocol.handler = { request in
            XCTAssertEqual(request.value(forHTTPHeaderField: "Authorization"), "Bearer access")
            XCTAssertEqual(request.url?.path, "/v1/me")
            XCTAssertEqual(request.httpMethod, "PATCH")
            return (200, Data("{\"remainingToday\":2,\"dailyLimit\":3}".utf8))
        }
        let api = client(); api.token = { "access" }
        let usage: UsageDTO = try await api.request("v1/me", method: "PATCH", body: ["aiConsent": .b(true)])
        XCTAssertEqual(usage.remainingToday, 2)
    }
    func testUnauthorizedRefreshesOnceAndRetriesThenSignsOut() async throws {
        let api = client(); var refreshCount = 0; var signedOut = false
        api.token = { "expired" }; api.refresh = { refreshCount += 1 }; api.unauthorized = { signedOut = true }
        MockURLProtocol.handler = { _ in (401, Data("{\"code\":\"UNAUTHORIZED\",\"message\":\"Expired\"}".utf8)) }
        do { let _: UsageDTO = try await api.request("v1/me/usage"); XCTFail("Expected auth failure") } catch { XCTAssertEqual((error as? APIError)?.code, "UNAUTHORIZED") }
        XCTAssertEqual(refreshCount, 1); XCTAssertTrue(signedOut)
    }
    func testStructuredQuotaFailureAndEmptyDelete() async throws {
        let api = client()
        MockURLProtocol.handler = { _ in (402, Data("{\"code\":\"OUT_OF_CREDITS\",\"message\":\"quota\",\"requestId\":\"trace\"}".utf8)) }
        do { let _: UsageDTO = try await api.request("v1/me/usage"); XCTFail() } catch { XCTAssertEqual((error as? APIError)?.requestId, "trace"); XCTAssertTrue(error.localizedDescription.contains("midnight")) }
        MockURLProtocol.handler = { _ in (204, Data()) }
        try await api.delete("v1/contacts/one")
    }
    func testMultipartHasRepeatedImagesAndStableKey() {
        let body = APIClient.multipart(boundary: "unit", fields: ["idempotencyKey": "same-key", "note": "hello"], files: [.init(field: "images", data: Data([1,2])), .init(field: "images", data: Data([3,4]))])
        let text = String(decoding: body, as: UTF8.self)
        XCTAssertEqual(text.components(separatedBy: "name=\"images\"").count - 1, 2)
        XCTAssertTrue(text.contains("same-key\r\n")); XCTAssertTrue(text.hasSuffix("--unit--\r\n"))
    }
    func testImageResizeAndCorruptInput() async throws {
        let data = UIGraphicsImageRenderer(size: CGSize(width: 2048, height: 1500)).jpegData(withCompressionQuality: 1) { c in UIColor.red.setFill(); c.fill(CGRect(x: 0,y: 0,width: 2048,height: 1500)) }
        let resized = try await ImageProcessor.jpeg(data)
        let image = try XCTUnwrap(UIImage(data: resized))
        XCTAssertEqual(max(image.size.width, image.size.height), 1024)
        do { _ = try await ImageProcessor.jpeg(Data([1,2,3])); XCTFail() } catch { XCTAssertTrue(error is ImageProcessor.ImageError) }
    }
    func testGenerationValidationAndRetryIdentity() {
        let model = GenerationViewModel(); model.toneID = UUID(); XCTAssertFalse(model.canGenerate)
        model.images = [Data([1])]; XCTAssertTrue(model.canGenerate)
        let key = model.idempotencyKey; model.cancel(); XCTAssertEqual(model.idempotencyKey, key)
        model.note = "different"; XCTAssertNotEqual(model.idempotencyKey, key)
        let reply = GenerationViewModel(reply: true); reply.toneID = UUID(); reply.text = "   "; XCTAssertFalse(reply.canGenerate)
        reply.text = "Hey, how was your weekend?"; XCTAssertTrue(reply.canGenerate)
        reply.images = Array(repeating: Data([1]), count: 4); XCTAssertFalse(reply.canGenerate)
    }
    func testKeychainStoresUpdatesAndRemoves() throws {
        let store = KeychainStore(namespace: "MyCoach.unit.\(UUID())")
        defer { try? store.remove(key: "token") }
        try store.store(key: "token", value: Data("a".utf8)); try store.store(key: "token", value: Data("b".utf8))
        XCTAssertEqual(try store.retrieve(key: "token"), Data("b".utf8))
        try store.remove(key: "token"); XCTAssertNil(try store.retrieve(key: "token"))
    }
}
