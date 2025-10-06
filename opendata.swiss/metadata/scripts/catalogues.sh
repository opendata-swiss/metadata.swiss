#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a


curl -i -X PUT -H "X-API-Key: $API_KEY_HUB" -H "Content-Type: text/turtle" --data @piveau_catalogues/data-staatskanzlei-kanton-zuerich.ttl "${HUB_REPO_ENDPOINT}/catalogues/staatskanzlei-kanton-zuerich"
curl -i -X PUT -H "X-API-Key: $API_KEY_HUB" -H "Content-Type: text/turtle" --data @piveau_catalogues/data-bafu.ttl "${HUB_REPO_ENDPOINT}/catalogues/bafu"
curl -i -X PUT -H "X-API-Key: $API_KEY_HUB" -H "Content-Type: text/turtle" --data @piveau_catalogues/data-stadt-winterthur.ttl "${HUB_REPO_ENDPOINT}/catalogues/stadt-winterthur-geocat"
