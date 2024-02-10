#!/bin/bash

export LC_ALL="C"
set -euo pipefail

export BROKER="${BROKER:-${DEFAULT_BROKER:-localhost:9192}}"
export TOPIC="${TOPIC:-${DEFAULT_TOPIC:-schedule-sms}}"
export PARTITION="${PARTITION:-}"
export EVENTKEY="${EVENTKEY:-}"
export GROUPID="${GROUPID:-}"
export OFFSET="${OFFSET:-}"
export DUMP=${DUMP:-}
export RAWOUT=${RAWOUT:-}
export VERBOSE=${VERBOSE:-}
export NOT_REALLY=${NOT_REALLY:-}

export USAGE="${USAGE:-}

  COMMON OPTIONS
   -b,--broker=BROKER  Set the broker host:port [$BROKER]
   -t,--topic=TOPIC    Set the topic [$TOPIC]
   -p,--partition=PARTITION  Set the partition [$PARTITION]
   -k,--key=EVENTKEY   Set the event key [$EVENTKEY]
   -g,--group=GROUPID  Group ID of client [$GROUPID]
   -o,--offset=OFFSET  Start reading at offset [$OFFSET]
   -a,--all            Read the topic from the beginning
   -d,--dump           Write input to stderr / full output
   -r,--raw            Do not filter output through JQ
   -n,--dry-run        Do everything except the real thing
   -v,--verbose        Display diagnostic output
   -h,--help           Display usage information

  All CAPITALISED options can also be passed as environment vars
"
# General functions

emit() { [ ! $VERBOSE ] || echo "${0##*/}: $*" >&2; }
usage_exit() { echo "Usage: ${0##*/} [-adrnvh] [-b BROKER ] [-t TOPIC] [-p PARTITION] [-k KEY] [-g GROUP] [-o OFFSET] ${USAGE}" >&2; exit ${1:-1}; }
err_exit() { echo "${0##*/}: $*" >&2; exit 1; }

# Check common options

TEMP=$(getopt -n "${0##*/}" -o 'hvdarnb:t:p:k:g:o:' -l 'help,verbose,dump,all,raw,dry-run,broker,topic,partition,key,group,offset' -- "$@" || exit 1)
eval set -- "$TEMP"
unset TEMP

# Parse common options

while true; do
    case "$1" in
        -b|--b*)    BROKER="$2";          shift 2 ;;
        -t|--t*)    TOPIC="$2";           shift 2 ;;
        -p|--p*)    PARTITION="$2";       shift 2 ;;
        -g|--g*)    GROUPID="$2";         shift 2 ;;
        -o|--o*)    OFFSET="$2";          shift 2 ;;
        -k|--k*)    EVENTKEY="$2";        shift 2 ;;
        -a|--a*)    OFFSET='beginning';   shift ;;
        -v|--v*)    VERBOSE=1;            shift ;;
        -d|--du*)   DUMP=1;               shift ;;
        -r|--r*)    RAWOUT=1;             shift ;;
        -n|--dr*)   NOT_REALLY=1; DUMP=1; shift ;;
        -h|--h*)    usage_exit 0  ;;
        --) shift; break ;;
        *)  err_exit "lpt1 on fire!" ;;
    esac
done

# Diagnostic output

dump_diags() {
    emit "BROKER    = $BROKER"
    emit "TOPIC     = $TOPIC"
    [ -z "$GROUPID" ]   || emit "GROUP     = $GROUPID"
    [ -z "$PARTITION" ] || emit "PARTITION = $PARTITION"
    [ -z "$OFFSET" ]    || emit "OFFSET    = $OFFSET"
    [ -z "$EVENTKEY" ]  || emit "EVENTKEY  = $EVENTKEY"
}

# Look for necessary functions

KCAT="$(which kcat 2>/dev/null)" || KCAT="$(which kafkacat 2>/dev/null)" || err_exit "command not found: kcat or kafkacat (do: apt install kcat || apt install kafkacat)"
JQ="$(which jq 2>/dev/null)" || err_exit "command not found: jq (do: apt install jq)"

# Functions to generate defaults

sms_client() {
    [ -n "${SMS_CLIENT:-}" ] ||
    declare -g SMS_CLIENT="$(realpath -e "$(dirname "$(realpath "$0")")/../../../sms-client/bin/sms-client" 2>/dev/null)" ||
    declare -g SMS_CLIENT="$(realpath -e "/opt/sms-client/bin/sms-client")" ||
        err_exit "sms-client not found; set its path in lib/defaults.sh"
    "$SMS_CLIENT" "$@"
}

encrock_phone() {
    emit "encrock phone number: $1"
    sms_client encrock "$1"
}

pubkey_encrypt() {
    emit "encrypt payload"
    sms_client encrypt "$1"
}

rewrite_schedule() {  # rewrite the +SEC1[+SEC2] to proper schedule
    [ "${1#+}" == "${1:-}" ] && return || true
    emit "rewrite schedule: $1"
    declare line="${1#+}" 
    declare t1="${line%+*}"
    declare -i now=$(date +%s)
    [ "$line" = "$t1" ] && declare -i s1=${t1:-0} s2=600 || declare -i s1=${t1:-0} s2=${line#*+}
    echo $(date -Is --date=@$((now + s1)))/$(date -Is --date=@$((now + s1 + s2)))
}

# Input and output filters

dump_input() {
    [ $DUMP ] && "$JQ" . "$@" | tee /dev/stderr || cat "$@"
}

format_out() {
    [ $RAWOUT ] && cat || "$JQ" .
}

# Kafka (kcat) functions

kcat_send() {
    F="${1:--}" && [ "$F" = '-' ] || [ -f "$F" ] || err_exit "no such file: $F"
    dump_input "$F" | tr '\n' ' ' | if [ $NOT_REALLY ]; then cat >/dev/null; else
        "$KCAT" -P -b "$BROKER" -t "$TOPIC" ${PARTITION:+-p $PARTITION} ${GROUPID:+-G $GROUPID} ${EVENTKEY:+-k} $EVENTKEY -c 1 ${VERBOSE:+-v} "$@"
    fi
}

kcat_listen() {
    "$KCAT" -C -u -b "$BROKER" -t "$TOPIC" ${PARTITION:+-p $PARTITION} ${GROUPID:+-G $GROUPID} ${DUMP:+-J} ${VERBOSE:+-v} ${OFFSET:+ -o $OFFSET} "$@" | format_out
}

# vim: sts=4:sw=4:ai:si:et
