#!/bin/bash

export LC_ALL="C"
set -euo pipefail

# Rewrite $1 schedule in +SEC1[+SEC2] notation to proper schedule and echo to stdout

rewrite_schedule() {  # rewrite the +SEC1[+SEC2] to proper schedule
    [ "${1#+}" == "${1:-}" ] && return || true
    emit "rewrite schedule: $1"
    declare line="${1#+}" 
    declare t1="${line%+*}"
    declare -i now=$(date +%s)
    [ "$line" = "$t1" ] && declare -i s1=${t1:-0} s2=600 || declare -i s1=${t1:-0} s2=${line#*+}
    echo $(date -Is --date=@$((now + s1)))/$(date -Is --date=@$((now + s1 + s2)))
}

# vim: sts=4:sw=4:ai:si:et
