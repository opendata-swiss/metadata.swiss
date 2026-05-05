#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

curl -i -X POST -H "X-API-Key:${PIVEAU_METRICS_CACHE_API_KEY}" http://localhost:8185/admin/refresh/
