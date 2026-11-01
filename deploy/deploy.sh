#!/usr/bin/env bash
# Runs one version of the app: ./deploy.sh <full commit sha on main>
# Without an argument, prints the version that is configured now.
# Rolling back is deploying the previous sha again (printed at the end).
set -euo pipefail
cd "$(dirname "$0")"

current=$(sed -n 's/^APP_VERSION=//p' .env)
if [ $# -eq 0 ]; then
    echo "Configured version: ${current:-none}"
    exit 0
fi
version=$1
# Overrides .env for every compose call below, including the ones in backup.sh.
export APP_VERSION=$version

docker compose pull app

# Flyway migrates the schema at startup, so keep a dump of the state before.
./backup.sh

sed "s/^APP_VERSION=.*/APP_VERSION=$version/" .env > .env.new
mv .env.new .env
chmod 600 .env
docker compose up -d

echo "Waiting for the app to become ready..."
for _ in $(seq 60); do
    if curl -fs -o /dev/null http://127.0.0.1:8080/q/health/ready; then
        echo "Deployed $version (was ${current:-none})."
        exit 0
    fi
    sleep 2
done
echo "The app did not become ready. See: docker compose logs app" >&2
echo "Roll back with: $0 $current" >&2
exit 1
