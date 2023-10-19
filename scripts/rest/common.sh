#!/bin/bash

export LC_ALL="C"
export USERNAME="admin"
export PASSWORD="test"
export BASE_URL="${BASE_URL:-http://localhost:8081}"

usage() { echo "Usage: ${0##*/} $*" >&2; exit 1; }

err_exit() { echo "${0##*/}: $*" >&2; exit 1; }

rest_GET() {
  curl -sD /dev/stderr \
    -u "$USERNAME:$PASSWORD" \
    -H 'Accept: application/json' \
    "${BASE_URL}${1:-}" |
  jq .
}

rest_POST() {
  curl -sD /dev/stderr -X POST \
    -u "$USERNAME:$PASSWORD" \
    -H 'Accept: application/json' \
    -H 'Content-Type: application/json' \
    -d "$2" \
    "${BASE_URL}${1:-}" |
  jq .
}

rest_DELETE() {
  curl -sD /dev/stderr -X DELETE \
    -u "$USERNAME:$PASSWORD" \
    -H 'Accept: application/json' \
    "${BASE_URL}${1:-}" |
  jq .
}

