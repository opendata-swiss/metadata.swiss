#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

CATALOGUE_NAME="${1:-}"
if [ -z "$CATALOGUE_NAME" ]; then
  echo "deleting all catalogues..."
  for file in piveau_catalogues/*.ttl; do
    echo "Deleting ${HUB_REPO_ENDPOINT}/catalogues/$(basename "$file" .ttl)"
    curl -i -X DELETE -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" "${HUB_REPO_ENDPOINT}/catalogues/$(basename "$file" .ttl)"
  done 
else   
    echo "Deleting ${HUB_REPO_ENDPOINT}/catalogues/${CATALOGUE_NAME}"
    curl -i -X DELETE -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" "${HUB_REPO_ENDPOINT}/catalogues/${CATALOGUE_NAME}"
fi




