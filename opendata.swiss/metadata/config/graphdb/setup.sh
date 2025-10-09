#!/bin/sh

echo "Waiting for GraphDB to be ready..."
for i in $(seq 1 30); do
  if curl -sf http://graphdb:7200/rest/repositories; then
    echo "GraphDB is up!"
    break
  fi
  echo "GraphDB not ready yet. Attempt ${i}/30"
  sleep 10
done

echo "Checking if repository exists..."
if curl -s http://graphdb:7200/rest/repositories/piveau | grep -q piveau; then
  echo "Repository 'piveau' already exists. Skipping creation."
else
  echo "Creating repository..."
  curl -X POST http://graphdb:7200/rest/repositories \
    -H "Content-Type: multipart/form-data" \
    -F config=@/config/repo-config.ttl
fi
