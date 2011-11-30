#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
CR=`echo -e "\0015\c"`
LOGS_FILE=`sed '/dubbo.log4j.file/!d;s/.*=//' conf/dubbo.properties | sed -e "s/$CR//g"`
LOGS_DIR=`dirname $LOGS_FILE`
tail -f $STDOUT_FILE