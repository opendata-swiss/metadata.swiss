#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

VOCAB_NAME="${1:-}"
if [ -z "$VOCAB_NAME" ]; then
  echo "No vocabulary name provided, deleting all vocabularies..."
  for file in piveau_vocabularies/*.ttl; do
    echo "Deleting ${HUB_REPO_ENDPOINT}/vocabularies/$(basename "$file" .ttl)"
    curl -i -X DELETE -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" "${HUB_REPO_ENDPOINT}/vocabularies/$(basename "$file" .ttl)"
  done 
else   
    echo "Deleting ${HUB_REPO_ENDPOINT}/vocabularies/${VOCAB_NAME}"
    curl -i -X DELETE -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" "${HUB_REPO_ENDPOINT}/vocabularies/${VOCAB_NAME}"
fi

# curl -i -X DELETE -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" "${HUB_REPO_ENDPOINT}/vocabularies/ch-licenses"
# curl -i -X DELETE -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" "${HUB_REPO_ENDPOINT}/vocabularies/showcase-types"
# curl -i -X DELETE -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" "${HUB_REPO_ENDPOINT}/vocabularies/legal-forms"
