#!/bin/sh

# grep "image" and "host" entries from the docker compose YAML file, eg:
# - image: ghcr.io/opendata-swiss/ods-consus-importing-csw:main-24456586647 -> ods-consus-importing-csw
# - traefik.http.routers.piveau-hub-repo.rule=Host(`piveau-hub-repo.test.ods.zazukoians.org`) -> piveau-hub-repo.test.ods.zazukoians.org

SCRIPT_DIR=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
COMPOSE_FILE="$SCRIPT_DIR/compose.yaml"

awk '
	{
		# Extract image names and normalize to the last path segment without tag/digest.
		if (match($0, /image:[[:space:]]*[^[:space:]]+/)) {
			image = substr($0, RSTART, RLENGTH)
			sub(/^image:[[:space:]]*/, "", image)
			gsub(/"/, "", image)
			sub(/@.*/, "", image)

			n = split(image, parts, "/")
			image_name = parts[n]
			sub(/:.*/, "", image_name)
			print image_name
		}

		# Extract every Host(`...`) occurrence from the line.
		rest = $0
		while (match(rest, /Host\(`[^`]+`\)/)) {
			host = substr(rest, RSTART + 6, RLENGTH - 8)
			print host
			rest = substr(rest, RSTART + RLENGTH)
		}
	}
' "$COMPOSE_FILE" | sort -u