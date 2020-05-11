#!/bin/bash
echo 'creating container zookeeper1'
docker run --name zookeeper1 -d \
--network app-tier \
-e ALLOW_ANONYMOUS_LOGIN=yes \
-e ZOO_SERVER_ID=1 \
-e ZOO_SERVERS=0.0.0.0:2881:3881,zookeeper2:2882:3882,zookeeper3:2883:3883 \
-p 2181:2181 \
-p 2881:2888 \
-p 3881:3888 \
bitnami/zookeeper:latest

echo 'creating container zookeeper2'
docker run --name zookeeper2 -d \
--network app-tier \
-e ALLOW_ANONYMOUS_LOGIN=yes \
-e ZOO_SERVER_ID=2 \
-e ZOO_SERVERS=zookeeper1:2881:3881,0.0.0.0:2882:3882,zookeeper3:2883:3883 \
-p 2182:2181 \
-p 2882:2888 \
-p 3882:3888 \
bitnami/zookeeper:latest

echo 'creating container zookeeper3'
docker run --name zookeeper3 -d \
--network app-tier \
-e ALLOW_ANONYMOUS_LOGIN=yes \
-e ZOO_SERVER_ID=3 \
-e ZOO_SERVERS=zookeeper1:2881:3881,zookeeper2:2882:3882,0.0.0.0:2883:3883 \
-p 2183:2181 \
-p 2883:2888 \
-p 3883:3888 \
bitnami/zookeeper:latest
