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
filePath=${path}"/../logs/msg.log"
if [ ! -f $filePath ];then
    touch $filePath
fi
#清空文件
`truncate -s 0 $filePath`
array=("msg-text" "msg-radio" "msg-emoj" "file-img" "file-normal" "file-video")
#初始化数据
#生产数据
while true
do
    for((i=0;i<=5;i++)) do
        ts=$(getTime)
        case $i in
            0)
            num=$(rand 100000 10000000)
            echo -e "$ts\t${array[$i]}\t$num" >> $filePath
            ;;
            1)
            num=$(rand 100000 3000000)
            echo -e "$ts\t${array[$i]}\t$num" >> $filePath
            ;;
            2)
            num=$(rand 100000 3000000)
            echo -e "$ts\t${array[$i]}\t$num" >> $filePath
            ;;
            3)
            num=$(rand 1000 5000)
            size=$(rand 1024 3145728)
            echo -e "$ts\t${array[$i]}\t$num\t$size" >> $filePath
            ;;
            4)
            num=$(rand 500 1000)
            size=$(rand 5120 10485760)
            echo -e "$ts\t${array[$i]}\t$num\t$size" >> $filePath
            ;;
            5)
            num=$(rand 500 1000)
            size=$(rand 5242880 31457280)
            echo -e "$ts\t${array[$i]}\t$num\t$size" >> $filePath
            ;;
        esac
    done
    sleep 0.5
done

