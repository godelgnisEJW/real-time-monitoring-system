#!/bin/sh

topics=("areaLog" "log-in-out-log" "interfaceLog" "msgLog" "queueLog" "userLog")
for i in ${topics[@]}
do
    docker exec -it $1  /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper zookeeper1:2181,zookeeper2:2181,zookeeper3:2181 --delete --topic $i
done

