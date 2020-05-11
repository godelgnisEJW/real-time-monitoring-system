# real-time-monitoring-system

## 配置环境

### 安装docker

参考链接 https://segmentfault.com/a/1190000018157675  ，配置阿里云镜像加速器

```
sudo mkdir -p /etc/docker
sudo tee /etc/docker/daemon.json <<-'EOF'
{
  "registry-mirrors": ["https://j6fk6tnr.mirror.aliyuncs.com"]
}
EOF
sudo systemctl daemon-reload
sudo systemctl restart docker
```

### 安装docker compose 

参考链接 http://get.daocloud.io/#install-compose

### 安装Oracle JDK或者Open JDK 

以Centos7为例

```
rpm -qa | grep jdk
yum install java-1.8.0-openjdk-devel.x86_64
```

### 安装maven 

***版本需大于3.3.9***，参考链接 https://www.cnblogs.com/qiyuan880794/p/9407342.html，配置阿里云镜像

```
#修改maven目录下的settings.xml文件
vim settings.xml

#找到<mirrors>标签，在标签中间插入
<!-- 阿里云仓库 -->
    <mirror>
        <id>alimaven</id>
        <mirrorOf>central</mirrorOf>
        <name>aliyun maven</name>
        <url>http://maven.aliyun.com/nexus/content/repositories/central/</url>
    </mirror>
 
 
    <!-- 中央仓库1 -->
    <mirror>
        <id>repo1</id>
        <mirrorOf>central</mirrorOf>
        <name>Human Readable Name for this Mirror.</name>
        <url>http://repo1.maven.org/maven2/</url>
    </mirror>
 
 
    <!-- 中央仓库2 -->
    <mirror>
        <id>repo2</id>
        <mirrorOf>central</mirrorOf>
        <name>Human Readable Name for this Mirror.</name>
        <url>http://repo2.maven.org/maven2/</url>
    </mirror>
 #保存退出
```

### 安装node   

+ 可以直接从官网下载安装，或者参考链接https://blog.csdn.net/ziwoods/article/details/83751842，

+ 配置国内源

```
npm config set registry https://registry.npm.taobao.org –global
```

## 项目准备

### 打包前端项目

```
#进入到前端项目中
cd realtime-vue
#更改后端长连接地址
vim src/api/websocket.js
#将文件第二行中let ws = new WebSocket("ws://192.168.50.146:8080/websocket");
#ip地址更改为自己虚拟机的ip
#按下ESC,:wq，保存退出

#在realtime-vue目录下
#安装依赖
npm install
#编译项目
npm run build
#将打包好的文件复制备用
mv  dist  ../Linux/app/nginx/
```

### 打包后端项目

```
#进入到后端项目中
cd realtimebackend
#修改配置文件
vim src/main/resources/application.properties
#修改redis地址
spring.redis.host=192.168.50.146 //根据自己的机器进行修改
#保存退出
#安装依赖
mvn install
#打包项目
mvn package
#将文件复制备用
mv target/realtimebackend-0.0.1-SNAPSHOT.jar  ../Linux/app/springboot/
```

### 打包Flink任务

```
#进入Flink任务项目
cd flinkdev
#打包项目
mvn install
#复制备用
mv areaStatistics/target/areaStatistics-1.0-SNAPSHOT-jar-with-dependencies.jar  ../Linux/app/tasks/
mv interfaceStatistics/target/interfaceStatistics-1.0-SNAPSHOT-jar-with-dependencies.jar  ../Linux/app/tasks/
mv msgStatistics//target/msgStatistics-1.0-SNAPSHOT-jar-with-dependencies.jar  ../Linux/app/tasks/
mv onlineNumStatistics/target/onlineNumStatistics-1.0-SNAPSHOT-jar-with-dependencies.jar  ../Linux/app/tasks/
mv queueStatistics/target/queueStatistics-1.0-SNAPSHOT-jar-with-dependencies.jar  ../Linux/app/tasks/
mv userStatistics/target/userStatistics-1.0-SNAPSHOT-jar-with-dependencies.jar  ../Linux/app/tasks/
```

### 下载镜像

