#!/usr/bin/env bash
# ==============================================================================
# baseline-existing-database.sh
#
# Purpose: One-time operator tool to baseline an existing pre-Flyway database.
# Invariant: NEVER baselines without verifying schema compatibility first.
# Standard: Uses Flyway's official baseline API via FlywayBaselineOperator.
# Security: Plaintext passwords in process arguments are strictly prohibited.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/../.." && pwd)"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3307}"
DB_USER="${DB_USER:-banking_dev}"
DB_NAME="${DB_NAME:-banking_system_dev}"
BASELINE_VERSION="${BASELINE_VERSION:-1}"
DEFAULTS_FILE=""

# Parse optional command-line flags
while [[ $# -gt 0 ]]; do
  case $1 in
    --host) DB_HOST="$2"; shift 2 ;;
    --port) DB_PORT="$2"; shift 2 ;;
    --user) DB_USER="$2"; shift 2 ;;
    --database) DB_NAME="$2"; shift 2 ;;
    --version) BASELINE_VERSION="$2"; shift 2 ;;
    --defaults-file|--defaults-extra-file) DEFAULTS_FILE="$2"; shift 2 ;;
    --password|--password=*)
      echo "FATAL: Passing passwords via command-line arguments is strictly prohibited for security." >&2
      echo "Use --defaults-file <path> or set DB_PASSWORD environment variable." >&2
      exit 1
      ;;
    --help)
      echo "Usage: $0 [--host HOST] [--port PORT] [--user USER] [--database DB] [--version VER] [--defaults-file PATH]"
      echo "Credentials: Set DB_PASSWORD or MYSQL_PWD environment variable, or use --defaults-file."
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

VERIFY_ARGS=(
  "--host" "$DB_HOST"
  "--port" "$DB_PORT"
  "--user" "$DB_USER"
  "--database" "$DB_NAME"
)
if [[ -n "$DEFAULTS_FILE" ]]; then
  VERIFY_ARGS+=("--defaults-file" "$DEFAULTS_FILE")
fi

echo ">>> Step 1: Verifying target database schema compatibility..."
if ! "$SCRIPT_DIR/verify-pre-flyway-schema.sh" "${VERIFY_ARGS[@]}"; then
  echo ">>> ABORT: Schema verification failed. Refusing to baseline drifted database!" >&2
  exit 1
fi

echo ">>> Step 2: Applying official Flyway baseline at version $BASELINE_VERSION..."
JDBC_URL="jdbc:mysql://${DB_HOST}:${DB_PORT}/${DB_NAME}?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

GRADLE_ARGS=(
  "--url" "$JDBC_URL"
  "--user" "$DB_USER"
  "--version" "$BASELINE_VERSION"
)
if [[ -n "$DEFAULTS_FILE" ]]; then
  GRADLE_ARGS+=("--defaults-file" "$DEFAULTS_FILE")
fi

# Execute official Flyway baseline operation through Gradle JavaExec runner
(
  cd "$REPO_ROOT/backend/corebank"
  ./gradlew -q flywayBaseline --args="${GRADLE_ARGS[*]}"
)

echo ">>> SUCCESS: Database '$DB_NAME' successfully baselined at version $BASELINE_VERSION via official Flyway API."
