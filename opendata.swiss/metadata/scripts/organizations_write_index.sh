#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

ORG_NAME="${1:-}"
if [ -z "$ORG_NAME" ]; then
  echo "No organization name provided, uploading all organizations..."
  for file in piveau_organizations/es/*.json; do
    echo "Uploading $(basename "$file") to ${HUB_SEARCH_ENDPOINT}/organizations/$(basename "$file" .json)"
    curl -i -X PUT -H "X-API-Key: ${PIVEAU_HUB_SEARCH_API_KEY}" -H "Content-Type: application/json" --data @"$file" "${HUB_SEARCH_ENDPOINT}/organizations/$(basename "$file" .json)"
  done
else   
    echo "Uploading ${ORG_NAME} to ${HUB_SEARCH_ENDPOINT}/organizations/${ORG_NAME}"
    curl -i -X PUT -H "X-API-Key: ${PIVEAU_HUB_SEARCH_API_KEY}" -H "Content-Type: application/json" --data @"piveau_organizations/es/${ORG_NAME}.json" "${HUB_SEARCH_ENDPOINT}/organizations/${ORG_NAME}"
fi
