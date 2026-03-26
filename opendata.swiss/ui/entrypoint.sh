set -e

git clone --depth 1 "https://${GITHUB_TOKEN}@github.com/${GITHUB_OWNER}/${GITHUB_REPO}.git" content

npm run build

node .output/server/index.mjs
