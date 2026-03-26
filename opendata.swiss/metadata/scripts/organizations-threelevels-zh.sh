#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" \
    --data @piveau_organizations/org-zh-kanton-zuerich.ttl \
    "${HUB_REPO_ENDPOINT}/organizations/kanton-zuerich"

curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" \
    --data @piveau_organizations/org-zh-foo.ttl \
    "${HUB_REPO_ENDPOINT}/organizations/zh-foo"

curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" \
    --data @piveau_organizations/org-zh-bar.ttl \
    "${HUB_REPO_ENDPOINT}/organizations/zh-bar"

curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" \
    --data @piveau_organizations/org-zh-staatskanzlei-kanton-zuerich.ttl \
    "${HUB_REPO_ENDPOINT}/organizations/staatskanzlei-kanton-zuerich"

curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" \
    --data @piveau_organizations/org-zh-awel-kanton-zuerich.ttl \
    "${HUB_REPO_ENDPOINT}/organizations/awel-kanton-zuerich"

curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" \
    --data @piveau_organizations/org-zh-stadt-winterthur.ttl \
    "${HUB_REPO_ENDPOINT}/organizations/stadt-winterthur"
