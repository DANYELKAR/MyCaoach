#!/usr/bin/env python3
"""Exercise the running LOCAL profile only. Creates and deletes its own test accounts."""
import json
import os
import struct
import urllib.request
import urllib.error
import uuid
import zlib
import time
import subprocess
from pathlib import Path

BASE = os.environ.get("SPARK_LOCAL_URL", "http://127.0.0.1:8080")
if not BASE.startswith(("http://127.0.0.1:", "http://localhost:")):
    raise SystemExit("This smoke script targets a local development server only.")

def call(method, path, body=None, token=None, expected=200, content_type="application/json"):
    headers = {"Content-Type": content_type}
    if token:
        headers["Authorization"] = "Bearer " + token
    data = body if isinstance(body, bytes) else json.dumps(body).encode() if body is not None else None
    request = urllib.request.Request(BASE + path, data=data, headers=headers, method=method)
    try:
        response = urllib.request.urlopen(request, timeout=60)
    except urllib.error.HTTPError as error:
        response = error
    raw = response.read()
    result = json.loads(raw) if raw else None
    if response.status != expected:
        raise AssertionError(f"{method} {path}: expected {expected}, got {response.status}; code={result.get('code') if isinstance(result, dict) else 'unknown'}")
    if expected >= 400:
        assert result["requestId"] == response.headers["X-Request-ID"]
    return result

def png():
    def chunk(kind, data):
        return struct.pack("!I", len(data)) + kind + data + struct.pack("!I", zlib.crc32(kind + data) & 0xFFFFFFFF)
    return b"\x89PNG\r\n\x1a\n" + chunk(b"IHDR", struct.pack("!2I5B", 1, 1, 8, 2, 0, 0, 0)) + chunk(b"IDAT", zlib.compress(b"\x00\xff\x00\x00")) + chunk(b"IEND", b"")

def multipart(fields, image=None):
    boundary = "spark-" + uuid.uuid4().hex
    chunks = []
    for name, value in fields.items():
        chunks.append(f'--{boundary}\r\nContent-Disposition: form-data; name="{name}"\r\n\r\n{value}\r\n'.encode())
    if image is not None:
        chunks.append(f'--{boundary}\r\nContent-Disposition: form-data; name="image"; filename="fixture.png"\r\nContent-Type: image/png\r\n\r\n'.encode() + image + b"\r\n")
    chunks.append(f"--{boundary}--\r\n".encode())
    return b"".join(chunks), "multipart/form-data; boundary=" + boundary

