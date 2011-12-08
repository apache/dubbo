#!/bin/bash
cd `dirname $0`
if [ "$1" = "start" ]; then
	./start.sh
else
	if [ "$1" = "stop" ]; then
		./stop.sh
	else
		if [ "$1" = "restart" ]; then
			./restart.sh
		else
			if [ "$1" = "dump" ]; then
				./dump.sh
			else
				echo "ERROR: Please input argument: start or stop or restart or dump"
			    exit 1
			fi
		fi
	fi
fi
