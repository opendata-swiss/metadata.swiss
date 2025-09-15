# Automatically export all variables
set -a
source .env
set +a


curl -i -X PUT -H "X-API-Key: $API_KEY_HUB" -H "Content-Type: text/turtle" --data @opendata.swiss/metadata/piveau_catalogues/data-staatskanzlei-kanton-zuerich.ttl "${HUB_REPO_ENDPOINT}/catalogues/staatskanzlei-kanton-zuerich"
curl -i -X PUT -H "X-API-Key: $API_KEY_HUB" -H "Content-Type: text/turtle" --data @opendata.swiss/metadata/piveau_catalogues/data-bafu.ttl "${HUB_REPO_ENDPOINT}/catalogues/bafu"
curl -i -X PUT -H "X-API-Key: $API_KEY_HUB" -H "Content-Type: text/turtle" --data @opendata.swiss/metadata/piveau_catalogues/data-so-kt-geocat.ttl "${HUB_REPO_ENDPOINT}/catalogues/so-kt-geocat"
