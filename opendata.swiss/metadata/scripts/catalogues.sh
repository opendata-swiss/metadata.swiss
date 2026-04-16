#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

CATALOGUE_NAME="${1:-}"
if [ -z "$CATALOGUE_NAME" ]; then
  echo "No catalogue name provided, uploading all catalogues..."
  for file in piveau_catalogues/*.ttl; do
    echo "Uploading $(basename "$file") to ${HUB_REPO_ENDPOINT}/catalogues/$(basename "$file" .ttl)"
    curl -i -X PUT -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" -H "Content-Type: text/turtle" --data @"$file" "${HUB_REPO_ENDPOINT}/catalogues/$(basename "$file" .ttl)"
  done
else   
    echo "Uploading ${CATALOGUE_NAME} to ${HUB_REPO_ENDPOINT}/catalogues/${CATALOGUE_NAME}"
    curl -i -X PUT -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" -H "Content-Type: text/turtle" --data @"piveau_catalogues/${CATALOGUE_NAME}.ttl" "${HUB_REPO_ENDPOINT}/catalogues/${CATALOGUE_NAME}"
fi


