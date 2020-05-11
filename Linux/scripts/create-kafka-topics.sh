#!/bin/bash
docker exec -it app_kafka1_1 /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper app_zookeeper_1:2181 --create --topic log-in-out-log --partitions 3 --replication-factor 3
docker exec -it app_kafka1_1 /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper app_zookeeper_1:2181 --create --topic areaLog --partitions 3 --replication-factor 3
docker exec -it app_kafka1_1 /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper app_zookeeper_1:2181 --create --topic msgLog --partitions 3 --replication-factor 3
docker exec -it app_kafka1_1 /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper app_zookeeper_1:2181 --create --topic interfaceLog --partitions 3 --replication-factor 3
docker exec -it app_kafka1_1 /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper app_zookeeper_1:2181 --create --topic queueLog --partitions 3 --replication-factor 3
docker exec -it app_kafka1_1 /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper app_zookeeper_1:2181 --create --topic userLog --partitions 3 --replication-factor 3

