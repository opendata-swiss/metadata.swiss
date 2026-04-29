#!/bin/sh

# Wait for listmonk to be ready and authorized
echo "Waiting for listmonk API to be ready and authorized..."

# Use LISTMONK_ADMIN_API_USER
if [ -z "${LISTMONK_ADMIN_API_USER}" ]; then
  echo "Error: LISTMONK_ADMIN_API_USER is not set."
  exit 1
fi

if [ -z "$AUTH_PASS" ]; then
  echo "Waiting for install logs to extract token for ${LISTMONK_ADMIN_API_USER}..."
  # Wait for the log file and the token to appear
  MAX_RETRIES=30
  RETRY_COUNT=0
  while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if [ -f "/uploads/install.log" ]; then
      TOKEN=$(grep "export LISTMONK_ADMIN_API_TOKEN=" /uploads/install.log | cut -d'"' -f2)
      if [ -n "$TOKEN" ]; then
        echo "Successfully extracted Admin API token."
        AUTH_USER="${LISTMONK_ADMIN_API_USER}"
        AUTH_PASS="${TOKEN}"
        break
      fi
    fi
    echo "Waiting for install log and token... ($RETRY_COUNT/$MAX_RETRIES)"
    sleep 2
    RETRY_COUNT=$((RETRY_COUNT + 1))
  done
fi

if [ -z "$AUTH_PASS" ]; then
  echo "Error: Token could not be extracted from /uploads/install.log within timeout."
  exit 1
fi

echo "Using Admin API credentials: ${AUTH_USER}:${TOKEN}"

until curl -s -u "${AUTH_USER}:${AUTH_PASS}" http://listmonk-app:9000/api/templates > /dev/null; do
  echo "Still waiting for API to be authorized..."
  sleep 2
done

LISTMONK_URL="http://listmonk-app:9000/api"

# Common jq filter to extract items from various Listmonk API response formats
# 1. Direct array: [...]
# 2. Wrapped in .data: {"data": [...]}
# 3. Nested in .data.results: {"data": {"results": [...]}}
JQ_EXTRACT_ITEMS='if type == "array" then .[] elif .data | type == "array" then .data[] elif .data.results | type == "array" then .data.results[] else empty end'

# Function to import a template
import_template() {
  local file=$1
  local name=$(basename "$file" .html)
  echo "Importing template: $name"

  content=$(cat "$file")

  # Check if template already exists
  EXISTING_ID=$(curl -s -u "${AUTH_USER}:${AUTH_PASS}" "${LISTMONK_URL}/templates" | jq -r "$JQ_EXTRACT_ITEMS | select(.name == \"$name\") | .id")

  if [ -z "$EXISTING_ID" ]; then
    echo "Creating template $name..."
    EXISTING_ID=$(curl -s -u "${AUTH_USER}:${AUTH_PASS}" -X POST "${LISTMONK_URL}/templates" \
      -H "Content-Type: application/json" \
      -d "$(jq -n --arg name "$name" --arg body "$content" '{name: $name, body: $body, type: "tx", subject: $name}')" | jq -r '.data.id // empty')
  else
    echo "Template $name already exists (ID: $EXISTING_ID), updating..."
    curl -s -u "${AUTH_USER}:${AUTH_PASS}" -X PUT "${LISTMONK_URL}/templates/$EXISTING_ID" \
      -H "Content-Type: application/json" \
      -d "$(jq -n --arg name "$name" --arg body "$content" '{name: $name, body: $body, type: "tx", subject: $name}')"
  fi

  # Always set the first template we find/create as the default to ensure we can delete ID=1 later
  if [ -n "$EXISTING_ID" ] && [ "$EXISTING_ID" != "null" ]; then
    echo "Setting template $name (ID: $EXISTING_ID) as default..."
    curl -s -u "${AUTH_USER}:${AUTH_PASS}" -X PUT "${LISTMONK_URL}/templates/$EXISTING_ID/default" > /dev/null
  fi
}

