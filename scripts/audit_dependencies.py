#!/usr/bin/env python3
"""Query public Maven coordinates against OSV. Excludes OS and test dependencies."""
import concurrent.futures
import datetime
import json
from pathlib import Path
import re
import subprocess
import tempfile
import urllib.request

ROOT=Path(__file__).resolve().parents[1]
with tempfile.TemporaryDirectory(prefix="spark-audit-") as directory:
    resolved=Path(directory)/"dependencies.txt"
    subprocess.run([str(ROOT/"backend/mvnw"),"-f",str(ROOT/"backend/pom.xml"),"-B","--no-transfer-progress","org.apache.maven.plugins:maven-dependency-plugin:3.8.1:list","-DincludeScope=runtime","-DoutputFile="+str(resolved)],cwd=ROOT,check=True,stdout=subprocess.DEVNULL)
    packages=[]
    for line in resolved.read_text().splitlines():
        match=re.match(r"\s+([^: ]+):([^: ]+):jar:([^: ]+):(compile|runtime)",line)
        if match:
            packages.append({"package":{"ecosystem":"Maven","name":match[1]+":"+match[2]},"version":match[3]})
request=urllib.request.Request("https://api.osv.dev/v1/querybatch",data=json.dumps({"queries":packages}).encode(),headers={"Content-Type":"application/json"})
response=json.load(urllib.request.urlopen(request,timeout=40))
findings=[]
for package,result in zip(packages,response["results"]):
    if result.get("vulns"):
        findings.append({"package":package["package"]["name"],"version":package["version"],"vulnerabilities":[v["id"] for v in result["vulns"]]})
known={"GHSA-j9f9-w8pj-32f8","GHSA-pc63-qcmh-9cmg"}
assessments={
    "GHSA-j9f9-w8pj-32f8":{"feature":"SSE with view fragments","assessment":"Not used by the current application. REST controller surface regression test is required.","source":"https://spring.io/security/cve-2026-47890"},
    "GHSA-pc63-qcmh-9cmg":{"feature":"XsltView with wildcard implicit view rendering","assessment":"No XSLT view resolver or wildcard application controller; regression test is required.","source":"https://github.com/spring-projects/spring-framework/commit/692dbc9160de3b5de50ccedeee014716d0cedb7b"},
}
report={"source":"https://api.osv.dev/v1/querybatch","date":datetime.date.today().isoformat(),"scope":"Maven compile/runtime; excludes container OS and test tooling","packagesChecked":len(packages),"findings":findings,"assessments":assessments,"regressionTest":"FoundationIntegrationTest.mvcDoesNotExposeSseFragmentsOrXsltViews"}
(ROOT/"docs/backend/dependency-audit.json").write_text(json.dumps(report,indent=2)+"\n")
unknown=[v for finding in findings for v in finding["vulnerabilities"] if v not in known]
print(f"Checked {len(packages)} runtime Maven dependencies; {sum(len(f['vulnerabilities']) for f in findings)} reported advisories, {len(unknown)} unassessed.")
if unknown:
    raise SystemExit("Dependency advisories require assessment: "+", ".join(unknown))
