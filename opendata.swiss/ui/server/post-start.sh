#!/bin/sh

# Generate JWT for GitHub App authentication
# Requires: GITHUB_APP_ID, GITHUB_INSTALLATION_ID, GITHUB_APP_PRIVATE_KEY (PEM string or file)

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

# Generate installation access token using Node.js
GITHUB_TOKEN=$(node "$SCRIPT_DIR"/get-github-app-token.js)

case "$GITHUB_REPO" in
  opendata-swiss-cms-content-int)
    ENVIRONMENT="INT" ;;
  opendata-swiss-cms-content)
    ENVIRONMENT="PROD" ;;
  *)
    ENVIRONMENT="TEST" ;;
esac
curl -X POST \
  -H "Authorization: Bearer $GITHUB_TOKEN" \
  -H "Accept: application/vnd.github+json" \
  https://api.github.com/repos/opendata-swiss/metadata.swiss/actions/workflows/script-manual-trigger.yaml/dispatches \
  -d "{\"ref\":\"main\",\"inputs\":{\"environment\":\"$ENVIRONMENT\",\"action\":\"HARVEST_SHOWCASES\"}}"
