if [ -z "$GITHUB_REF" ]; then
  echo "GITHUB_REF not set, defaulting to refs/heads/main"
  GITHUB_REF="refs/heads/main"
fi
set -e

# Only clone if content directory does not exist
if [ ! -d "content" ]; then
  git clone "https://github.com/${GITHUB_OWNER}/${GITHUB_CMS_REPO}.git" --no-checkout content
  cd content
  git fetch origin "${GITHUB_REF}"
  git checkout FETCH_HEAD
  cd ..
fi

npm run build

node .output/server/index.mjs
