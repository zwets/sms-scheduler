#!/bin/bash

export LC_ALL="C"
set -euo pipefail

curl -s http://localhost:8081/actuator/flowable | jq .
#xdg-open http://localhost:8081/actuator/flowable
