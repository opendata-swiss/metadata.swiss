#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

curl -i -X DELETE -H "X-API-Key: $PIVEAU_HUB_API_KEY" "${HUB_REPO_ENDPOINT}/organizations/kanton-zuerich"
curl -i -X DELETE -H "X-API-Key: $PIVEAU_HUB_API_KEY" "${HUB_REPO_ENDPOINT}/organizations/zh-foo"
curl -i -X DELETE -H "X-API-Key: $PIVEAU_HUB_API_KEY" "${HUB_REPO_ENDPOINT}/organizations/zh-bar"
curl -i -X DELETE -H "X-API-Key: $PIVEAU_HUB_API_KEY" "${HUB_REPO_ENDPOINT}/organizations/staatskanzlei-kanton-zuerich"
curl -i -X DELETE -H "X-API-Key: $PIVEAU_HUB_API_KEY" "${HUB_REPO_ENDPOINT}/organizations/awel-kanton-zuerich"
curl -i -X DELETE -H "X-API-Key: $PIVEAU_HUB_API_KEY" "${HUB_REPO_ENDPOINT}/organizations/stadt-winterthur"
