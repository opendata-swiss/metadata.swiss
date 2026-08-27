#!/usr/bin/env sh

DIR="$(cd "$(dirname "$0")" && pwd)"
TEMP_FILE="$(mktemp).ttl"

riot -q --nocheck --formatted=TURTLE \
  https://inspire.ec.europa.eu/metadata-codelist/ResponsiblePartyRole/ResponsiblePartyRole.en.rdf \
  https://inspire.ec.europa.eu/metadata-codelist/ResponsiblePartyRole/ResponsiblePartyRole.de.rdf \
  https://inspire.ec.europa.eu/metadata-codelist/ResponsiblePartyRole/ResponsiblePartyRole.fr.rdf \
  https://inspire.ec.europa.eu/metadata-codelist/ResponsiblePartyRole/ResponsiblePartyRole.it.rdf \
  > "$TEMP_FILE"

arq -q --query "$DIR/relationshipRoles.rq" --data "$TEMP_FILE" > "$DIR/relationshipRoles.ttl"

rm "$TEMP_FILE"
