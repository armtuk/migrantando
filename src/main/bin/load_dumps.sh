#!/bin/sh

DATAHOME=$1
TABLESFILE=$2


TABLESLIST=`cat $TABLESFILE`

cd "$1"
DATAFILES=`ls *.dump`

for a in $TABLESLIST
do
	FILENAME=`echo "dbo.$a.dump" | sed 's/_//g'`
	echo "Loading $a from $FILENAME"
	if [ -e "$FILENAME" ]; then
		echo "Found $FILENAME"
		psql -hlocalhost eve -c "copy $a from '$DATAHOME/$FILENAME'"
	else
		echo "Warning: $FILENAME not found"
	fi
done
