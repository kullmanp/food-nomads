#!/usr/bin/env bash
# Dumps the database into backups/ and keeps the newest 14 dumps.
# Runs nightly from cron and before every deploy.
# Restore: see the nomads page in the home-automation wiki.
set -euo pipefail
cd "$(dirname "$0")"

if [ -z "$(docker compose ps --status running -q db)" ]; then
    echo "Database is not running, no backup taken."
    exit 0
fi

mkdir -p backups
file="backups/foodnomads-$(date +%Y-%m-%d-%H%M%S).dump"
docker compose exec -T db pg_dump -U foodnomads -d foodnomads --format=custom > "$file.tmp"
mv "$file.tmp" "$file"
echo "Backup: $file"

ls -1t backups/*.dump | tail -n +15 | xargs -r rm --
