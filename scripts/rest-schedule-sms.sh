#!/bin/bash

export LC_ALL="C"
set -euo pipefail

#URL='http://admin:test@localhost:8081/schedule'
URL='http://localhost:8081/schedule'

NOW=$(date +%s)
JSON="$(printf '{
  "clientId": "CLIENT",
  "targetId": "TARGET",
  "uniqueId": "UNIQUE",
  "schedule": "%d %d %d %d",
  "payload":  "PAYLOAD"
}' $NOW $((NOW + 10)) $((NOW + 20)) $((NOW + 30)))"

# Echo the request

echo "REQUEST:"
echo "$JSON" | jq .
echo

# And do the thing

echo "RESPONSE:"
curl -sX POST \
  -H 'Accept: application/json' \
  -H 'Content-Type: application/json' \
  -d "$JSON" \
  "$URL" | jq .
