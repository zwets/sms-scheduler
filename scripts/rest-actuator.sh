#!/bin/bash

export LC_ALL="C"
set -euo pipefail

#xdg-open http://localhost:8081/actuator/flowable
curl -s http://localhost:8081/actuator/flowable | jq .
