#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`

KILL_PIDS=`ps  --no-heading -C java -f --width 1000 | grep "$DEPLOY_DIR" |awk '{print $2}'`
if [ -z "$KILL_PIDS" ]; then
    echo "Dubbo registry admin server does not started!"
    exit 1;
fi

echo -e "Stopping dubbo registry admin server \c"
for PID in $KILL_PIDS ; do
	echo -e "$PID \c"
	kill $PID > /dev/null 2>&1
done

COUNT=0
while [ $COUNT -lt 1 ]; do    
    echo -e ".\c"
    sleep 1
    COUNT=1
    for PID in $KILL_PIDS ; do
		PID_PS=`ps --no-heading -p $PID`
		if [ -n "$PID_PS" ]; then
			COUNT=0
			break
		fi
	done
done
echo "OK!"
echo "PID: $KILL_PIDS"
