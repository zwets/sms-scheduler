#!/bin/bash
#
# This file is meant to be sourced as the very first thing in every script.

export LC_ALL="C"
set -euo pipefail

# General functions we need right from the start
emit() { [ -z ${VERBOSE:-} ] || echo "${0##*/}: $*" >&2; }
err_exit() { echo "${0##*/}: $*" >&2; exit 1; }

# The script must have exported LIB_DIR to the location of this script
[ -n "$LIB_DIR" ] || err_exit "script must set LIB_DIR before including lib/settings.sh"

# Check that this file is indeed in LIB_DIR (else LIB_DIR is almost certainly wrong)
[ -f "$LIB_DIR/settings.sh" ] || err_exit "LIB_DIR is not set correctly; no such file: $LIB_DIR/settings.sh"

# Check for the defaults
[ -f "$LIB_DIR/defaults" ] || err_exit "please create the defaults file from defaults.example in: $LIB_DIR"

# Source the defaults
. "$LIB_DIR/defaults"

# Give all script variables their DEFAULT value unless they were already
# set, either by the user in the env or ny the scripts sourcing us
CLIENT="${CLIENT:-$DEFAULT_CLIENT}"
SCHEDULE="${SCHEDULE:-$DEFAULT_SCHEDULE}"
SENDER="${SENDER:-$DEFAULT_SENDER}"
RECIPIENT="${RECIPIENT:-$DEFAULT_RECIPIENT}"
UNAME="${UNAME:-$DEFAULT_UNAME}"
PWORD="${PWORD:-$DEFAULT_PWORD}"
BASEURL="${BASEURL:-$DEFAULT_BASEURL}"
CONTEXT="${CONTEXT:-$DEFAULT_CONTEXT}"
BROKER="${BROKER:-$DEFAULT_BROKER}"
TOPIC="${TOPIC:-$DEFAULT_TOPIC}"

# Define other variables (to save scripts from having to do this)
GROUPID="${GROUPID:-}"
PART="${PART:-}"
OFFSET="${OFFSET:-}"
KAFKAKEY="${KAFKAKEY:-}"
DUMP="${DUMP:-}"
RAWOUT="${RAWOUT:-}"
VERBOSE="${VERBOSE:-}"
HEADERS="${HEADERS:-}"
NOT_REALLY="${NOT_REALLY:-}"

# Generate default (pseudo) unique value for BIZKEY
BIZKEY="${BIZKEY:-${CLIENT}-${RANDOM}$$}"

# Default the PUBFILE to 'LIB_DIR/keys/$CLIENT.pub'
PUBFILE="${PUBFILE:-$LIB_DIR/keys/${CLIENT}.pub}"

# vim: sts=4:sw=4:ai:si:et
