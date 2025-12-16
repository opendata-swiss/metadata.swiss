#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

# Set CURL_CREDS if username and password are defined
if [ -n "${CONSUS_SCHEDULING_USERNAME:-}" ] && [ -n "${CONSUS_SCHEDULING_PASSWORD:-}" ]; then
  CURL_CREDS="-u ${CONSUS_SCHEDULING_USERNAME}:${CONSUS_SCHEDULING_PASSWORD}"
else
  CURL_CREDS=""
fi

# We disable the trigger for Staatskanzlei Kanton Zürich as their endpoint seems to be broken
# curl -i -X PUT -H 'Content-Type: application/json' $CURL_CREDS --data '{"status": "enabled", "id": "immediateTrigger"}' "${CONSUS_SCHEDULING_ENDPOINT}/pipes/staatskanzelei-kanton-zuerich/triggers/immediateTrigger"
curl -i -X PUT -H 'Content-Type: application/json' $CURL_CREDS --data '{"status": "enabled", "id": "immediateTrigger"}' "${CONSUS_SCHEDULING_ENDPOINT}/pipes/bafu/triggers/immediateTrigger"
