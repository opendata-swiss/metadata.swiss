#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

ORG_NAME="${1:-}"
if [ -z "$ORG_NAME" ]; then
  echo "No organization name provided, uploading all organizations..."
  for file in piveau_organizations/*.ttl; do
    echo "Uploading $(basename "$file") to ${HUB_REPO_ENDPOINT}/organizations/$(basename "$file" .ttl)"
    curl -i -X PUT -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" -H "Content-Type: text/turtle" --data @"$file" "${HUB_REPO_ENDPOINT}/organizations/$(basename "$file" .ttl)"
  done
else   
    echo "Uploading ${ORG_NAME} to ${HUB_REPO_ENDPOINT}/organizations/${ORG_NAME}"
    curl -i -X PUT -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" -H "Content-Type: text/turtle" --data @"piveau_organizations/${ORG_NAME}.ttl" "${HUB_REPO_ENDPOINT}/organizations/${ORG_NAME}"
fi
