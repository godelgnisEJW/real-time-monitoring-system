#!/bin/sh

path=`dirname $0`
filePath=${path}"/../logs/log-in-out.log"

if [ ! -f $filePath ];then
    touch $filePath
fi
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

function getType(){
    if [ $(($1%2)) == 0 ] ; then
        echo login
    else
        echo logout
    fi
}
#清空文件
`truncate -s 0 $filePath`
#初始化数据
ts=$(getTime)
echo -e  "$ts\t142318675\tlogin" >> $filePath
#生产数据
while true
do
    ts=$(getTime)
    num=$(rand 0 300)
    tp=$(getType $num)

    echo -e  "$ts\t$num\t$tp" >> $filePath
    sleep 0.1
done