```
docker pull nginx
docker pull redis
docker pull bitnami/kafka
docker pull bitnami/zookeeper
docker pull flink:1.9.2-scala_2.12
docker pull docker.elastic.co/beats/filebeat:7.6.0
```

### 修改docker-compose.yml文件

```
cd Linux/app
#修改文件
vim docker-compose.yml
#修改文件中的本地文件路径，必须是绝对路径
#需要修改的有：
#  1. jm下的volumes
#  2. tm下的volumes
#  3. nginx下的volumes
#  4. filebeat下的volumes
#以nginx为例，文件中的配置为：
#/home/wje/real-time-monitoring-system/Linux/app/nginx/dist:/usr/share/nginx/html:ro
#real-time-monitoring-system为本项目根目录，/home/wje为项目存放的路径
#如果本项目放到了/home/zkpk下，那么就需要修改为：
#/home/zkpk/real-time-monitoring-system/Linux/app/nginx/dist:/usr/share/nginx/html:ro
#改完之后保存退出
```

## 启动项目

### 启动容器

```
#在app目录下，执行
docker-compose up -d
```

### 启动后端

```
cd springboot
nohup java -jar realtimebackend-0.0.1-SNAPSHOT.jar & 
```

### 创建kafka Topic

```
cd ../../scripts
./create-kafka-topics.sh
#如果报错，可以先执行
 ./delete-kafka-topics.sh
#再执行
./create-kafka-topics.sh
```

### 提交**所有**任务

+ **访问本机ip地址的8081端口，通过webUI提交任务**

![image1](E:\final\real-time-monitoring-system\images\image1.png)

![image2](E:\final\real-time-monitoring-system\images\image2.png)

+ **jar包对应的主类**

|                             包名                             |         主类名          |
| :----------------------------------------------------------: | :---------------------: |
|  **areaStatistics-1.0-SNAPSHOT-jar-with-dependencies.jar**   |   **areaStatistics**    |
| **interfaceStatistics-1.0-SNAPSHOT-jar-with-dependencies.jar** | **InterfaceStatistics** |
|   **msgStatistics-1.0-SNAPSHOT-jar-with-dependencies.jar**   |    **MsgStatistics**    |
| **onlineNumStatistics-1.0-SNAPSHOT-jar-with-dependencies.jar** | **OnlineNumStatistics** |
|  **queueStatistics-1.0-SNAPSHOT-jar-with-dependencies.jar**  |   **QueueStatistics**   |
|  **userStatistics-1.0-SNAPSHOT-jar-with-dependencies.jar**   |   **UserStatistics**    |

### 启动数据生成脚本

```
#在scripts目录下,执行
./start-all-producers.sh
#若想停止产生数据生成脚本，执行
./start-all-producers.sh
#若想清空脚本数据，执行
./clean.sh

#单独启动脚本
./start-producer-by-name.sh area.sh
#单独停止脚本
./stop-producer-by-name.sh area.sh
```

### redis主题

既是channe，也是key

|              功能              |          主题兼KEY          |
| :----------------------------: | :-------------------------: |
|          **在线人数**          |       **ONLINE:NUM**        |
|          **区域热度**          |        **AREA:HOT**         |
|          **区域排行**          |       **AREA:RANGE**        |
|        **接口请求统计**        |      **INTERFACE:NUM**      |
|        **队列堆积消息**        |    **QUEUE:PILEUP:NUM**     |
|          **消息数量**          |         **MSG:NUM**         |
|          **文件数量**          |        **FILE:SIZE**        |
| **用户来源及各端活跃用户占比** | **USER:SOURCES:PROPORTION** |

### 外部访问kafka数据

需要修改docker-compose.yml文件，kafka对应的配置下，environment下添加相应的环境变量，以下可做参考，通过29092端口即可访问kafka

```
- ALLOW_PLAINTEXT_LISTENER=yes   
- KAFKA_CFG_LISTENER_SECURITY_PROTOCOL_MAP=PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT 
- KAFKA_CFG_LISTENERS=PLAINTEXT://:9092,PLAINTEXT_HOST://:29092 
-KAFKA_CFG_ADVERTISED_LISTENERS=PLAINTEXT://kafka1:9092,PLAINTEXT_HOST://192.168.50.146:29092 
```

