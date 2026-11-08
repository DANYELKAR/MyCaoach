import Foundation
import Security
import Supabase

struct KeychainStore: AuthLocalStorage {
    let namespace: String
    init(namespace: String = "com.danyelkar.mycoach.auth") { self.namespace = namespace }
    func store(key: String, value: Data) throws {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: namespace, kSecAttrAccount as String: key]
        let values: [String: Any] = [kSecValueData as String: value, kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly]
        let updated = SecItemUpdate(query as CFDictionary, values as CFDictionary)
        if updated == errSecItemNotFound {
            var add = query; values.forEach { add[$0.key] = $0.value }
            let status = SecItemAdd(add as CFDictionary, nil)
            guard status == errSecSuccess else { throw StoreError(status: status) }; return
        }
        guard updated == errSecSuccess else { throw StoreError(status: updated) }
    }
    func retrieve(key: String) throws -> Data? {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: namespace, kSecAttrAccount as String: key, kSecReturnData as String: true, kSecMatchLimit as String: kSecMatchLimitOne]
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        if status == errSecItemNotFound { return nil }
        guard status == errSecSuccess else { throw StoreError(status: status) }
        return result as? Data
    }
    func remove(key: String) throws {
        let query: [String: Any] = [kSecClass as String: kSecClassGenericPassword, kSecAttrService as String: namespace, kSecAttrAccount as String: key]
        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else { throw StoreError(status: status) }
    }
    struct StoreError: Error { let status: OSStatus }
}
