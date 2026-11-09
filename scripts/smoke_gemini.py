#!/usr/bin/env python3
"""Opt-in REAL Supabase + Gemini opener smoke using one disposable synthetic account.

Verify the Gemini project's free billing tier in AI Studio before invoking.
Sends only a generated flat PNG and fictional interests. No personal photos/content.
Performs one inference; replay and conflicting idempotency requests must not infer.
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
    parser.add_argument("--allow-test-account", action="store_true", required=True)
    parser.add_argument("--free-tier-confirmed", action="store_true", required=True)
    parser.add_argument("--api", default="http://127.0.0.1:8081")
    args = parser.parse_args()
    target = urllib.parse.urlsplit(args.api)
    if target.scheme != "http" or target.hostname not in ("127.0.0.1", "localhost") or target.username or target.query:
        raise SystemExit("Loopback development API required.")
    env = {}
    for line in (ROOT / ".env").read_text().splitlines():
        if "=" not in line or line.lstrip().startswith("#"):
            continue
        key, value = line.split("=", 1)
        value = value.strip()
        if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
            value = value[1:-1]
        env[key.strip()] = value
    if env.get("AI_ENABLED") != "true" or env.get("BILLING_ENABLED") != "false" or not env.get("GEMINI_API_KEY"):
        raise SystemExit("Requires real AI enabled, key configured and billing disabled.")
    cloud = env["SUPABASE_URL"]
    if not cloud.startswith("https://"):
        raise SystemExit("Supabase HTTPS required.")
    key = env["SUPABASE_SERVICE_KEY"]
    admin = {"apikey": key}
    if not key.startswith("sb_secret_"):
        admin["Authorization"] = "Bearer " + key
    public = next(line.split("=", 1)[1].strip() for line in (ROOT / "MyCoach/Config/Local.xcconfig").read_text().splitlines()
                  if line.startswith("MYCOACH_SUPABASE_PUBLISHABLE_KEY"))

    def call(base, method, path, body=None, headers=None, expected=200, mime="application/json", raw=False):
        data = body if isinstance(body, bytes) else json.dumps(body).encode() if body is not None else None
        req = urllib.request.Request(base + path, data=data, method=method,
                                     headers={"Content-Type": mime, **(headers or {})})
        try:
            r = urllib.request.urlopen(req, timeout=65)
        except urllib.error.HTTPError as e:
            r = e
        payload = r.read()
        if r.status != expected:
            raise RuntimeError(f"HTTP {r.status}; expected {expected}")
        return payload if raw else json.loads(payload) if payload else None

    checks = []
    user = None
    token = None
    deleted = False
    def passed(name):
        checks.append(name)
        print(name + "=passed", flush=True)

    try:
        credentials = {"email": "mycoach-ai-smoke-" + uuid.uuid4().hex + "@example.invalid", "password": secrets.token_urlsafe(40)}
        user = call(cloud, "POST", "/auth/v1/admin/users", {**credentials, "email_confirm": True}, admin)["id"]
        token = call(cloud, "POST", "/auth/v1/token?grant_type=password", credentials, {"apikey": public})["access_token"]
        auth = {"Authorization": "Bearer " + token}
        call(args.api, "PATCH", "/v1/me", {"ageConfirmed": True, "aiConsent": True, "storeImages": True}, auth)
        tones = call(args.api, "GET", "/v1/tones", headers=auth)
        tone = next(t for t in tones if t["name"] == "Casual")
        contact = call(args.api, "POST", "/v1/contacts", {"name": "Fictional adult", "interests": ["books", "hiking"], "source": "OTHER"}, auth)
        def chunk(kind, data):
            return struct.pack("!I", len(data)) + kind + data + struct.pack("!I", zlib.crc32(kind + data) & 0xFFFFFFFF)
        png = b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack("!2I5B", 16, 16, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress((b"\x00" + b"\x00\x00\x00" * 16) * 16)) + chunk(b"IEND", b"")
        boundary = "mycoach-" + uuid.uuid4().hex
        fields = {"toneId": tone["id"], "contactId": contact["id"], "idempotencyKey": "live-gemini-synthetic",
                  "note": "Synthetic test only. The plain image has no person. Use the fictional interests to write respectful openers."}
        def multipart(values):
            body = b"".join(f'--{boundary}\r\nContent-Disposition: form-data; name="{k}"\r\n\r\n{v}\r\n'.encode() for k, v in values.items())
            return body + f'--{boundary}\r\nContent-Disposition: form-data; name="image"; filename="synthetic.png"\r\nContent-Type: image/png\r\n\r\n'.encode() + png + f"\r\n--{boundary}--\r\n".encode()
        mime = "multipart/form-data; boundary=" + boundary
        started = datetime.datetime.now(datetime.timezone.utc)
        result = call(args.api, "POST", "/v1/generations/opener", multipart(fields), auth, mime=mime)
        elapsed = (datetime.datetime.now(datetime.timezone.utc) - started).total_seconds()
        suggestions = result["suggestions"]
        assert len(suggestions) == 5
        assert len({s["text"].strip().casefold() for s in suggestions}) == 5
        assert all(0 < len(s["text"]) <= 220 for s in suggestions)
        assert result["usage"]["remainingToday"] == 2
        passed("real_gemini_five_distinct_valid_opener_suggestions")
        replay = call(args.api, "POST", "/v1/generations/opener", multipart(fields), auth, mime=mime)
        assert replay["generation"]["id"] == result["generation"]["id"]
        assert replay["suggestions"] == suggestions
        conflict = call(args.api, "POST", "/v1/generations/opener", multipart({**fields, "note": "Different synthetic input"}), auth, expected=409, mime=mime)
        assert conflict["code"] == "IDEMPOTENCY_CONFLICT"
        assert call(args.api, "GET", "/v1/me/usage", headers=auth)["remainingToday"] == 2
        passed("idempotent_replay_and_conflict_do_not_spend_extra_credit")
        image = call(args.api, "GET", "/v1/generations/" + result["generation"]["id"] + "/images/0", headers=auth, raw=True)
        assert image == png
        passed("retained_private_image_roundtrip")
    finally:
        if user:
            if token:
                try:
                    call(args.api, "DELETE", "/v1/me", headers={"Authorization": "Bearer " + token})
                    deleted = True
                except Exception:
                    pass
            if not deleted:
                # Only this invocation's synthetic user/prefix; never unrelated data.
                objects = call(cloud, "POST", "/storage/v1/object/list/inputs", {"prefix": user, "limit": 100}, admin)
                for folder in objects:
                    prefix = user + "/" + folder["name"]
                    for obj in call(cloud, "POST", "/storage/v1/object/list/inputs", {"prefix": prefix, "limit": 100}, admin):
                        call(cloud, "DELETE", "/storage/v1/object/inputs", {"prefixes": [prefix + "/" + obj["name"]]}, admin)
                call(cloud, "DELETE", "/auth/v1/admin/users/" + user, headers=admin)
            call(cloud, "GET", "/auth/v1/admin/users/" + user, headers=admin, expected=404)
            assert call(cloud, "POST", "/storage/v1/object/list/inputs", {"prefix": user, "limit": 100}, admin) == []
            if deleted:
                call(args.api, "GET", "/v1/me", headers={"Authorization": "Bearer " + token}, expected=401)
            passed("synthetic_auth_database_storage_cleanup")
    evidence = {"verifiedAt": datetime.datetime.now(datetime.timezone.utc).isoformat(), "model": env["GEMINI_MODEL"],
                "billingTierObserved": "Free tier", "checks": checks, "openerRoundtripSeconds": round(elapsed, 2),
                "syntheticAccountsCreatedAndDeleted": 1, "personalDataSent": False, "billingEnabledByAgent": False,
                "note": "One successful opener inference; replay/conflict do not perform additional inference. No content, IDs, signed URLs, tokens or credentials recorded."}
    (ROOT / "docs/infrastructure/gemini-verification.json").write_text(json.dumps(evidence, indent=2) + "\n")


if __name__ == "__main__":
    try:
        main()
    except Exception as e:
        raise SystemExit("Gemini smoke failed: " + (str(e) if isinstance(e, RuntimeError) and str(e).startswith("HTTP ") else type(e).__name__)) from None
