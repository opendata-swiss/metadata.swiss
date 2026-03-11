#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

echo cat ...
cat piveau_organizations/org-foo.ttl
echo 

echo PUT ...
curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" \
    --data @piveau_organizations/org-foo.ttl \
    "${HUB_REPO_ENDPOINT}/organizations/org-foo"

echo GET ...
curl -i -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Accept: text/turtle" \
    "${HUB_REPO_ENDPOINT}/organizations/org-foo"

# --------------
echo cat ...
cat piveau_organizations/org-foo-2.ttl
echo 

echo PUT ...
curl -i -X PUT -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Content-Type: text/turtle" \
    --data @piveau_organizations/org-foo-2.ttl \
    "${HUB_REPO_ENDPOINT}/organizations/org-foo-2"

echo GET ...
curl -i -H "X-API-Key: $PIVEAU_HUB_API_KEY" -H "Accept: text/turtle" \
    "${HUB_REPO_ENDPOINT}/organizations/org-foo-2"