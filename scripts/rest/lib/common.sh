#!/bin/bash

export LC_ALL="C"
set -euo pipefail

# Defaults

export VERBOSE=${VERBOSE:-}
export HEADERS=${HEADERS:-}
export TEXTONLY=${TEXTONLY:-}
export DUMPINPUT=${DUMPINPUT:-}
export BASEURL="${BASEURL:-http://localhost:8081}"
export CONTEXT="${CONTEXT:-}"
# UANDP is a convenience for testing with accounts with UNAME==PWORD
export UNAME="${UNAME:-${UANDP:-admin}}"
export PWORD="${PWORD:-${UANDP:-test}}"

export USAGE="${USAGE:-}

  COMMON OPTIONS
   -u,--username=USER  Set the username for authentication [$UNAME]
   -p,--password=PASS  Set the password for authentication [$PWORD]
   -b,--base-url=URL   Set the base URL [$BASEURL]
   -c,--context=PATH   Set the request context [$CONTEXT]
   -t,--text-only      Do not use jq to pretty print JSON
   -x,--headers        Write response headers to stderr
   -d,--dump-input     Write request entity to stderr (PUT, POST)
   -v,--verbose        Display diagnostic output
   -h,--help           Display usage information
"
# General functions

emit() { [ ! $VERBOSE ] || echo "${0##*/}: $*" >&2; }
usage_exit() { echo "Usage: ${0##*/} [-txdvh] [-u USER] [-p PASS] [-b URL] [-c PATH] ${USAGE}" >&2; exit ${1:-1}; }
err_exit() { echo "${0##*/}: $*" >&2; exit 1; }

# Check common options

TEMP=$(getopt -n "${0##*/}" -o 'hvxdtu:p:b:c:' -l 'help,verbose,headers,dump-input,text-only,username:,password:,base-url:,context:' -- "$@" || exit 1)
eval set -- "$TEMP"
unset TEMP

# Parse common options

while true; do
    case "$1" in
        -h|--hel*) usage_exit 0  ;;
        -v|--v*)   VERBOSE=1;    shift ;;
        -x|--hea*) HEADERS=1;    shift ;;
        -t|--t*)   TEXTONLY=1;   shift ;;
        -d|--d*)   DUMPINPUT=1;  shift ;;
        -b|--b*)   BASEURL="$2"; shift 2 ;;
        -c|--c*)   CONTEXT="$2"; shift 2 ;;
        -u|--u*)   UNAME="$2";   shift 2 ;;
        -p|--p*)   PWORD="$2";   shift 2 ;;
        --) shift; break ;;
        *)  err_exit "lpt1 on fire!" ;;
    esac
done

# Diagnostic output

emit "BASEURL = $BASEURL"
emit "CONTEXT = $CONTEXT"
emit "UNAME   = $UNAME"
emit "PWORD   = $PWORD"
[ ! $VERBOSE ] || echo >&2

# REST functions

jq_pipe() {
    [ $TEXTONLY ] && cat "$@" || jq . "$@"
}

dump_input() {
    [ $DUMPINPUT ] && jq_pipe "$@" | tee /dev/stderr || cat "$@"
}

# Base REST call
rest_call() {
    curl -sS --fail${VERBOSE:+-with-body} ${HEADERS:+--dump-header} ${HEADERS:+/dev/stderr} \
        -u "$UNAME:$PWORD" \
        -H 'Accept: application/json' \
        -H 'Accept: text/plain' \
        "$@" |
    jq_pipe
}

# Perform verb $1 on $2 of data from file $3 or stdin
rest_with_data() {
    F="${3:--}" && [ "$F" = '-' ] || [ -f "$F" ] || err_exit "no such file: $F"
    dump_input "$F" | rest_call -X $1 -H 'Content-Type: application/json' --data "@-" "${BASEURL}${CONTEXT}${2:-}"
}

# Perform verb $1 on $2 as multipart/form-data from file $3 or stdin
rest_with_formdata() {
    F="${3:--}" && [ "$F" = '-' ] || [ -f "$F" ] || err_exit "no such file: $F"
    dump_input "$F" | rest_call -X $1 --form "body=@-" "${BASEURL}${CONTEXT}${2:-}"
}

# Perform a GET on $1
rest_GET() {
    rest_call "${BASEURL}${CONTEXT}${1:-}"
}

# Perform a DELETE on $1
rest_DELETE() {
    rest_call -X DELETE "${BASEURL}${CONTEXT}${1:-}"
}

# Perform a PUT on $1 of data from file $2 or stdin
rest_PUT() {
    rest_with_data PUT "$@"
}

# Perform a PUTg on $1 of data from file $2 or stdin
rest_PUT_FORM() {
    rest_with_formdata PUT "$@"
}

# Perform a POST on $1 of data from file $2 or stdin
rest_POST() {
    rest_with_data POST "$@"
}

# Perform a POST as multipart/form-data to $1 of file $2 or stdin
rest_POST_FORM() {
    rest_with_formdata POST "$@"
}

# vim: sts=4:sw=4:ai:si:et
