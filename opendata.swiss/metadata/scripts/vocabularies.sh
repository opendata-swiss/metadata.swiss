# Automatically export all variables
set -a
source .env
set +a

HUB_REPO_ENDPOINT="https://piveau-hub-repo-ln.zazukoians.org"

curl -i -X PUT -H "X-API-Key: $API_KEY_HUB" -H "Content-Type: text/turtle" --data @piveau_vocabularies/licenses-20240716.ttl "${HUB_REPO_ENDPOINT}/vocabularies/ch-licenses"

