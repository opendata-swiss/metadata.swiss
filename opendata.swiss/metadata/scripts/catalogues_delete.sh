# Automatically export all variables
set -a
source .env
set +a

HUB_REPO_ENDPOINT="https://piveau-hub-repo-ln.zazukoians.org"

curl -i -X DELETE -H "X-API-Key: $API_KEY_HUB" -H "${HUB_REPO_ENDPOINT}/catalogues/staatskanzlei-kanton-zuerich"
curl -i -X DELETE -H "X-API-Key: $API_KEY_HUB" -H "${HUB_REPO_ENDPOINT}/catalogues/bafu"
curl -i -X DELETE -H "X-API-Key: $API_KEY_HUB" -H "${HUB_REPO_ENDPOINT}/catalogues/so-kt-geocat"
