#!/bin/bash

export LC_ALL="C"
set -euo pipefail

# Defaults

export BROKER="${BROKER:-localhost:9192}"
export TOPIC="${TOPIC:-schedule-sms}"
export PARTITION="${PARTITION:-0}"
export EVENTKEY="${EVENTKEY:-}"
export DUMPINPUT=${DUMPINPUT:-}
export VERBOSE=${VERBOSE:-}

export USAGE="${USAGE:-}

  COMMON OPTIONS
   -b,--broker=BROKER  Set the broker host:port [$BROKER]
   -t,--topic=TOPIC    Set the topic [$TOPIC]
   -p,--partition=P    Set the partition [$PARTITION]
   -k,--key=KEY        Set the event key [$EVENTKEY]
   -d,--dump-input     Write request message to stderr
   -v,--verbose        Display diagnostic output
   -h,--help           Display usage information
"
# General functions

emit() { [ ! $VERBOSE ] || echo "${0##*/}: $*" >&2; }
usage_exit() { echo "Usage: ${0##*/} [-dvh] [-b BROKER ] [-t TOPIC] [-p PARTITION] [-k KEY] ${USAGE}" >&2; exit ${1:-1}; }
err_exit() { echo "${0##*/}: $*" >&2; exit 1; }

# Check common options

TEMP=$(getopt -n "${0##*/}" -o 'hvdb:t:p:k:' -l 'help,verbose,dump-event,dump-input,broker,topic,partition,key' -- "$@" || exit 1)
eval set -- "$TEMP"
unset TEMP

# Parse common options

while true; do
    case "$1" in
        -b|--b*)      BROKER="$2";    shift 2 ;;
        -t|--t*)      TOPIC="$2";     shift 2 ;;
        -p|--p*)      PARTITION="$2"; shift 2 ;;
        -k|--k*)      EVENTKEY="$2";  shift 2 ;;
        -v|--v*)      VERBOSE=1;      shift ;;
        -d|--d*)      DUMPINPUT=1;    shift ;;
        -h|--hel*)    usage_exit 0  ;;
        --) shift; break ;;
        *)  err_exit "lpt1 on fire!" ;;
    esac
done

# Diagnostic output

emit "BROKER    = $BROKER"
emit "TOPIC     = $TOPIC"
emit "PARTITION = $PARTITION"
[ ! $VERBOSE ] || echo >&2

# Helper functions

dump_input() {
    [ $DUMPINPUT ] && jq . "$@" | tee /dev/stderr || cat "$@"
}

# Kafka (kcat) functions

kcat_send() {
    F="${1:--}" && [ "$F" = '-' ] || [ -f "$F" ] || err_exit "no such file: $F"
    dump_input "$F" | tr '\n' ' ' | kcat -P -b "$BROKER" -t "$TOPIC" -p "$PARTITION" ${EVENTKEY:+-k} $EVENTKEY -c 1 ${VERBOSE:+-v}
}

# vim: sts=4:sw=4:ai:si:et
