#!/bin/sh

#如果文件夹不存在，创建它

path=`dirname $0`
dir=${path}"/../logs"
if [ ! -d ${dir} ];then
    mkdir ${dir}
fi

nohup  sh ${path}/area.sh  >/dev/null 2>&1 &
pid=`ps -aux|grep area.sh|grep -v grep|awk {'print $2'}`
echo "area.sh started $pid"
nohup sh  ${path}/interface.sh  >/dev/null 2>&1 &
pid=`ps -aux|grep interface.sh|grep -v grep|awk {'print $2'}`
echo "interface.sh started $pid"
nohup sh ${path}/log-in-out.sh  >/dev/null 2>&1 &
pid=`ps -aux|grep log-in-out.sh|grep -v grep|awk {'print $2'}`
echo "log-in-out.sh started $pid"
nohup sh ${path}/msg.sh  >/dev/null 2>&1 &
pid=`ps -aux|grep msg.sh|grep -v grep|awk {'print $2'}`
echo "msg.sh started $pid"
nohup sh ${path}/queue.sh  >/dev/null 2>&1 &
pid=`ps -aux|grep queue.sh|grep -v grep|awk {'print $2'}`
echo "queue.sh started $pid"
nohup sh ${path}/user.sh  >/dev/null 2>&1 &
pid=`ps -aux|grep user.sh|grep -v grep|awk {'print $2'}`
echo "user.sh stared $pid"
