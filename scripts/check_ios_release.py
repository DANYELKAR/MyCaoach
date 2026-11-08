#!/usr/bin/env python3
"""Validate publication settings in a built app without printing credential values."""
import argparse
import json
import plistlib
import re
from pathlib import Path
from urllib.parse import urlsplit

parser = argparse.ArgumentParser()
parser.add_argument('app', type=Path, help='MyCoach.app directory, e.g. inside an xcarchive')
args = parser.parse_args()
with (args.app / 'Info.plist').open('rb') as f:
    info = plistlib.load(f)
missing = []
def value(key):
    raw = str(info.get(key, '')).strip()
    return raw if raw and '$(' not in raw else ''
def https(key):
    raw = value(key)
    try:
        url = urlsplit(raw)
        valid = url.scheme == 'https' and url.hostname and not url.username and not url.password and not url.hostname.endswith(('.example', '.test', '.invalid')) and url.hostname not in ('localhost','127.0.0.1')
    except ValueError:
        valid = False
    if not valid:
        missing.append(key)
for key in ['MyCoachAPIBaseURL','MyCoachSupabaseURL','MyCoachPrivacyURL','MyCoachTermsURL']:
    https(key)
if value('MyCoachBackendMode') != 'supabase': missing.append('MyCoachBackendMode')
key = value('MyCoachSupabasePublishableKey')
if not key or key.startswith('sb_secret_'): missing.append('MyCoachSupabasePublishableKey')
if not value('MyCoachRevenueCatPublicKey').startswith('appl_'): missing.append('MyCoachRevenueCatPublicKey')
google = value('MyCoachGoogleClientID')
if not google.endswith('.apps.googleusercontent.com'): missing.append('MyCoachGoogleClientID')
else:
    reversed_id = '.'.join(reversed(google.split('.')))
    schemes = [s for v in info.get('CFBundleURLTypes', []) for s in v.get('CFBundleURLSchemes', [])]
    if reversed_id not in schemes: missing.append('Google reversed client ID URL scheme')
if not re.fullmatch(r'[^\s@]+@[^\s@]+\.[^\s@]+', value('MyCoachSupportEmail')): missing.append('MyCoachSupportEmail')
print(json.dumps({'configurationReady': not missing, 'missingOrInvalidFields': missing, 'note': 'This checks configuration only; signed device/provider/sandbox validation is separate.'}, indent=2))
raise SystemExit(1 if missing else 0)
