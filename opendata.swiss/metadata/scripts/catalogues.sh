#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu


for file in piveau_catalogues/*.ttl; do
    echo "Uploading $file to ${HUB_REPO_ENDPOINT}/catalogues/$(basename "$file" .ttl)"
    curl -i -X PUT -H "X-API-Key: ${PIVEAU_HUB_API_KEY}" -H "Content-Type: text/turtle" --data @"$file" "${HUB_REPO_ENDPOINT}/catalogues/$(basename "$file" .ttl)"
done
