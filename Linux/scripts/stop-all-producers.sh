#!/bin/sh

pid=`ps -aux|grep area.sh|grep -v grep|awk {'print $2'}`
pkill -f area.sh
echo "area.sh stoped $pid"

pid=`ps -aux|grep interface.sh|grep -v grep|awk {'print $2'}`
pkill -f interface.sh 
echo "interface.sh stoped $pid"

pid=`ps -aux|grep log-in-out.sh|grep -v grep|awk {'print $2'}`
pkill -f log-in-out.sh 
echo "log-in-out.sh stoped $pid"

pid=`ps -aux|grep msg.sh|grep -v grep|awk {'print $2'}`
pkill -f msg.sh 
echo "msg.sh stoped $pid"

pid=`ps -aux|grep queue.sh|grep -v grep|awk {'print $2'}`
pkill -f queue.sh
echo "queue.sh stoped $pid"

pid=`ps -aux|grep user.sh|grep -v grep|awk {'print $2'}`
pkill -f user.sh 
echo "user.sh stoped $pid"
