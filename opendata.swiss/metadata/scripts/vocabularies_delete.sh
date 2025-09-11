# Automatically export all variables
set -a
source .env
set +a

HUB_REPO_ENDPOINT="https://piveau-hub-repo-ln.zazukoians.org"

curl -i -X DELETE -H "X-API-Key: $API_KEY_HUB" "${HUB_REPO_ENDPOINT}/vocabularies/ch-licenses"
