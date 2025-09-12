#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a



curl -i -X DELETE -H "X-API-Key: $PIVEAU_HUB_API_KEY" "${HUB_REPO_ENDPOINT}/vocabularies/ch-licenses"
