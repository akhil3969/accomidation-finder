#!/usr/bin/env bash
# ---------------------------------------------------------------------------
#  Restore a backup made by backup-db.sh into a database.
#
#    DB_HOST=... DB_PORT=3306 DB_NAME=restore_check DB_USER=... DB_PASSWORD=... \
#      scripts/restore-db.sh backups/accommodation_finder-20261027T033000Z.sql.gz
#
#  Creates DB_NAME if it does not exist, and refuses to load into a database
#  that already has tables unless FORCE=1 - restoring over production by
#  mistake should take a deliberate extra step.
#
#  For a restore drill, point DB_NAME at a scratch database, then start the app
#  against it (or compare row counts) to prove the backup is usable.
# ---------------------------------------------------------------------------
set -euo pipefail

file="${1:?usage: restore-db.sh <backup.sql.gz>}"
: "${DB_NAME:?set DB_NAME}" "${DB_USER:?set DB_USER}" "${DB_PASSWORD:?set DB_PASSWORD}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"

# DB_NAME is spliced into SQL below; allow only a plain identifier.
if [[ ! "$DB_NAME" =~ ^[A-Za-z0-9_]+$ ]]; then
  echo "restore-db: DB_NAME may only contain letters, digits and _" >&2
  exit 1
fi

gzip -t "$file"
export MYSQL_PWD="$DB_PASSWORD"
mysql_cmd=(mysql --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER")

"${mysql_cmd[@]}" -e "CREATE DATABASE IF NOT EXISTS \`$DB_NAME\` CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci"

existing="$("${mysql_cmd[@]}" -N -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$DB_NAME'")"
if [[ "$existing" -gt 0 && "${FORCE:-0}" != "1" ]]; then
  echo "restore-db: $DB_NAME already has $existing table(s). Use an empty database, or FORCE=1 to overwrite." >&2
  exit 1
fi

gzip -dc "$file" | "${mysql_cmd[@]}" "$DB_NAME"

tables="$("${mysql_cmd[@]}" -N -e \
  "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = '$DB_NAME'")"
echo "restore-db: restored $tables table(s) into $DB_NAME"
