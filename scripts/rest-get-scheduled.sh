#!/bin/bash

export LC_ALL="C"
set -euo pipefail

echo "FOR CLIENT"
curl -s http://localhost:8081/scheduled?client=CLIENT | jq .

echo "WITHOUT CLIENT ARGUMENT"
curl -s http://localhost:8081/scheduled | jq .

echo "FOR CLIENT and WRONG TARGET"
curl -s http://localhost:8081/scheduled?client=CLIENT&target=UNKNOWN | jq .

echo
