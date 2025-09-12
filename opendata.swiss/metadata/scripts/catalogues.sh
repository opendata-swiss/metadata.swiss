#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" --data @piveau_catalogues/data-staatskanzlei-kanton-zuerich.ttl "${HUB_REPO_ENDPOINT}/catalogues/staatskanzlei-kanton-zuerich"
curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" --data @piveau_catalogues/data-bafu.ttl "${HUB_REPO_ENDPOINT}/catalogues/bafu"
