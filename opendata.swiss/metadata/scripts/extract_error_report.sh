#!/bin/sh

# docker logs -f metadata-piveau-consus-filter-1 | ./scripts/extract_error_report.sh

set -eu

CURRENT_DATE=$(date +%Y-%m-%d)

# get start date from input argument or use current date if not provided
START_DATE="${1:-$CURRENT_DATE}"

# Extract JSON payload from logged errors for date $START_DATE
sed -n "/^$START_DATE/,\$ { /Validation error/ { s/.*\({.*}\).*/\1/p; } }"

