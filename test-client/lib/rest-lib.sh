#!/bin/bash

export LC_ALL="C"
set -euo pipefail

USAGE="${USAGE:-}

  COMMON OPTIONS
   -u,--username=UNAME    Set the username for authentication [$UNAME]
   -p,--password=PWORD    Set the password for authentication [$PWORD]
   -b,--base-url=BASEURL  Set the base URL [$BASEURL]
   -c,--context=CONTEXT   Set the request context [$CONTEXT]
   -d,--dump              Write request entity to stderr (PUT, POST)
   -x,--headers           Write response headers to stderr
   -r,--raw               Do not use jq to pretty print JSON
   -n,--dry-run           Do everything except the real thing
   -v,--verbose           Display diagnostic output
   -h,--help              Display usage information

  All CAPITALISED options can also be passed as environment vars
"

# The usage exit function pulls in the including script's USAGE string

usage_exit() { echo "Usage: ${0##*/} [-dxrnvh] [-u UNAME] [-p PWORD] [-b BASEURL] [-c CONTEXT] ${USAGE}" >&2; exit ${1:-1}; }

# Parse the common options

TEMP=$(getopt -n "${0##*/}" -o 'dxrnvhu:p:b:c:' -l 'dump,headers,raw,dry-run,verbose,help,username:,password:,base-url:,context:' -- "$@" || exit 1)
eval set -- "$TEMP"
unset TEMP

while true; do
    case "$1" in
        -u|--u*)    UNAME="$2";   shift 2 ;;
        -p|--p*)    PWORD="$2";   shift 2 ;;
        -b|--b*)    BASEURL="$2"; shift 2 ;;
        -c|--c*)    CONTEXT="$2"; shift 2 ;;
        -d|--du*)   DUMP=1;       shift ;;
        -x|--hea*)  HEADERS=1;    shift ;;
        -r|--r*)    RAWOUT=1;     shift ;;
        -n|--dr*)   NOT_REALLY=1; DUMP=1; shift ;;
        -v|--v*)    VERBOSE=1;    shift ;;
        -h|--hel*)  usage_exit 0  ;;
        --) shift; break ;;
        *)  err_exit "lpt1 on fire!" ;;
    esac
done

# Check required params

[ $NOT_REALLY ] || [ -n "$UNAME" ] || err_exit "UNAME must be specified; set DEFAULT_UNAME in lib/defaults or use -u/--username"
[ $NOT_REALLY ] || [ -n "$PWORD" ] || err_exit "PWORD must be specified; set DEFAULT_PWORD in lib/defaults or use -p/--password"
[ $NOT_REALLY ] || [ -n "$BASEURL" ] || err_exit "BASEURL must be specified; set DEFAULT_BASEURL in lib/defaults or use -b/--base-url"

# Resolve necessary tools

CURL="$(which curl 2>/dev/null)" || err_exit "command not found: curl (do: apt install)"
JQ="$(which jq 2>/dev/null)" || err_exit "command not found: jq (do: apt install jq)"

# Input and output filters

dump_diags() {
    emit "BASEURL   = $BASEURL"
    emit "CONTEXT   = $CONTEXT"
    emit "UNAME     = $UNAME"
    emit "PWORD     = $PWORD"
}

json_filter() {
    [ $RAWOUT ] && cat "$@" || "$JQ" . "$@"
}

dump_filter() {
    [ $DUMP ] && json_filter "$@" | tee /dev/stderr || cat "$@"
}

# Base REST calls

# Base call to curl with authentication and headers
rest_call() {
    [ $NOT_REALLY ] && echo "REST $@" >&2 && cat >/dev/null ||
    curl -sS --fail${VERBOSE:+-with-body} ${HEADERS:+--dump-header /dev/stderr} \
        -u "$UNAME:$PWORD" \
        -H 'Accept: application/json' \
        -H 'Accept: text/plain' \
        "$@" |
        json_filter
}

# Perform verb $1 on $2 of data from file $3 or stdin
rest_with_data() {
    F="${3:--}" && [ "$F" = '-' ] || [ -f "$F" ] || err_exit "no such file: $F"
    dump_filter "$F" | rest_call -X $1 -H 'Content-Type: application/json' --data "@-" "${BASEURL}${CONTEXT}${2:-}"
}

# Perform verb $1 on $2 as multipart/form-data from file $3 or stdin
rest_with_formdata() {
    F="${3:--}" && [ "$F" = '-' ] || [ -f "$F" ] || err_exit "no such file: $F"
    dump_filter "$F" | rest_call -X $1 --form "body=@-" "${BASEURL}${CONTEXT}${2:-}"
}

# Perform a GET on $1
rest_GET() {
    [ $NOT_REALLY ] && echo "REST GET ${BASEURL}${CONTEXT}${1:-}" >&2 ||
    rest_call "${BASEURL}${CONTEXT}${1:-}"
}

# Perform a DELETE on $1
rest_DELETE() {
    [ $NOT_REALLY ] && echo "REST DELETE ${BASEURL}${CONTEXT}${1:-}" >&2 ||
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
