#!/bin/bash

export LC_ALL="C"
set -euo pipefail
cd "$(dirname "$0")/.."

JAR=sms-scheduler-0.0.1-SNAPSHOT.war
# Note use arguments to override application.properties
# or environment properties: SERVER_PORT=8081
exec java -jar "$JAR" --server.port=8081 "$@"
