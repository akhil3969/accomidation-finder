#!/usr/bin/env bash
# ---------------------------------------------------------------------------
#  Back up the MySQL database to a compressed, timestamped file.
#
#    DB_HOST=... DB_PORT=3306 DB_NAME=accommodation_finder DB_USER=... DB_PASSWORD=... \
#      scripts/backup-db.sh [backup-dir]          # default ./backups
#
#  Optional:
#    RETENTION_DAYS=30         delete local backups older than this
#    BACKUP_S3_URI=s3://...    also copy the file there (needs the aws CLI);
#                              a backup on the same disk as the database is not a backup
#
#  Schedule it daily (cron, or your host's cron-job feature) and run
#  scripts/restore-db.sh against a scratch database now and then - an
#  untested backup is a hope, not a backup. CI runs that drill on every push.
#
#  Needs the MySQL 8 client tools (mysqldump). Prints the file path on success.
# ---------------------------------------------------------------------------
set -euo pipefail

: "${DB_NAME:?set DB_NAME}" "${DB_USER:?set DB_USER}" "${DB_PASSWORD:?set DB_PASSWORD}"
DB_HOST="${DB_HOST:-127.0.0.1}"
DB_PORT="${DB_PORT:-3306}"
RETENTION_DAYS="${RETENTION_DAYS:-30}"
BACKUP_DIR="${1:-./backups}"

# The dump holds every user's personal data: owner-only permissions.
umask 077
mkdir -p "$BACKUP_DIR"

stamp="$(date -u +%Y%m%dT%H%M%SZ)"
file="$BACKUP_DIR/${DB_NAME}-${stamp}.sql.gz"
partial="$file.partial"
trap 'rm -f "$partial"' EXIT

# Via the environment, so the password never shows up in `ps`.
export MYSQL_PWD="$DB_PASSWORD"

# --single-transaction: a consistent snapshot without locking InnoDB tables,
#                       so the site stays up while this runs.
mysqldump \
  --host="$DB_HOST" --port="$DB_PORT" --user="$DB_USER" \
  --single-transaction --quick --routines --triggers \
  --no-tablespaces --set-gtid-purged=OFF \
  "$DB_NAME" | gzip -9 > "$partial"

# A dump cut short (full disk, dropped connection) still gzips fine, so check
# for the trailer mysqldump writes only when it finished.
gzip -t "$partial"
# (Captured first: piping into `grep -q` can end in SIGPIPE, which pipefail
# would report as a failure on a perfectly good dump.)
trailer="$(gzip -dc "$partial" | tail -n 5)"
if [[ "$trailer" != *"-- Dump completed"* ]]; then
  echo "backup-db: dump is incomplete, not keeping it" >&2
  exit 1
fi
mv "$partial" "$file"

if [[ -n "${BACKUP_S3_URI:-}" ]]; then
  aws s3 cp "$file" "${BACKUP_S3_URI%/}/" --only-show-errors
fi

find "$BACKUP_DIR" -name "${DB_NAME}-*.sql.gz" -type f -mtime +"$RETENTION_DAYS" -delete

echo "$file"
