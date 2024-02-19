#!/bin/bash

export LC_ALL="C"
set -euo pipefail

cd "$(dirname "$(realpath "$0")")/.."

VERSION=1.0.1
JAR="target/sms-scheduler-${VERSION}.war"

exec java -jar "$JAR" --spring.profiles.active='test' "$@"
