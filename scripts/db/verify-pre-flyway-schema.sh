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
# - Exit code 0: Schema is 100% structurally compatible with V1 baseline.
# - Exit code 1: Incompatible drift, missing tables, or mismatch detected.
# - Security: NEVER accepts passwords on the command line.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"
V1_PATH="$REPO_ROOT/src/main/resources/db/migration/V1__baseline.sql"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3307}"
DB_USER="${DB_USER:-banking_dev}"
DB_NAME="${DB_NAME:-banking_system_dev}"
DEFAULTS_FILE=""

# Parse command-line flags
while [[ $# -gt 0 ]]; do
  case $1 in
    --host) DB_HOST="$2"; shift 2 ;;
    --port) DB_PORT="$2"; shift 2 ;;
    --user) DB_USER="$2"; shift 2 ;;
    --database) DB_NAME="$2"; shift 2 ;;
    --defaults-file|--defaults-extra-file) DEFAULTS_FILE="$2"; shift 2 ;;
    --password|--password=*)
      echo "FATAL: Passing passwords via command-line arguments is strictly prohibited for security." >&2
      echo "Use --defaults-file <path> or set DB_PASSWORD environment variable." >&2
      exit 1
      ;;
    --help)
      echo "Usage: $0 [--host HOST] [--port PORT] [--user USER] [--database DB] [--defaults-file PATH]"
      echo "Credentials: Set DB_PASSWORD or MYSQL_PWD environment variable, or use --defaults-file."
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

if [[ ! -f "$V1_PATH" ]]; then
  echo "ERROR: V1 baseline file not found at: $V1_PATH" >&2
  exit 1
fi

# Secure credentials resolution
TMP_CNF=""
cleanup() {
  if [[ -n "$TMP_CNF" && -f "$TMP_CNF" ]]; then
    rm -f "$TMP_CNF"
  fi
}
trap cleanup EXIT

MYSQL_AUTH_ARGS=()
if [[ -n "$DEFAULTS_FILE" ]]; then
  if [[ ! -f "$DEFAULTS_FILE" ]]; then
    echo "ERROR: Specified defaults-file not found: $DEFAULTS_FILE" >&2
    exit 1
  fi
  MYSQL_AUTH_ARGS=("--defaults-extra-file=$DEFAULTS_FILE")
else
  # Use environment variable to build a secure, ephemeral 0600 file
  PASS_VAL="${DB_PASSWORD:-${MYSQL_PWD:-${SPRING_DATASOURCE_PASSWORD:-}}}"
  TMP_CNF="$(mktemp -t db-creds.XXXXXX)"
  chmod 0600 "$TMP_CNF"
  cat > "$TMP_CNF" << EOF
[client]
password=$PASS_VAL
EOF
  MYSQL_AUTH_ARGS=("--defaults-extra-file=$TMP_CNF")
fi

# Test connectivity
if ! mysql "${MYSQL_AUTH_ARGS[@]}" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" -e "SELECT 1;" >/dev/null 2>&1; then
  echo "ERROR: Cannot connect to MySQL database '$DB_NAME' on $DB_HOST:$DB_PORT with user '$DB_USER'" >&2
  exit 1
fi

TMP_DUMP="$(mktemp -t db-dump.XXXXXX)"
trap 'cleanup; rm -f "$TMP_DUMP"' EXIT

mysqldump "${MYSQL_AUTH_ARGS[@]}" -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" \
  --no-data --no-tablespaces --skip-comments --skip-dump-date \
  "$DB_NAME" > "$TMP_DUMP"

python3 - "$V1_PATH" "$TMP_DUMP" << 'EOF'
import sys
import re

v1_file, target_file = sys.argv[1], sys.argv[2]

def parse_tables(sql_text):
    tables = {}
    blocks = re.findall(r'CREATE TABLE `?([a-zA-Z0-9_]+)`?\s*\((.*?)\)\s*ENGINE=InnoDB', sql_text, re.DOTALL | re.IGNORECASE)
    for table_name, body in blocks:
        if table_name == 'flyway_schema_history':
            continue
        lines = []
        for line in body.splitlines():
            line = line.strip().rstrip(',')
            if not line:
                continue
            line = re.sub(r'AUTO_INCREMENT=\d+', '', line)
            line = re.sub(r'\s+', ' ', line)
            lines.append(line)
        tables[table_name] = sorted(lines)
    return tables

with open(v1_file) as f:
    v1_tables = parse_tables(f.read())

with open(target_file) as f:
    target_tables = parse_tables(f.read())

v1_names = set(v1_tables.keys())
target_names = set(target_tables.keys())

missing_in_target = v1_names - target_names
extra_in_target = target_names - v1_names
has_diff = False

if missing_in_target:
    print(f"FAILED: Missing required tables in target database: {sorted(missing_in_target)}")
    has_diff = True

if extra_in_target:
    print(f"FAILED: Unexpected extra tables in target database: {sorted(extra_in_target)}")
    has_diff = True

for table in sorted(v1_names & target_names):
    v1_defs = set(v1_tables[table])
    tgt_defs = set(target_tables[table])
    
    missing_defs = v1_defs - tgt_defs
    extra_defs = tgt_defs - v1_defs
    
    if missing_defs or extra_defs:
        has_diff = True
        print(f"FAILED: Schema drift detected in table '{table}':")
        if missing_defs:
            print("  Missing or altered in target DB:")
            for d in sorted(missing_defs):
                print(f"    - {d}")
        if extra_defs:
            print("  Extra definitions in target DB:")
            for d in sorted(extra_defs):
                print(f"    + {d}")

if has_diff:
    print("\nVERDICT: INCOMPATIBLE DRIFT DETECTED — ABORT BASELINE!")
    sys.exit(1)
else:
    print(f"SUCCESS: Target database schema matches V1 baseline ({len(v1_names)} tables verified, 0 differences).")
    print("VERDICT: DATABASE IS SAFE TO BASELINE AT VERSION 1.")
    sys.exit(0)
EOF
