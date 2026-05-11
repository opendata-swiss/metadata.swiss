#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

ORG_NAME="${1:-}"
if [ -z "$ORG_NAME" ]; then
  echo "No organization name provided, deleting all organizations..."
  for file in piveau_organizations/*.ttl; do
    echo "Deleting ${HUB_REPO_ENDPOINT}/organizations/$(basename "$file" .ttl)"
    curl -i -X DELETE -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" "${HUB_REPO_ENDPOINT}/organizations/$(basename "$file" .ttl)"
  done 
else   
    echo "Deleting ${HUB_REPO_ENDPOINT}/organizations/${ORG_NAME}"
    curl -i -X DELETE -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" "${HUB_REPO_ENDPOINT}/organizations/${ORG_NAME}"
fi
