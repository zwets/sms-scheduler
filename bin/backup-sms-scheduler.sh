#!/bin/sh
#
# backup-sms-scheduler.sh
# Marco van Zwetselaar <io@zwets.it>
# 
# Script to create a backup of the SMS Scheduler on this server.

# Exit at first error to occur
set -e
trap cleanup EXIT
RETVAL=1

# Overall parameters
BACKUPS_BASE_DIR="/home/zwets"
WORK_DIR="/tmp/$(basename "$0").$$"

# Work with umask excluding access to non-root
umask 027

# Function to write arguments to stderr if VERBOSE is set
emit() {
	[ -z "$VERBOSE" ] || echo "$(basename "$0"): $*" >&2 || true
}

# Function to exit this script with an error message on stderr
err_exit() {
	echo "$(basename "$0"): $*" >&2
	exit # RETVAL will be returned from cleanup
}

# Function to cleanup at exit, trapped on EXIT
cleanup() {
	[ -d "$WORK_DIR" ] && emit "clean up, remove $WORK_DIR" && rm -rf "$WORK_DIR" || true
	exit $RETVAL
}
	
# Function to show usage information and exit
usage_exit() {
	echo "
Usage:$(basename $0) [OPTIONS]

  Make a backup of the SMS Scheduler and optionally copy it to a remote.

  OPTIONS
   -o, --outfile FILENAME
   -r, --remote HOSTNAME
   -p, --port PORT
   -v, --verbose

  Unless --outfile FILENAME is specified, the backup will be written
  under a timestamped filename to ${BACKUPS_BASE_DIR}.

  Use -r HOSTNAME to copy the backup to a remote machine.
"
	exit ${1:-1}
}

#Function to backup PostgreSQL database $1 to file $2
backup_database(){
	emit "dump database $1 to $2"
	su -l postgres -c "pg_dump -Fc $1" > "$2"
}

# Function to backup directory $1 to file $2
backup_directory() {
	[ -d "$1" ] || err_exit "no such directory: $1"
	emit "zipping directory $1 to $2"
	tar -cf "$2" -C "$1" .
}

# Function to backup files $3+ from DIR $2 to file $1
backup_files() {
	emit "backing up files from $2 to $1"
	local TAR="$1"; shift
	local DIR="$1"; shift
	tar -cf "$TAR" -C "$DIR" "$@"
}

# Parse options

while [ $# -ne 0 -a .$(expr "$1" : '\(.\).*'). = .-. ]; do
	case $1 in
	--outfile=*) OUTFILE=${1#--outfile=} ;;
	-o|--outfile) shift && OUTFILE=$1 ;;
	-v|--verbose) VERBOSE=1 ;;
	-h|--help) usage_exit 0 ;;
	*) usage_exit ;;
	esac
	shift
done

# Check options and argument

[ $# -eq 0 ] || usage_exit

[ $(id -u) -eq 0 ] || err_exit "must be root"

# Check for existence of backup directories

BACKUP_DIR=$BACKUPS_BASE_DIR
[ -d "$BACKUP_DIR" ] || err_exit "no such directory: $BACKUP_DIR"

# Perform the actual backup

DB_NAME="smes_prod"
emit "database name: $DB_NAME"

BACKUP_FILE="${OUTFILE:-$BACKUP_DIR/backup_sms-scheduler_$(date '+%F_%H%M%S').tar}"
BACKUP_NAME=$(basename "$BACKUP_FILE" .tar)
emit "commencing backup to $BACKUP_FILE"

mkdir -p "$WORK_DIR/$BACKUP_NAME" || true
backup_database "$DB_NAME" "$WORK_DIR/$BACKUP_NAME/psql.$DB_NAME.dump"

FILEDATA_DIR="/opt/sms-scheduler"
backup_directory "$FILEDATA_DIR" "$WORK_DIR/$BACKUP_NAME/filedata.tar"

emit "writing backup: $BACKUP_FILE"
tar cJf "$BACKUP_FILE" -C "$WORK_DIR" "$BACKUP_NAME"

# Clean up working directory
rm -rf "$WORK_DIR"

# Exit with success

RETVAL=0
exit # RETVAL will be returned from cleanup EXIT trap 

