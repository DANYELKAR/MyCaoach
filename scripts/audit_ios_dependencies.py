#!/usr/bin/env python3
"""Query pinned Swift packages against OSV without reading application credentials."""
import datetime
import json
import urllib.request
from pathlib import Path
from urllib.parse import urlsplit

root = Path(__file__).resolve().parents[1]
pins = json.loads((root/'MyCoach/MyCoach.xcodeproj/project.xcworkspace/xcshareddata/swiftpm/Package.resolved').read_text())['pins']
queries = []
for pin in pins:
    url = urlsplit(pin['location'])
    name = (url.netloc + url.path).removesuffix('.git').rstrip('/')
    queries.append({'package': {'ecosystem': 'SwiftURL', 'name': name}, 'version': pin['state']['version']})
request = urllib.request.Request('https://api.osv.dev/v1/querybatch', data=json.dumps({'queries': queries}).encode(), headers={'Content-Type': 'application/json'})
with urllib.request.urlopen(request, timeout=40) as response:
    results = json.load(response)['results']
report = {'checkedAt': datetime.datetime.now(datetime.timezone.utc).isoformat(), 'source': 'https://api.osv.dev/v1/querybatch', 'scope': 'Pinned SwiftURL package versions; this is an advisory lookup, not proof of absence of vulnerabilities.', 'packages': []}
for pin, query, result in zip(pins, queries, results):
    report['packages'].append({'name': query['package']['name'], 'version': pin['state']['version'], 'revision': pin['state']['revision'], 'advisories': [v['id'] for v in result.get('vulns', [])]})
output = root/'docs/ios/dependency-audit.json'
output.write_text(json.dumps(report, indent=2)+'\n')
count = sum(len(p['advisories']) for p in report['packages'])
print(f'Queried {len(pins)} pinned packages; {count} advisory matches. Report: docs/ios/dependency-audit.json')
raise SystemExit(1 if count else 0)