# Delete default templates (except those we just imported)
delete_default_templates() {
  echo "Checking for default templates to remove..."
  # Get all templates
  TEMPLATES_JSON=$(curl -s -u "${AUTH_USER}:${AUTH_PASS}" "${LISTMONK_URL}/templates")

  # Default templates often have names like "Default template" or are the ones created by listmonk on install.
  # We'll target "Default template" and any template that isn't in our local templates folder.

  echo "$TEMPLATES_JSON" | jq -c "$JQ_EXTRACT_ITEMS" | while read -r template; do
    [ -z "$template" ] && continue
    T_NAME=$(echo "$template" | jq -r '.name // empty')
    T_ID=$(echo "$template" | jq -r '.id // empty')
    T_IS_DEFAULT=$(echo "$template" | jq -r '.is_default // false')

    # Skip if ID or Name is empty
    [ -z "$T_ID" ] || [ "$T_ID" = "null" ] && continue
    [ -z "$T_NAME" ] || [ "$T_NAME" = "null" ] && continue

    # Keep if it matches one of our local files
    KEEP=false
    if [ -d "/init/templates" ]; then
      for f in /init/templates/*.html; do
        [ -e "$f" ] || continue
        L_NAME=$(basename "$f" .html)
        if [ "$T_NAME" = "$L_NAME" ]; then
          KEEP=true
          break
        fi
      done
    fi

    # Delete if it's named "Default template" or ID=1, or any template that isn't ours and isn't marked as system default
    if [ "$T_NAME" = "Default template" ] || [ "$T_ID" = "1" ] || [ "$KEEP" = "false" ]; then
       echo "Deleting template: $T_NAME (ID: $T_ID)"
       curl -s -u "${AUTH_USER}:${AUTH_PASS}" -X DELETE "${LISTMONK_URL}/templates/$T_ID"
    fi
  done
}

# Delete default list
delete_default_list() {
  echo "Checking for default list to remove..."
  LISTS_JSON=$(curl -s -u "${AUTH_USER}:${AUTH_PASS}" "${LISTMONK_URL}/lists")

  # Handle potential array response directly or within .data or nested .data.results
  echo "$LISTS_JSON" | jq -c "$JQ_EXTRACT_ITEMS" | while read -r list; do
    [ -z "$list" ] && continue
    L_NAME=$(echo "$list" | jq -r '.name // empty')
    L_ID=$(echo "$list" | jq -r '.id // empty')

    # Skip if ID or Name is empty
    [ -z "$L_ID" ] || [ "$L_ID" = "null" ] && continue
    [ -z "$L_NAME" ] || [ "$L_NAME" = "null" ] && continue

    if [ "$L_NAME" = "Default list" ] || [ "$L_ID" = "2" ]; then
      echo "Deleting list: $L_NAME (ID: $L_ID)"
      curl -s -u "${AUTH_USER}:${AUTH_PASS}" -X DELETE "${LISTMONK_URL}/lists/$L_ID"
    fi
  done
}

# Delete test campaigns
delete_test_campaigns() {
  echo "Checking for test campaigns to remove..."
  CAMPAIGNS_JSON=$(curl -s -u "${AUTH_USER}:${AUTH_PASS}" "${LISTMONK_URL}/campaigns")

  echo "$CAMPAIGNS_JSON" | jq -c "$JQ_EXTRACT_ITEMS" | while read -r campaign; do
    [ -z "$campaign" ] && continue
    C_NAME=$(echo "$campaign" | jq -r '.name // empty')
    C_ID=$(echo "$campaign" | jq -r '.id // empty')
    C_STATUS=$(echo "$campaign" | jq -r '.status // empty')

    # Skip if ID or Name is empty
    [ -z "$C_ID" ] || [ "$C_ID" = "null" ] && continue
    [ -z "$C_NAME" ] || [ "$C_NAME" = "null" ] && continue

    # Delete if it's named "Test campaign" or similar
    if [ "$C_NAME" = "Test campaign" ] || [ "$C_NAME" = "My first campaign" ]; then
      echo "Deleting campaign: $C_NAME (ID: $C_ID)"
      curl -s -u "${AUTH_USER}:${AUTH_PASS}" -X DELETE "${LISTMONK_URL}/campaigns/$C_ID"
    fi
  done
}

# Delete default subscribers (e.g. the ones added to list ID 1)
delete_default_subscribers() {
  echo "Checking for default subscribers to remove..."
  # Listmonk usually doesn't have a "delete all subscribers" but we can delete subscribers from the default list
  # or list all subscribers and delete them.
  # First, find the ID of "Default list" if it still exists (or we just deleted it, but we might need its ID)

  # Actually, Listmonk has /api/subscribers
  SUBS_JSON=$(curl -s -u "${AUTH_USER}:${AUTH_PASS}" "${LISTMONK_URL}/subscribers")

  echo "$SUBS_JSON" | jq -c "$JQ_EXTRACT_ITEMS" | while read -r sub; do
    [ -z "$sub" ] && continue
    S_ID=$(echo "$sub" | jq -r '.id // empty')
    S_EMAIL=$(echo "$sub" | jq -r '.email // empty')

    # Skip if ID or Email is empty
    [ -z "$S_ID" ] || [ "$S_ID" = "null" ] && continue
    [ -z "$S_EMAIL" ] || [ "$S_EMAIL" = "null" ] && continue

    # Delete if it's a known demo subscriber or if it looks like a demo one
    if [ "$S_EMAIL" = "subscriber@example.com" ] || [ "$S_EMAIL" = "admin@listmonk.app" ] || echo "$S_EMAIL" | grep -q "@example.com"; then
       echo "Deleting subscriber: $S_EMAIL (ID: $S_ID)"
       curl -s -u "${AUTH_USER}:${AUTH_PASS}" -X DELETE "${LISTMONK_URL}/subscribers/$S_ID"
    fi
  done
}

# Function to configure SMTP
configure_smtp() {
  echo "Configuring SMTP to use Mailpit..."

  # Fetch current settings
  SETTINGS_JSON=$(curl -s -u "${AUTH_USER}:${AUTH_PASS}" "${LISTMONK_URL}/settings")

  if [ -z "$SETTINGS_JSON" ] || [ "$SETTINGS_JSON" = "null" ]; then
    echo "Error: Could not fetch current settings."
    return
  fi

  # Update the SMTP configuration in the settings JSON
  # We need to extract the .data object because Listmonk returns settings wrapped in .data
  # and the PUT /api/settings expects the object that contains the keys to update.

  # Mailpit configuration
  MAILPIT_SMTP='{
    "uuid": "default",
    "name": "Mailpit",
    "host": "mailpit",
    "port": 1025,
    "auth_protocol": "none",
    "hello_hostname": "localhost",
    "max_conns": 10,
    "idle_timeout": "15s",
    "wait_timeout": "5s",
    "enabled": true,
    "email_headers": []
  }'

  UPDATED_SETTINGS=$(echo "$SETTINGS_JSON" | jq -c --argjson mailpit "$MAILPIT_SMTP" '.data | .smtp = [$mailpit]')

  if [ -z "$UPDATED_SETTINGS" ] || [ "$UPDATED_SETTINGS" = "null" ]; then
    echo "Error: Failed to process settings JSON with jq."
    return
  fi

  echo "Uploading updated SMTP settings..."
  curl -s -u "${AUTH_USER}:${AUTH_PASS}" -X PUT "${LISTMONK_URL}/settings" \
    -H "Content-Type: application/json" \
    -d "$UPDATED_SETTINGS" > /dev/null
}

# Import templates
if [ -d "/init/templates" ]; then
  for f in /init/templates/*.html; do
    [ -e "$f" ] || continue
    import_template "$f"
  done
fi

# Cleanup default Listmonk resources
configure_smtp
delete_default_templates
delete_default_list
delete_test_campaigns
delete_default_subscribers

echo "Initialization complete."
