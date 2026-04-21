if [ -z "$GITHUB_REF" ]; then
  echo "GITHUB_REF not set, defaulting to refs/heads/main"
  GITHUB_REF="refs/heads/main"
fi
set -e

# Only clone if content directory does not exist
if [ ! -d "content" ]; then
  git clone --depth 1 "https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}.git" --no-checkout content
  cd content
  git fetch --depth 1 origin "${GITHUB_REF}"
  git checkout FETCH_HEAD
  cd ..
fi

npm run build

# Start the server in the background
node .output/server/index.mjs &
SERVER_PID=$!

# Wait for the server to be up
MAX_TRIES=60
TRIES=0
SERVER_UP=1
while ! curl -sf http://localhost/ > /dev/null; do
  TRIES=$((TRIES+1))
  if [ $TRIES -ge $MAX_TRIES ]; then
    echo "Server did not start in time. Showcases will not be harvested."
    SERVER_UP=0
    break
  fi
  echo "Waiting for server to start... ($TRIES/$MAX_TRIES)"
  sleep 1
done

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

if [ $SERVER_UP -eq 1 ]; then
  echo "Server is up. Harvesting showcases..."
  "$SCRIPT_DIR"/post-start.sh
else
  echo "Skipping showcase harvesting."
fi

wait $SERVER_PID
