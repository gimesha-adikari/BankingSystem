#!/usr/bin/env bash
# ==============================================================================
# verify-pre-flyway-schema.sh
# 
# Purpose: Pre-Flyway adoption safety tool.
# Validates that an existing MySQL database matches the canonical V1 baseline
# schema BEFORE an operator executes an explicit Flyway baseline.
#
# Rules:
# - Read-only: NEVER modifies, baselines, or updates the database.
# - Exit code 0: Schema is 100% compatible with V1 baseline.
# - Exit code 1: Incompatible drift, missing tables, or mismatch detected.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
V1_PATH="$REPO_ROOT/backend/corebank/src/main/resources/db/migration/V1__baseline.sql"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3307}"
DB_USER="${DB_USER:-banking_dev}"
DB_PASSWORD="${DB_PASSWORD:-change-me-locally}"
DB_NAME="${DB_NAME:-banking_system_dev}"

# Parse optional command-line flags
while [[ $# -gt 0 ]]; do
  case $1 in
    --host) DB_HOST="$2"; shift 2 ;;
    --port) DB_PORT="$2"; shift 2 ;;
    --user) DB_USER="$2"; shift 2 ;;
    --password) DB_PASSWORD="$2"; shift 2 ;;
    --database) DB_NAME="$2"; shift 2 ;;
    --help)
      echo "Usage: $0 [--host HOST] [--port PORT] [--user USER] [--password PASS] [--database DB]"
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

if [[ ! -f "$V1_PATH" ]]; then
  echo "ERROR: V1 baseline file not found at: $V1_PATH" >&2
  exit 1
fi

export MYSQL_PWD="$DB_PASSWORD"

# Test connectivity
if ! mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" -e "SELECT 1;" >/dev/null 2>&1; then
  echo "ERROR: Cannot connect to MySQL database '$DB_NAME' on $DB_HOST:$DB_PORT with user '$DB_USER'" >&2
  exit 1
fi

TMP_DUMP="$(mktemp)"
trap 'rm -f "$TMP_DUMP"' EXIT

mysqldump -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" \
  --no-data --no-tablespaces --skip-comments --skip-dump-date \
  "$DB_NAME" > "$TMP_DUMP"

python3 - "$V1_PATH" "$TMP_DUMP" << 'EOF'
import sys
import re

v1_path = sys.argv[1]
dump_path = sys.argv[2]

def parse_tables(filepath, ignore_tables={'flyway_schema_history'}):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()

    pattern = re.compile(r'CREATE TABLE\s+(?:IF NOT EXISTS\s+)?`?([^`\s(]+)`?\s*\(([\s\S]*?)\)\s*ENGINE=InnoDB.*?;', re.IGNORECASE)
    tables = {}
    for match in pattern.finditer(content):
        tname = match.group(1).strip('`')
        if tname in ignore_tables:
            continue
        body = match.group(2)
        # Normalize lines
        raw_lines = [l.strip().rstrip(',') for l in body.splitlines() if l.strip()]
        norm_lines = []
        for l in raw_lines:
            # Strip comments and AUTO_INCREMENT values
            l = re.sub(r'/\*![\s\S]*?\*/', '', l).strip()
            l = re.sub(r'AUTO_INCREMENT=\d+', '', l).strip()
            if l:
                norm_lines.append(l)
        tables[tname] = sorted(norm_lines)
    return tables

v1_tables = parse_tables(v1_path)
dump_tables = parse_tables(dump_path)

v1_names = set(v1_tables.keys())
dump_names = set(dump_tables.keys())

missing_tables = v1_names - dump_names
extra_tables = dump_names - v1_names

has_error = False

if missing_tables:
    print(f"FAILED: Target database is missing required tables: {sorted(missing_tables)}")
    has_error = True

if extra_tables:
    print(f"FAILED: Target database contains unexpected extra tables: {sorted(extra_tables)}")
    has_error = True

for t in sorted(v1_names.intersection(dump_names)):
    v1_def = v1_tables[t]
    dump_def = dump_tables[t]
    if v1_def != dump_def:
        print(f"FAILED: Schema drift detected in table '{t}':")
        diff_v1 = set(v1_def) - set(dump_def)
        diff_dump = set(dump_def) - set(v1_def)
        if diff_v1:
            print("  Missing or altered in target DB:")
            for d in sorted(diff_v1):
                print(f"    - {d}")
        if diff_dump:
            print("  Unexpected definitions in target DB:")
            for d in sorted(diff_dump):
                print(f"    + {d}")
        has_error = True

if has_error:
    print("\nVERDICT: INCOMPATIBLE DRIFT DETECTED — ABORT BASELINE!")
    sys.exit(1)
else:
    print(f"SUCCESS: Target database schema matches V1 baseline ({len(v1_names)} tables verified, 0 differences).")
    print("VERDICT: DATABASE IS SAFE TO BASELINE AT VERSION 1.")
    sys.exit(0)
EOF
