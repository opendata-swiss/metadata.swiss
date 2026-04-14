if [ -z "$GITHUB_REF" ]; then
  echo "GITHUB_REF not set, defaulting to refs/heads/main"
  GITHUB_REF="refs/heads/main"
fi
set -e

git clone --depth 1 "https://github.com/${GITHUB_OWNER}/${GITHUB_REPO}.git" --no-checkout content
cd content
git fetch --depth 1 origin "${GITHUB_REF}"
git checkout FETCH_HEAD
cd ..

npm run build

node .output/server/index.mjs
