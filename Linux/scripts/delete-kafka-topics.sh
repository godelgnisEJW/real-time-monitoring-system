#!/bin/bash
docker exec -it app_kafka1_1 /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper app_zookeeper_1:2181 --delete  --topic log-in-out-log 
docker exec -it app_kafka1_1 /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper app_zookeeper_1:2181 --delete  --topic areaLog 
docker exec -it app_kafka1_1 /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper app_zookeeper_1:2181 --delete  --topic msgLog 
docker exec -it app_kafka1_1 /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper app_zookeeper_1:2181 --delete  --topic interfaceLog 
docker exec -it app_kafka1_1 /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper app_zookeeper_1:2181 --delete --topic queueLog 
docker exec -it app_kafka1_1 /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper app_zookeeper_1:2181 --delete --topic userLog 
