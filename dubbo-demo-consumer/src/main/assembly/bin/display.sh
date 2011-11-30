#!/bin/bash
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=`pwd`
LOGS_DIR=$DEPLOY_DIR/logs
STDOUT_FILE=$LOGS_DIR/stdout.log
tail -f $STDOUT_FILE