accounts = []
credentials = []
try:
    assert call("GET", "/actuator/health")["status"] == "UP"
    call("GET", "/v1/me", expected=401)
    for _ in range(2):
        credentials.append({"email": uuid.uuid4().hex + "@smoke.example.test", "password": "local-smoke-password-123"})
        accounts.append(call("POST", "/local/auth/register", credentials[-1]))
    token = accounts[0]["accessToken"]
    other = accounts[1]["accessToken"]
    profile = call("PATCH", "/v1/me", {"ageConfirmed": True, "aiConsent": True, "timezone": "Europe/Istanbul", "storeImages": True}, token)
    assert profile["plan"] == "FREE"
    tones = call("GET", "/v1/tones", token=token)
    assert len(tones) == 7
    contact = call("POST", "/v1/contacts", {"name": "Smoke person", "source": "OTHER", "interests": ["books"]}, token)
    call("GET", "/v1/contacts/" + contact["id"], token=other, expected=404)
    fields = {"toneId": tones[0]["id"], "contactId": contact["id"], "idempotencyKey": "smoke-opener"}
    data, mime = multipart(fields, png())
    opener = call("POST", "/v1/generations/opener", data, token, content_type=mime)
    assert len(opener["suggestions"]) == 5 and opener["usage"]["remainingToday"] == 2
    image_request=urllib.request.Request(BASE+"/v1/generations/"+opener["generation"]["id"]+"/images/0",headers={"Authorization":"Bearer "+token})
    with urllib.request.urlopen(image_request) as response:
        assert response.read()==png() and response.headers["Cache-Control"]=="no-store"
    call("GET","/v1/generations/"+opener["generation"]["id"]+"/images/0",token=other,expected=404)
    replay = call("POST", "/v1/generations/opener", data, token, content_type=mime)
    assert replay["generation"]["id"] == opener["generation"]["id"]
    folder = call("POST", "/v1/folders", {"name": "Smoke folder"}, token)
    suggestion = opener["suggestions"][0]["id"]
    call("POST", "/v1/saved", {"kind": "SUGGESTION", "suggestionId": suggestion, "folderId": folder["id"]}, token)
    call("PATCH", "/v1/suggestions/" + suggestion, {"feedback": "LIKE", "sentStatus": "SENT", "outcome": "REPLIED"}, token)
    call("POST", "/v1/suggestions/" + suggestion + "/report", {"reason": "smoke test"}, token)
    assert len(call("GET", "/v1/threads/" + opener["generation"]["threadId"], token=token)["generations"]) == 1
    call("PUT", "/local/billing/entitlement", {"active": True, "expiresAt": "2099-01-01T00:00:00Z"}, token)
    tone = call("POST", "/v1/tones", {"name": "Smoke tone", "styleInstruction": "Keep it warm and curious", "humorLevel": 60}, token)
    data, mime = multipart({"toneId": tone["id"], "text": "What are you reading?", "contactId": contact["id"], "idempotencyKey": "smoke-reply"})
    reply = call("POST", "/v1/generations/reply", data, token, content_type=mime)
    assert len(reply["suggestions"]) == 4
    call("PUT", "/v1/style-profile", {"sampleMessages": ["Hello there!", "How was your day?", "What are you reading?", "Tell me more", "That sounds fun!"]}, token)
    assert call("POST", "/v1/style-profile/analyze", {}, token)["summary"]["formality"] == "casual"
    call("POST", "/v1/saved", {"kind": "TEMPLATE", "text": "What made you smile today?"}, token)
    if os.environ.get("SPARK_TEST_RESTART")=="1":
        root=Path(__file__).resolve().parents[1]
        subprocess.run(["docker","compose","restart","postgres"],cwd=root,check=True,capture_output=True)
        subprocess.run(["docker","compose","up","-d","--wait","postgres"],cwd=root,check=True,capture_output=True)
        subprocess.run(["docker","compose","restart","api"],cwd=root,check=True,capture_output=True)
        deadline=time.monotonic()+45
        while True:
            try:
                call("GET","/actuator/health/readiness")
                break
            except (urllib.error.URLError,AssertionError,ConnectionError):
                if time.monotonic()>deadline:
                    raise
                time.sleep(0.3)
        call("GET","/v1/me",token=token,expected=401)
        for index,credential in enumerate(credentials):
            accounts[index]=call("POST","/local/auth/login",credential)
        token=accounts[0]["accessToken"]
        other=accounts[1]["accessToken"]
        assert call("GET","/v1/contacts/"+contact["id"],token=token)["name"]=="Smoke person"
        assert call("POST","/v1/billing/sync",token=token)["plan"]=="PLUS"
        with urllib.request.urlopen(urllib.request.Request(BASE+"/v1/generations/"+opener["generation"]["id"]+"/images/0",headers={"Authorization":"Bearer "+token})) as response:
            assert response.read()==png()
        print("PASS: PostgreSQL/API restart preserves users, contacts, image volume, generation history and canonical local Plus entitlement; new login required")
    before = call("GET", "/v1/me/usage", token=token)["remainingToday"]
    call("DELETE", "/v1/threads", token=token)
    assert call("GET", "/v1/me/usage", token=token)["remainingToday"] == before
    assert call("GET", "/v1/threads", token=token) == []
    spec = call("GET", "/v3/api-docs")
    path = Path(__file__).resolve().parents[1] / "docs/api/openapi.json"
    path.write_text(json.dumps(spec, indent=2, sort_keys=True) + "\n")
    print(f"PASS: local auth, profile, ownership, opener/replay, saved, feedback/report, timeline, billing, reply, style, tones, template, history and OpenAPI ({len(spec['paths'])} paths)")
finally:
    for account in accounts:
        call("DELETE", "/v1/me", token=account["accessToken"])
        call("GET", "/v1/me", token=account["accessToken"], expected=401)
    if accounts:
        print("PASS: smoke accounts deleted; old tokens rejected")
