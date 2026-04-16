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

echo "Uploading bulk.json to ${CONSUS_SCHEDULING_ENDPOINT}/triggers ..."
curl -i -X PUT -H 'Content-Type: application/json' $CURL_CREDS --data @"piveau_triggers/bulk.json" "${CONSUS_SCHEDULING_ENDPOINT}/triggers"
