#!/usr/bin/env python3
"""Run the application's Flyway migrations without starting AI/billing services.

Reads only DATABASE_* from the private root .env (literal values, never shell code).
No credentials in argv or output. Only info, validate, migrate are exposed.
"""
import argparse
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("action", choices=("info", "validate", "migrate"))
    args = parser.parse_args()
    values = {}
    path = ROOT / ".env"
    if not path.exists():
        sys.exit("Create the private root .env from .env.example first.")
    for line in path.read_text().splitlines():
        if not line.strip() or line.lstrip().startswith("#") or "=" not in line:
            continue
        key, value = line.split("=", 1)
        if key.strip() in ("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD"):
            value = value.strip()
            if len(value) >= 2 and value[0] == value[-1] and value[0] in "\"'":
                value = value[1:-1]
            values[key.strip()] = value
    for key in ("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD"):
        if not values.get(key):
            sys.exit(f"Fill {key} in the private .env; never send credentials in chat.")
    url = values["DATABASE_URL"]
    if not url.startswith("jdbc:postgresql://") or "password=" in url.lower() or "user=" in url.lower():
        sys.exit("Use a PostgreSQL JDBC URL with credentials in separate fields.")
    env = os.environ.copy()
    env.update(values)
    # Do not allow unrelated Flyway overrides from a developer's shell.
    env = {k: v for k, v in env.items() if not k.startswith("FLYWAY_")}
    result = subprocess.run(
        [str(ROOT / "backend/mvnw"), "-B", "--no-transfer-progress", f"flyway:{args.action}"],
        cwd=ROOT / "backend", env=env, stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
        text=True,
    )
    output = result.stdout
    for value in sorted(values.values(), key=len, reverse=True):
        output = output.replace(value, "[REDACTED]")
    print(output, end="")
    return result.returncode


if __name__ == "__main__":
    sys.exit(main())
