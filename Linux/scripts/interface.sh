#!/bin/sh


function getTime(){
    cur_sec_and_ns=`date '+%s-%N'`
    cur_sec=${cur_sec_and_ns%-*}
    cur_ns=${cur_sec_and_ns##*-}
    cur_timestamp=$((cur_sec*1000+$((10#$cur_ns))/1000000))
    echo $cur_timestamp
}

function rand(){   
    min=$1   
    max=$(($2-$min+1))   
    num=$(date +%s%N)   
    echo $(($num%$max+$min))   
} 

path=`dirname $0`
filePath=${path}"/../logs/interface.log"
if [ ! -f $filePath ];then
    touch $filePath
fi
#清空文件
`truncate -s 0 $filePath`
ss=("server1" "server2" "server3")
is=("/godelgnis/login" "/godelgnis/logout" "/godelgnis/msg" "/godelgnis/file")
#初始化数据
#生产数据
while true
do
    for((i=1;i<=10;i++)) do
        ts=$(getTime)
    	for((si=0;si<=2;si++)) do
        	ii=$(rand 0 3)
        	num=$(rand 10000 50000)
        	echo -e "$ts\t${ss[$si]}\t${is[$ii]}\t$num" >> $filePath
	done
    done
    sleep 0.8
done

