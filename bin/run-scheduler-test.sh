#!/bin/bash

export LC_ALL="C"
set -euo pipefail

cd "$(dirname "$(realpath "$0")")/.."

# This assumes a symlink in our base directory to the versioned war
JAR="sms-scheduler.jar"
exec java -jar "$JAR" --spring.profiles.active='test' "$@"
