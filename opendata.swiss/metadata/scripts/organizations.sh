#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" --data @piveau_organizations/org-zh-kanton-zuerich.ttl "${HUB_REPO_ENDPOINT}/resources/organization?id=kanton-zuerich&catalogId=organizations-ods"
curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" --data @piveau_organizations/org-zh-staatskanzlei-kanton-zuerich.ttl "${HUB_REPO_ENDPOINT}/resources/organization?id=staatskanzlei-kanton-zuerich&catalogId=organizations-ods"
curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" --data @piveau_organizations/org-zh-stadt-winterthur.ttl "${HUB_REPO_ENDPOINT}/resources/organization?id=stadt-winterthur&catalogId=organizations-ods"