#!/bin/sh

path=`dirname $0`
dir=${path}"/../logs"
if [ ! -d ${dir} ];then
    mkdir ${dir}
fi

nohup  sh ${path}/$1  >/dev/null 2>&1 &
pid=`ps -aux|grep $1 | grep -v grep|awk {'print $2'}`
echo `$1  started $pid`
