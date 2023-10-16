#!/bin/bash

export LC_ALL="C"
set -euo pipefail

curl -sX POST  http://admin:test@localhost:8081/process
