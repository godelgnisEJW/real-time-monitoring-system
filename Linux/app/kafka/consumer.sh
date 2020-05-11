#!/bin/bash
docker exec -it $1 /opt/bitnami/kafka/bin/kafka-console-consumer.sh --bootstrap-server kafka1:9092,kafka2:9092,kafka3:9092 --topic $2 --from-beginning
