#!/bin/bash

#
# Stop felix
#
# Kill the currently running server (there's gotta be a better way!)
CAPEHUB_PID=`ps aux | awk '/felix.jar/ && !/awk/ {print $2}'`
if [ -z "$CAPEHUB_PID" ]; then
  echo "Capehub already stopped"
  exit 1
fi

kill $CAPEHUB_PID

sleep 5

CAPEHUB_PID=`ps aux | awk '/felix.jar/ && !/awk/ {print $2}'`
if [ ! -z "$CAPEHUB_PID" ]; then
  echo "Hard killing since felix ($CAPEHUB_PID) seems unresponsive to regular kill"
  
  kill -9 $CAPEHUB_PID
fi