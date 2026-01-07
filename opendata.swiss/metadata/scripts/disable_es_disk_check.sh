#!/bin/sh

set -eu

# Configure Elasticsearch to skip low disk space checks
curl -X PUT "http://localhost:9200/_cluster/settings" \
  -H "Content-Type: application/json" \
  -d '{
    "persistent": {
      "cluster.routing.allocation.disk.threshold_enabled": false
    }
  }'

# To check the current settings, you can use:
curl -X GET "http://localhost:9200/_cluster/settings?pretty"
