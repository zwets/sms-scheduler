#!/bin/bash

export LC_ALL="C"
set -euo pipefail
cd "$(dirname "$0")/.."

for TOPIC in schedule-sms send-sms sms-status; do
    /opt/kafka/bin/list-topics.sh | fgrep -q "$TOPIC" \
    && { echo "${0##*/}: topic exists: $TOPIC" >&2; } \
    || { echo "${0##*/}: create topic: $TOPIC" >&2; \
         /opt/kafka/bin/create-topic.sh "$TOPIC"; }
done
