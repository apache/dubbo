#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
CONF_DIR=$DEPLOY_DIR/conf
LIB_DIR=$DEPLOY_DIR/lib
LOGS_DIR=$DEPLOY_DIR/logs
STDOUT_FILE=$LOGS_DIR/stdout.log
CR=`echo -e "\0015\c"`
SERVER_NAME=`sed '/dubbo.application.name/!d;s/.*=//' conf/dubbo.properties | sed -e "s/$CR//g"`

if [ ! -d $LOGS_DIR ]; then
	mkdir $LOGS_DIR
fi

JAVA_OPTS=" -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true "
JAVA_DEBUG_OPTS=""
if [ "$1" = "debug" ]; then
JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n "
fi
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
    echo "$SERVER_NAME already started!"
    echo "PID: $EXIST_PIDS"
    exit;
fi

LIB_JARS=`ls $LIB_DIR|grep .jar|awk '{print "'$LIB_DIR'/"$0}'|tr "\n" ":"`

echo -e "Starting $SERVER_NAME \c"

nohup java $JAVA_OPTS $JAVA_MEM_OPTS $JAVA_DEBUG_OPTS -classpath $CONF_DIR:$LIB_JARS com.alibaba.dubbo.demo.consumer.DemoConsumer > $STDOUT_FILE 2>&1 &

echo "OK!"
START_PIDS=`ps  --no-heading -C java -f --width 1000 | grep "$DEPLOY_DIR" |awk '{print $2}'`
echo "PID: $START_PIDS"
echo "========================================================="
tail -f $STDOUT_FILE
