#!/bin/bash

export LC_ALL="C"
set -euo pipefail

cd "$(dirname "$(realpath "$0")")/.."

RELEASE=0.9.1-SNAPSHOT
JAR="target/sms-scheduler-${RELEASE}.jar"

exec java -jar "$JAR" --spring.profiles.active='test' "$@"