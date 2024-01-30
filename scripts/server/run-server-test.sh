#!/bin/bash

export LC_ALL="C"
set -euo pipefail

cd "$(dirname "$(realpath "$0")")/../../target"

JAR=sms-scheduler-*-SNAPSHOT.war
exec java -jar $JAR --spring.profiles.active=test "$@"
