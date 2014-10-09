#!/bin/bash
#include db general functions
pushd $(dirname ${0})
source ./dbfunctions.sh
source ./dbcustomfunctions.sh

#setting defaults
set_defaults

usage() {
    printf "Usage: ${ME} [-h] [-s SERVERNAME [-p PORT]] [-d DATABASE] [-u USERNAME] [-l LOGFILE] [-v]\n"
    printf "\n"
    printf "\t-s SERVERNAME - The database servername for the database  (def. ${SERVERNAME})\n"
    printf "\t-p PORT       - The database port for the database        (def. ${PORT})\n"
    printf "\t-d DATABASE   - The database name                         (def. ${DATABASE})\n"
    printf "\t-u USERNAME   - The admin username for the database.\n"
    printf "\t-l LOGFILE    - The logfile for capturing output          (def. ${LOGFILE})\n"
    printf "\t-v            - Turn on verbosity                         (WARNING: lots of output)\n"
    printf "\t-h            - This help text.\n"
    printf "\n"
    popd
    exit $ret
}

DEBUG () {
    if $VERBOSE; then
        printf "DEBUG: $*"
    fi
}

while getopts :hs:d:u:p:l:f:v option; do
    case $option in
        s) SERVERNAME=$OPTARG;;
        p) PORT=$OPTARG;;
        d) DATABASE=$OPTARG;;
        u) USERNAME=$OPTARG;;
    	l) LOGFILE=$OPTARG;;
        v) VERBOSE=true;;
        h) ret=0 && usage;;
       \?) ret=1 && usage;;
    esac
done

printf "Creating the database: ${DATABASE}\n"
#try to drop the database first (if exists)
dropdb --username=${USERNAME} --host=${SERVERNAME} --port=${PORT} ${DATABASE} -e > /dev/null
createdb --username=${USERNAME} --host=${SERVERNAME} --port=${PORT} ${DATABASE} -e -E UTF8 -T template0 > /dev/null
if [ $? -ne 0 ]
    then
      printf "Failed to create database ${DATABASE}\n"
      popd
      exit 1;
fi
createlang --host=${SERVERNAME} --port=${PORT} --dbname=${DATABASE} --echo --username=${USERNAME} plpgsql >& /dev/null
#set database min error level
CMD="ALTER DATABASE \"${DATABASE}\" SET client_min_messages=ERROR;"
execute_command "${CMD}"  ${DATABASE} ${SERVERNAME} ${PORT}> /dev/null

echo user name is: ${USERNAME}

printf "Creating tables...\n"
execute_file "create_tables.sql" ${DATABASE} ${SERVERNAME} ${PORT} > /dev/null

printf "Creating functions...\n"
execute_file "create_functions.sql" ${DATABASE} ${SERVERNAME} ${PORT} > /dev/null

printf "Creating common functions...\n"
execute_file "common_sp.sql" ${DATABASE} ${SERVERNAME} ${PORT} > /dev/null

#inserting initial data
insert_initial_data

#remove checksum file in clean install in order to run views/sp creation
rm -f .${DATABASE}.scripts.md5 >& /dev/null

# Running upgrade scripts
printf "Running upgrade scripts...\n"
run_upgrade_files

popd
exit $?
