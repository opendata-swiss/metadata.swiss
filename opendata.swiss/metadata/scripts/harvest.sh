#!/bin/sh

# Automatically export all variables
set -a
. ./.env
set +a

set -eu

curl -i -X PUT -H 'Content-Type: application/json' --data '{"status": "enabled", "id": "immediateTrigger"}' "${CONSUS_SCHEDULING_ENDPOINT}/pipes/staatskanzelei-kanton-zuerich/triggers/immediateTrigger"
curl -i -X PUT -H 'Content-Type: application/json' --data '{"status": "enabled", "id": "immediateTrigger"}' "${CONSUS_SCHEDULING_ENDPOINT}/pipes/bafu/triggers/immediateTrigger"
