#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`

CR=`echo -e "\0015\c"`
SERVER_NAME=`sed '/dubbo.application.name/!d;s/.*=//' conf/dubbo.properties | sed -e "s/$CR//g"`
LOGS_FILE=`sed '/dubbo.log4j.file/!d;s/.*=//' conf/dubbo.properties | sed -e "s/$CR//g"`

LOGS_DIR=""
if [ -n "$LOGS_FILE" ]; then
	LOGS_DIR=`dirname $LOGS_FILE`
else
	LOGS_DIR=$DEPLOY_DIR/logs
fi

KILL_PIDS=`ps  --no-heading -C java -f --width 1000 | grep "$DEPLOY_DIR" |awk '{print $2}'`
if [ -z "$KILL_PIDS" ]; then
    echo "$SERVER_NAME does not started!"
    exit 1;
fi

if [ "$1" != "skip" ]; then
	DUMP_DATE=`date +%Y%m%d%H%M%S`
	DUMP_DIR=$LOGS_DIR/dump/dubbo-registry-$DUMP_DATE
	if [ ! -d $DUMP_DIR ]; then
		mkdir $DUMP_DIR
	fi
	echo -e "Dumping $SERVER_NAME \c"
	for PID in $KILL_PIDS ; do
		$JAVA_HOME/bin/jstack $PID > $DUMP_DIR/jstack-$PID.dump 2>&1
		echo -e ".\c"
		$JAVA_HOME/bin/jinfo $PID > $DUMP_DIR/jinfo-$PID.dump 2>&1
		echo -e ".\c"
		$JAVA_HOME/bin/jstat -gcutil $PID > $DUMP_DIR/jstat-gcutil-$PID.dump 2>&1
		echo -e ".\c"
		$JAVA_HOME/bin/jstat -gccapacity $PID > $DUMP_DIR/jstat-gccapacity-$PID.dump 2>&1
		echo -e ".\c"
		$JAVA_HOME/bin/jmap $PID > $DUMP_DIR/jmap-$PID.dump 2>&1
		echo -e ".\c"
		$JAVA_HOME/bin/jmap -heap $PID > $DUMP_DIR/jmap-heap-$PID.dump 2>&1
		echo -e ".\c"
		$JAVA_HOME/bin/jmap -histo $PID > $DUMP_DIR/jmap-histo-$PID.dump 2>&1
		echo -e ".\c"
		if [ -r /usr/sbin/lsof ]; then
		/usr/sbin/lsof -p $PID > $DUMP_DIR/lsof-$PID.dump
		echo -e ".\c"
		fi
	done
	if [ -r /usr/bin/sar ]; then
	/usr/bin/sar > $DUMP_DIR/sar.dump
	echo -e ".\c"
	fi
	if [ -r /usr/bin/uptime ]; then
	/usr/bin/uptime > $DUMP_DIR/uptime.dump
	echo -e ".\c"
	fi
	if [ -r /usr/bin/free ]; then
	/usr/bin/free -t > $DUMP_DIR/free.dump
	echo -e ".\c"
	fi
	if [ -r /usr/bin/vmstat ]; then
	/usr/bin/vmstat > $DUMP_DIR/vmstat.dump
	echo -e ".\c"
	fi
	if [ -r /usr/bin/mpstat ]; then
	/usr/bin/mpstat > $DUMP_DIR/mpstat.dump
	echo -e ".\c"
	fi
	if [ -r /usr/bin/iostat ]; then
	/usr/bin/iostat > $DUMP_DIR/iostat.dump
	echo -e ".\c"
	fi
	if [ -r /bin/netstat ]; then
	/bin/netstat > $DUMP_DIR/netstat.dump
	echo -e ".\c"
	fi
	echo "OK!"
fi

echo -e "Stopping $SERVER_NAME \c"
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
