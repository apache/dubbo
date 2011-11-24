#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`

LIB_DIR=$DEPLOY_DIR/lib
LOGS_DIR=$DEPLOY_DIR/logs
STDOUT_FILE=$LOGS_DIR/stdout.log
SERVER_PORT=$1

if [ ! -d $LOGS_DIR ]; then
	mkdir $LOGS_DIR
fi

if [ -z "$SERVER_PORT" ]; then
	SERVER_PORT=8080
fi

SERVER_PORT_COUNT=`netstat -tln | grep $SERVER_PORT | wc -l`
if [ $SERVER_PORT_COUNT -gt 0 ]; then
	echo "********************************************************************"
	echo "** Error: Dubbo registry admin server port $SERVER_PORT already used!"
	echo "********************************************************************"
	exit 1
fi

JAVA_OPTS=" -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true "
JAVA_MEM_OPTS=""
BITS=`file $JAVA_HOME/bin/java | grep 64-bit`
if [ -n "$BITS" ]; then
    let memTotal=`cat /proc/meminfo |grep MemTotal|awk '{printf "%d", $2/1024 }'`
    if [ $memTotal -gt 2500 ];then
        JAVA_MEM_OPTS=" -server -Xmx2g -Xms2g -Xmn256m -XX:PermSize=128m -Xss256k -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 "
    else 
        JAVA_MEM_OPTS=" -server -Xmx1g -Xms1g -Xmn256m -XX:PermSize=128m -Xss256k -XX:+DisableExplicitGC -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+UseCMSCompactAtFullCollection -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 "
    fi
else
	JAVA_MEM_OPTS=" -server -Xms1024m -Xmx1024m -XX:PermSize=128m -XX:SurvivorRatio=2 -XX:+UseParallelGC "
fi

EXIST_PIDS=`ps  --no-heading -C java -f --width 1000 | grep "$DEPLOY_DIR" |awk '{print $2}'`
if [ ! -z "$EXIST_PIDS" ]; then
    echo "Dubbo registry admin server already started!"
    echo "PID: $EXIST_PIDS"
    exit;
fi

LIB_JARS=`ls $LIB_DIR|grep .jar|awk '{print "'$LIB_DIR'/"$0}'|tr "\n" ":"`

echo -e "Starting dubbo registry admin server on $SERVER_PORT \c"
nohup java $JAVA_OPTS $JAVA_MEM_OPTS -classpath $LIB_JARS com.alibaba.dubbo.container.Main properties log4j registry jetty >> $STDOUT_FILE 2>&1 &

COUNT=0
while [ $COUNT -lt 1 ]; do    
    echo -e ".\c"
    sleep 1 
	COUNT=`curl -s "http://127.0.0.1:$SERVER_PORT/status" |grep -c "OK"`
	if [ $COUNT -lt 1 ]; then
		break
	fi
done
echo "OK!"
START_PIDS=`ps  --no-heading -C java -f --width 1000 | grep "$DEPLOY_DIR" |awk '{print $2}'`
echo "PID: $START_PIDS"
echo "========================================================="
tail -f $STDOUT_FILE
