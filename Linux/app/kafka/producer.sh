#!/bin/bash
docker exec -it kafka1 /opt/bitnami/kafka/bin/kafka-console-producer.sh --broker-list kafka1:9092 --topic first
