#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

curl -i -X POST -H "X-API-Key:${PIVEAU_METRICS_CACHE_API_KEY}" "${PIVEAU_METRICS_CACHE_ENDPOINT}/admin/refresh/"
