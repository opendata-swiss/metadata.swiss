# Automatically export all variables
set -a
source .env
set +a


curl -i -X DELETE -H "X-API-Key: $API_KEY_HUB" "${HUB_REPO_ENDPOINT}/catalogues/staatskanzlei-kanton-zuerich"
curl -i -X DELETE -H "X-API-Key: $API_KEY_HUB" "${HUB_REPO_ENDPOINT}/catalogues/bafu"
curl -i -X DELETE -H "X-API-Key: $API_KEY_HUB" "${HUB_REPO_ENDPOINT}/catalogues/stadt-winterthur-geocat"
