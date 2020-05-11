#!/bin/bash
docker exec -it kafka1 /opt/bitnami/kafka/bin/kafka-topics.sh --zookeeper zookeeper1:2181 --list
