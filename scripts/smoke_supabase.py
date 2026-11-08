#!/usr/bin/env python3
"""Opt-in smoke for a loopback Spring Boot API with REAL Supabase development services.

Requires AI_ENABLED=false and BILLING_ENABLED=false. Creates disposable, confirmed
synthetic Auth users without sending email. Exercises JWT/CRUD/isolation/deletion,
then cleans up only its own users. Never prints credentials, tokens or user content.
"""
import argparse
import datetime
import json
import secrets
import struct
import urllib.error
import urllib.parse
import urllib.request
import uuid
import zlib
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--allow-test-accounts", action="store_true", required=True)
    parser.add_argument("--api", default="http://127.0.0.1:8081")
    args = parser.parse_args()
    url = urllib.parse.urlsplit(args.api)
    if url.scheme != "http" or url.hostname not in ("127.0.0.1", "localhost") or url.username or url.query:
        raise SystemExit("Use a loopback development API only.")
    env = {}
    for line in (ROOT / ".env").read_text().splitlines():
        if "=" not in line or line.lstrip().startswith("#"):
            continue
        key, value = line.split("=", 1)
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        env[key.strip()] = value
    if env.get("AI_ENABLED") != "false" or env.get("BILLING_ENABLED") != "false":
        raise SystemExit("This smoke requires explicitly disabled AI/billing; no live inference or purchases.")
    cloud = env["SUPABASE_URL"]
    if not cloud.startswith("https://"):
        raise SystemExit("Supabase must use HTTPS.")
    server = env["SUPABASE_SERVICE_KEY"]
    if not server or server.startswith("sb_publishable_"):
        raise SystemExit("Missing backend secret.")
    config = (ROOT / "MyCoach/Config/Local.xcconfig").read_text()
    public = next(line.split("=", 1)[1].strip() for line in config.splitlines()
                  if line.startswith("MYCOACH_SUPABASE_PUBLISHABLE_KEY"))
    checks = []
    users = []

    def call(base, method, path, body=None, headers=None, expected=200, mime="application/json"):
        data = body if isinstance(body, bytes) else json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(base + path, data=data, method=method,
                                     headers={"Content-Type": mime, **(headers or {})})
        try:
            response = urllib.request.urlopen(req, timeout=60)
        except urllib.error.HTTPError as e:
            response = e
        raw = response.read()
        if response.status != expected:
            # No response body, request body, header or dynamic path in failure output.
            raise AssertionError(f"{method} request expected HTTP {expected}, received {response.status}")
        result = json.loads(raw) if raw else None
        if base == args.api and expected >= 400:
            assert result["requestId"] == response.headers["X-Request-ID"]
        return result

    admin = {"apikey": server}
    if not server.startswith("sb_secret_"):
        admin["Authorization"] = "Bearer " + server

    def api(method, path, body=None, token=None, expected=200, mime="application/json"):
        return call(args.api, method, path, body, {"Authorization": "Bearer " + token} if token else {}, expected, mime)

    def passed(name):
        checks.append(name)
        print(name + "=passed", flush=True)

    try:
        assert api("GET", "/actuator/health/readiness")["status"] == "UP"
        api("GET", "/v1/me", expected=401)
        api("GET", "/v1/me", token="invalid-jwt", expected=401)
        passed("health_and_missing_invalid_jwt")
        for _ in range(2):
            credentials = {"email": "mycoach-smoke-" + uuid.uuid4().hex + "@example.invalid", "password": secrets.token_urlsafe(40)}
            user = call(cloud, "POST", "/auth/v1/admin/users", {**credentials, "email_confirm": True}, admin)
            record = {"id": user["id"], "deleted": False}
            users.append(record)
            session = call(cloud, "POST", "/auth/v1/token?grant_type=password", credentials, {"apikey": public})
            record.update(token=session["access_token"], refresh=session["refresh_token"])
            assert api("GET", "/v1/me", token=record["token"])["id"] == record["id"]
        token, other = users[0]["token"], users[1]["token"]
        passed("real_supabase_jwt_profile_creation")
        profile = api("PATCH", "/v1/me", {"ageConfirmed": True, "aiConsent": True, "storeImages": True, "timezone": "Europe/Istanbul"}, token)
        assert profile["plan"] == "FREE"
        tones = api("GET", "/v1/tones", token=token)
        assert len(tones) == 7
        assert api("GET", "/v1/me/usage", token=token)["remainingToday"] == 3
        contact = api("POST", "/v1/contacts", {"name": "Synthetic verification", "source": "OTHER"}, token)
        for method, body in [("GET", None), ("PATCH", {"name": "Forbidden"}), ("DELETE", None)]:
            api(method, "/v1/contacts/" + contact["id"], body, other, 404)
        assert api("GET", "/v1/contacts", token=other) == []
        passed("cross_user_read_update_delete_isolation")
        for _ in range(2):
            api("POST", "/v1/contacts", {"name": "Synthetic capacity"}, token)
        api("POST", "/v1/contacts", {"name": "Beyond Free limit"}, token, 403)
        for _ in range(3):
            api("POST", "/v1/folders", {"name": "Synthetic folder"}, token)
        api("POST", "/v1/folders", {"name": "Beyond Free limit"}, token, 403)
        passed("free_contact_and_folder_limits")
        assert api("POST", "/v1/billing/sync", token=token, expected=503)["code"] == "BILLING_UNAVAILABLE"
        api("POST", "/v1/billing/webhook", {"event": {"type": "TEST"}}, expected=401)
        assert api("GET", "/v1/me", token=token)["plan"] == "FREE"
        passed("disabled_billing_never_grants_plus")
        boundary = "mycoach-" + uuid.uuid4().hex
        fields = {"toneId": tones[0]["id"], "contactId": contact["id"], "idempotencyKey": "cloud-disabled-ai"}
        body = b"".join(f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode() for k, v in fields.items())
        def chunk(kind, data):
            return struct.pack("!I", len(data)) + kind + data + struct.pack("!I", zlib.crc32(kind + data) & 0xFFFFFFFF)
        png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack("!2I5B", 1, 1, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(b"\x00\xff\x00\x00")) + chunk(b"IEND", b"")
        body += f'--{boundary}\r\nContent-Disposition: form-data; name="image"; filename="synthetic.png"\r\nContent-Type: image/png\r\n\r\n'.encode() + png + f"\r\n--{boundary}--\r\n".encode()
        assert api("POST", "/v1/generations/opener", body, token, 503, "multipart/form-data; boundary=" + boundary)["code"] == "AI_UNAVAILABLE"
        assert api("GET", "/v1/me/usage", token=token)["remainingToday"] == 3
        assert call(cloud, "POST", "/storage/v1/object/list/inputs", {"prefix": users[0]["id"], "limit": 100}, admin) == []
        passed("disabled_ai_refunds_credit_and_cleans_storage")
        refreshed = call(cloud, "POST", "/auth/v1/token?grant_type=refresh_token", {"refresh_token": users[0]["refresh"]}, {"apikey": public})
        users[0].update(token=refreshed["access_token"], refresh=refreshed["refresh_token"])
        assert api("GET", "/v1/me", token=users[0]["token"])["id"] == users[0]["id"]
        passed("real_supabase_session_refresh")
        for user in users:
            api("DELETE", "/v1/me", token=user["token"])
            user["deleted"] = True
            api("GET", "/v1/me", token=user["token"], expected=401)
            call(cloud, "GET", "/auth/v1/admin/users/" + user["id"], headers=admin, expected=404)
            # Supabase returns 400 for a deleted refresh session.
            call(cloud, "POST", "/auth/v1/token?grant_type=refresh_token", {"refresh_token": user["refresh"]}, {"apikey": public}, expected=400)
        passed("account_deletion_auth_removal_old_jwt_and_refresh_rejected")
    finally:
        # Failure cleanup is scoped to IDs created by this invocation only.
        for user in users:
            if not user["deleted"]:
                call(cloud, "DELETE", "/auth/v1/admin/users/" + user["id"], headers=admin)
    evidence = {"verifiedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(),
                "projectRef": urllib.parse.urlsplit(cloud).hostname.split(".")[0],
                "checks": checks, "syntheticAccountsCreatedAndDeleted": len(users),
                "aiInferencePerformed": False, "purchasesPerformed": False,
                "smtpEmailSent": False, "note": "Deletion tombstones intentionally remain; no tokens, passwords, user IDs or user content included."}
    (ROOT / "docs/infrastructure/supabase-runtime-verification.json").write_text(json.dumps(evidence, indent=2) + "\n")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        # Avoid arbitrary SDK/network exception text that might include credentials.
        raise SystemExit("Cloud smoke failed: " + (str(e) if isinstance(e, AssertionError) and str(e).startswith(("GET request", "POST request", "PATCH request", "DELETE request")) else type(e).__name__)) from None
