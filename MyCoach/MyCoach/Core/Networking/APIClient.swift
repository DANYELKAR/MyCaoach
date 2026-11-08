import Foundation

struct APIError: Error, LocalizedError, Decodable, Sendable {
    let code: String
    let message: String
    let requestId: String?
    var errorDescription: String? {
        switch code {
        case "UNAUTHORIZED": "Your session expired. Please sign in again."
        case "FORBIDDEN_PLAN": "This feature requires MyCoach Plus."
        case "OUT_OF_CREDITS": "You have used today's credits. They reset at your local midnight."
        case "CONTENT_BLOCKED": "We couldn't help with this content. Try another photo or message."
        case "RATE_LIMITED": "A little too fast. Wait a moment and try again."
        case "AI_UNAVAILABLE": "Suggestions are temporarily unavailable. Please try again."
        default: message
        }
    }
}
struct MultipartFile: Sendable { let field: String; let data: Data }

@MainActor
final class APIClient {
    deinit {}
    let baseURL: URL
    let session: URLSession
    var token: (() async throws -> String?)?
    var refresh: (() async throws -> Void)?
    var unauthorized: (() -> Void)?
    init(baseURL: URL, session: URLSession = URLSession(configuration: .ephemeral)) { self.baseURL = baseURL; self.session = session }

    func request<T: Decodable>(_ path: String, method: String = "GET", body: [String: JSONValue]? = nil, authenticated: Bool = true) async throws -> T {
        let parts = path.split(separator: "?", maxSplits: 1)
        var components = URLComponents(url: baseURL.appendingPathComponent(String(parts[0])), resolvingAgainstBaseURL: false)!
        if parts.count > 1 { components.percentEncodedQuery = String(parts[1]) }
        guard let url = components.url else { throw URLError(.badURL) }
        var r = URLRequest(url: url)
        r.httpMethod = method
        if let body { r.httpBody = try JSONEncoder().encode(body); r.setValue("application/json", forHTTPHeaderField: "Content-Type") }
        return try await send(r, authenticated: authenticated)
    }
    func send<T: Decodable>(_ original: URLRequest, authenticated: Bool = true, retry: Bool = true) async throws -> T {
        var r = original
        r.timeoutInterval = 65
        r.cachePolicy = .reloadIgnoringLocalCacheData
        r.setValue("application/json", forHTTPHeaderField: "Accept")
        if authenticated, let accessToken = try await token?() { r.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization") }
        let (data, response) = try await session.data(for: r)
        try Task.checkCancellation()
        guard let http = response as? HTTPURLResponse else { throw URLError(.badServerResponse) }
        if http.statusCode == 401, authenticated {
            if retry, let refresh {
                do { try await refresh(); return try await send(original, authenticated: true, retry: false) }
                catch { unauthorized?(); throw APIError(code: "UNAUTHORIZED", message: "Sign in again.", requestId: nil) }
            }
            unauthorized?()
        }
        guard (200..<300).contains(http.statusCode) else {
            throw (try? JSONDecoder().decode(APIError.self, from: data)) ?? APIError(code: "HTTP_\(http.statusCode)", message: "The request couldn't be completed. Please try again.", requestId: nil)
        }
        if data.isEmpty, let empty = EmptyResponse() as? T { return empty }
        return try JSONDecoder().decode(T.self, from: data)
    }
    func delete(_ path: String) async throws { let _: EmptyResponse = try await request(path, method: "DELETE") }
    func generate(reply: Bool, toneID: UUID, note: String, text: String, contactID: UUID?, key: String, images: [Data]) async throws -> GenerationResult {
        let boundary = "MyCoach-" + UUID().uuidString
        var fields = ["toneId": toneID.uuidString, "idempotencyKey": key, "note": note]
        if let contactID { fields["contactId"] = contactID.uuidString }
        if reply && !text.isEmpty { fields["text"] = text }
        let body = Self.multipart(boundary: boundary, fields: fields, files: images.map { MultipartFile(field: reply ? "images" : "image", data: $0) })
        var r = URLRequest(url: baseURL.appendingPathComponent("v1/generations/" + (reply ? "reply" : "opener")))
        r.httpMethod = "POST"; r.httpBody = body
        r.setValue("multipart/form-data; boundary=\(boundary)", forHTTPHeaderField: "Content-Type")
        return try await send(r)
    }
    static func multipart(boundary: String, fields: [String: String], files: [MultipartFile]) -> Data {
        var data = Data()
        func append(_ s: String) { data.append(Data(s.utf8)) }
        for key in fields.keys.sorted() {
            append("--\(boundary)\r\nContent-Disposition: form-data; name=\"\(key)\"\r\n\r\n\(fields[key]!)\r\n")
        }
        for (i, file) in files.enumerated() {
            append("--\(boundary)\r\nContent-Disposition: form-data; name=\"\(file.field)\"; filename=\"image\(i).jpg\"\r\nContent-Type: image/jpeg\r\n\r\n")
            data.append(file.data); append("\r\n")
        }
        append("--\(boundary)--\r\n"); return data
    }
}
