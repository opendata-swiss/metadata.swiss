#!/bin/sh

# call it like this ...
#   ~/git/metadata.swiss/opendata.swiss/metadata$ ./scripts/publisher/dataset.sh bfs-test bfs-test-foo

# variables loaded from .env become environment variables
set -a
. ./.env
set +a

# exit immediately if a command fails (non-zero status)
# treat use of unset variables as an error and exit.
set -eu

KEYCLOAK_CLIENT_ID="bfs-publisher-apitest"
# KEYCLOAK_CLIENT_SECRET env variable required to be set

KEYCLOAK_URL="https://keycloak.zazukoians.org"

# KEYCLOAK_REALM="lindas-next"    # TEST environment: "lindas-next"
KEYCLOAK_REALM="lindas-next-int"    # INT  environment: "lindas-next-int"

KEYCLOAK_TOKEN_ENDPOINT="${KEYCLOAK_URL}/realms/${KEYCLOAK_REALM}/protocol/openid-connect/token"
KEYCLOAK_TOKEN_AUDIENCE="piveau-hub-repo"

echo "${KEYCLOAK_TOKEN_ENDPOINT}"

CATALOGUE_NAME="${1:-}"
DATASET_NAME="${2:-}"

if [ -z "$CATALOGUE_NAME" ]; then
  echo "No catalogue name provided"
elif [ -z "$DATASET_NAME" ]; then
  echo "No dataset name provided"
else

  # ── 1. Get access token ───────────────────────────────────────
  RESPONSE=$(curl -sf -X POST \
    "${KEYCLOAK_TOKEN_ENDPOINT}" \
    -H 'Content-Type: application/x-www-form-urlencoded' \
    -d "grant_type=urn:ietf:params:oauth:grant-type:uma-ticket" \
    -d "audience=${KEYCLOAK_TOKEN_AUDIENCE}" \
    -d "client_id=${KEYCLOAK_CLIENT_ID}" \
    -d "client_secret=${KEYCLOAK_CLIENT_SECRET}")

  TOKEN=$(echo "$RESPONSE" | python3 -c \
    "import sys,json; print(json.load(sys.stdin)['access_token'])")

  # ── 2. Call the API ──────────────────────────────────────────────
  URL="${HUB_REPO_ENDPOINT}/catalogues/${CATALOGUE_NAME}/datasets/origin?originalId=${DATASET_NAME}"
  echo "Uploading ${DATASET_NAME}.ttl to ${URL}"
  curl -i -X PUT \
    -H "Authorization: Bearer ${TOKEN}" \
    -H "Content-Type: text/turtle" \
    --data @"piveau_datasets/${DATASET_NAME}.ttl" \
    "${URL}"

fi
