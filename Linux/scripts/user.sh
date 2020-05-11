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

function getType(){
    types=("ios" "android" "pc")
    index=$(rand 0 2)
    echo ${types[$index]}
}

path=`dirname $0`
filePath=${path}"/../logs/user.log"
if [ ! -f $filePath ];then
    touch $filePath
fi
#清空文件
`truncate -s 0 $filePath`
#初始化数据

#生产数据
while true
do
    for((i=1;i<=10;i++)) do
        ts=$(getTime)
        num=$(rand 1000 5000)
        tp=$(getType $ts)
        rate=$(printf "%.2f" `echo "scale=4;$(rand 0 100)/100" |bc`)
        echo -e "$ts\t$tp\t$num\t$rate" >> $filePath
    done
    sleep 0.8
done

