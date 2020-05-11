#!/bin/sh

pid=`ps -aux | grep $1 | grep -v grep | awk {'print $2'}`
pkill -f $1
echo `$1  stoped $pid`
