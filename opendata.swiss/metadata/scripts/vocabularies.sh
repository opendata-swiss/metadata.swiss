#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a



curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" --data @piveau_vocabularies/licenses-20240716.ttl "${HUB_REPO_ENDPOINT}/vocabularies/ch-licenses"

