#!/bin/bash

export LC_ALL="C"
set -euo pipefail
cd "$(dirname "$0")/.."

# Note: when started, create topics schedule-sms, send-sms, and sms-status
exec /opt/kafka/bin/start-kafka.sh sms-broker
