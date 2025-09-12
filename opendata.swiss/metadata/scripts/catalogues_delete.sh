#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

curl -i -X DELETE -H "X-API-Key: $PIVEAU_HUB_API_KEY" "${HUB_REPO_ENDPOINT}/catalogues/staatskanzlei-kanton-zuerich"
curl -i -X DELETE -H "X-API-Key: $PIVEAU_HUB_API_KEY" "${HUB_REPO_ENDPOINT}/catalogues/bafu"
