#!/usr/bin/env bash
# ==============================================================================
# baseline-existing-database.sh
#
# Purpose: One-time operator tool to baseline an existing pre-Flyway database.
# Invariant: NEVER baselines without verifying schema compatibility first.
# ==============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3307}"
DB_USER="${DB_USER:-banking_dev}"
DB_PASSWORD="${DB_PASSWORD:-change-me-locally}"
DB_NAME="${DB_NAME:-banking_system_dev}"
BASELINE_VERSION="${BASELINE_VERSION:-1}"

# Parse optional command-line flags
while [[ $# -gt 0 ]]; do
  case $1 in
    --host) DB_HOST="$2"; shift 2 ;;
    --port) DB_PORT="$2"; shift 2 ;;
    --user) DB_USER="$2"; shift 2 ;;
    --password) DB_PASSWORD="$2"; shift 2 ;;
    --database) DB_NAME="$2"; shift 2 ;;
    --version) BASELINE_VERSION="$2"; shift 2 ;;
    --help)
      echo "Usage: $0 [--host HOST] [--port PORT] [--user USER] [--password PASS] [--database DB] [--version VER]"
      exit 0
      ;;
    *) echo "Unknown option: $1"; exit 1 ;;
  esac
done

echo ">>> Step 1: Verifying target database schema compatibility..."
if ! "$SCRIPT_DIR/verify-pre-flyway-schema.sh" --host "$DB_HOST" --port "$DB_PORT" --user "$DB_USER" --password "$DB_PASSWORD" --database "$DB_NAME"; then
  echo ">>> ABORT: Schema verification failed. Refusing to baseline drifted database!" >&2
  exit 1
fi

echo ">>> Step 2: Applying explicit Flyway baseline at version $BASELINE_VERSION..."
export MYSQL_PWD="$DB_PASSWORD"

mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" "$DB_NAME" << SQL
CREATE TABLE IF NOT EXISTS \`flyway_schema_history\` (
  \`installed_rank\` int NOT NULL,
  \`version\` varchar(50) DEFAULT NULL,
  \`description\` varchar(200) NOT NULL,
  \`type\` varchar(20) NOT NULL,
  \`script\` varchar(1000) NOT NULL,
  \`checksum\` int DEFAULT NULL,
  \`installed_by\` varchar(100) NOT NULL,
  \`installed_on\` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,
  \`execution_time\` int NOT NULL,
  \`success\` tinyint(1) NOT NULL,
  PRIMARY KEY (\`installed_rank\`),
  KEY \`flyway_schema_history_s_idx\` (\`success\`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO \`flyway_schema_history\` 
  (\`installed_rank\`, \`version\`, \`description\`, \`type\`, \`script\`, \`checksum\`, \`installed_by\`, \`execution_time\`, \`success\`)
VALUES 
  (1, '$BASELINE_VERSION', '<< Flyway Baseline >>', 'BASELINE', '<< Flyway Baseline >>', NULL, USER(), 0, 1);
SQL

echo ">>> SUCCESS: Database '$DB_NAME' successfully baselined at version $BASELINE_VERSION."
