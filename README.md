- [1. Overview](#1-overview)
  - [1.1. Prepare Environment](#11-prepare-environment)
    - [1.1.1. Download Apache Pulsar](#111-download-apache-pulsar)
    - [1.1.2. Run Pulsar in "Standalone" Mode](#112-run-pulsar-in-standalone-mode)
    - [1.1.3. Pulsar CLI Commands](#113-pulsar-cli-commands)
      - [1.1.3.1. Configure CLI Connection to Pulsar Server](#1131-configure-cli-connection-to-pulsar-server)
      - [1.1.3.2. Demonstrate Pulsar Admin CLI](#1132-demonstrate-pulsar-admin-cli)
- [2. Demo Program Description](#2-demo-program-description)
  - [2.1. Configure Connection Properties](#21-configure-connection-properties)
- [3. Pulsar Native Client API Demo](#3-pulsar-native-client-api-demo)
  - [3.1. Simple Demo](#31-simple-demo)
    - [3.1.1.  Run as a Message Producer](#311--run-as-a-message-producer)
    - [3.1.2. Run as a Message Consumer](#312-run-as-a-message-consumer)
- [4. DS FastJMS API Demo](#4-ds-fastjms-api-demo)
  - [4.1. Simple Demo (Including JMS Topic Pattern Demo)](#41-simple-demo-including-jms-topic-pattern-demo)
    - [4.1.1. Run as a Message Producer to a JMS Queue (p2p)](#411-run-as-a-message-producer-to-a-jms-queue-p2p)
    - [4.1.2. Run as a Message Consumer to a JMS Queue (p2p)](#412-run-as-a-message-consumer-to-a-jms-queue-p2p)
    - [4.1.3. Run as a Message Producer to a JMS Topic (pub/sub)](#413-run-as-a-message-producer-to-a-jms-topic-pubsub)
    - [4.1.4. Run as a Message Consumer to a JMS Topic (pub/sub)](#414-run-as-a-message-consumer-to-a-jms-topic-pubsub)
  - [4.2. JMS Queue Pattern Demo](#42-jms-queue-pattern-demo)
    - [4.2.1. Send Messages Using JMS QueueSender (Fire and Forget)](#421-send-messages-using-jms-queuesender-fire-and-forget)
    - [4.2.2. Receive Messages Using JMS QueueReceiver with Message Selector](#422-receive-messages-using-jms-queuereceiver-with-message-selector)
    - [4.2.3. Browse JMS Queue Messages Using JMS QueueBrowser](#423-browse-jms-queue-messages-using-jms-queuebrowser)
    - [4.2.4. Send Messages and Wait for Responses using JMS QueueRequestor](#424-send-messages-and-wait-for-responses-using-jms-queuerequestor)
- [5. More Advanced DS FastJMS API Example](#5-more-advanced-ds-fastjms-api-example)
  - [5.1. Multiple Queue Consumers - Message Consumption by "Keys"](#51-multiple-queue-consumers---message-consumption-by-keys)

# 1. Overview

[DataStax FastJMS for Pulsar API (DS FastJMS API)](https://github.com/datastax/pulsar-jms) is a Java based API that allows existing JMS based applications connecting to [Apache Pulsar](https://pulsar.apache.org/) as a JMS provider to publish and consume messages. Compared with an existing JMS provider like ActiveMQ, RabbitMQ, etc., Apache Pulsar is a modern, unified messaging and streaming platform that can deliver much higher throughput message publishing and consuming while maintaining consistent low latency. It also has other major benefits that are not available in any existing JMS providers, such as effective multi-tenancy support, native schema registry, out-of-the-box message geo-replication, and so on.

DS FastJMS API is the only JMS API for Apache Pulsar that supports JMS 2.0 with backward-compatibility with JMS 1.1. It is a full-fledged API that supports the vast majority of the JMS functionalities as described JMS spec. It passes about 98% JMS compliance TCK test suite.

In this repo, we're going to do some demos about how to use DS FastJMS API to achieve some typical JMS message sending and receiving methods against an Apache Pulsar server.

## 1.1. Prepare Environment

* OS: Linux or MacOS
* Java: JDK 8 or 11

### 1.1.1. Download Apache Pulsar 

** latest release: 2.8.0

```
curl -L -O 'https://www.apache.org/dyn/mirrors/mirrors.cgi?action=download&filename=pulsar/pulsar-2.8.0/apache-pulsar-2.8.0-bin.tar.gz'

tar -zxvf apache-pulsar-2.8.0-bin.tar.gz
cd apache-pulsar-2.8.0/
export PULSAR_HOME=`pwd`
export PATH="$PULSAR_HOME/bin:$PATH"

which pulsar
``` 

### 1.1.2. Run Pulsar in "Standalone" Mode 

Run the following command to start a standalone Pulsar server locally. 

```
bin/pulsar standalone &
```

### 1.1.3. Pulsar CLI Commands

Pulsar has a set of built-in CLI command utilities:
* Pulsar Admin CLI: for the purpose of administrating Pulsar objects such as tenants, namespaces, topics, and etc.  
  * https://pulsar.apache.org/tools/pulsar-admin/2.8.0-SNAPSHOT/
* Pulsar Client CLI: for the purpose of quick simulation of a message sending and consuming client 
  * https://pulsar.apache.org/docs/en/reference-cli-tools/#pulsar-client
* Pulsar Perf CLI: for Pulsar performance testing purposes
  * https://pulsar.apache.org/docs/en/reference-cli-tools/#pulsar-perf

In this demo, we'll briefly explore some Pulsar Admin CLI commands. 
  
#### 1.1.3.1. Configure CLI Connection to Pulsar Server

In order to use these CLI utilities, we need to first configure the client connection property file.

* default: <PULSAR_HOME>/conf/**client.conf**
* https://pulsar.apache.org/docs/en/reference-configuration/#client
  
```
PULSAR_CLIENT_CONF=<custom_client_conf_path> pulsar-admin [sub-command ...]
```

#### 1.1.3.2. Demonstrate Pulsar Admin CLI  

*  Verify Pulsar cluster
```
bin/pulsar-admin clusters list
```

* Manage Pulsar **Tenant**
```
bin/pulsar-admin tenants list
bin/pulsar-admin tenants create mytenant

bin/pulsar-admin tenants
```

* Manage Pulsar **Namespace**

```
bin/pulsar-admin namespaces create mytenant/ns0
bin/pulsar-admin namespaces list mytenant

bin/pulsar-admin namespaces
```

* Manage Pulsar **Topics** (Partitioned or not)

```
bin/pulsar-admin topics create mytenant/ns0/t0
bin/pulsar-admin topics list mytenant/ns0

bin/pulsar-admin topics create-partitioned-topic -p 5 mytenant/ns0/pt0
bin/pulsar-admin topics list-partitioned-topics mytenant/ns0
bin/pulsar-admin topics get-partitioned-topic-metadata mytenant/ns0/pt0
bin/pulsar-admin topics update-partitioned-topic mytenant/ns0/pt0 -p 6
```

# 2. Demo Program Description

In this repo, there are several sets of demo programs organized under different java packages (com.example.XYZ). These sets of programs are summarized as below:

| Demo Program Package | Description |
| -------------------- | ----------- |
| com.example.pulsar.* | Simple message producer and consumer using native Pulsar client API |
| com.example.fastjms.* | Simple message producer and consumer using DS FastJMS API, including JMS topic related operations. |
| com.example.fastjms.queue_pattern.* | Specific JMS queue related operations using DS FastJMS API |

## 2.1. Configure Connection Properties

In order to run the demo programs against the Pulsar server, we need to first configure the connection properties. An example is as below: 

```
## Pulsar Client connection configuration
webServiceUrl=http://localhost:8080
brokerServiceUrl=pulsar://localhost:6650
authPlugin=org.apache.pulsar.client.impl.auth.AuthenticationToken
authParams=token:<token_value>

## Other Pulsar client specific configuration
client.numIoThreads=2

## Pulsar producer specific configuration
producer.blockIfQueueFull=true

## Pulsar consumer specific configuration
consumer.receiverQueueSize=1000

## JMS specific configuration
jms.queueSubscriptionName=myjms-queue
jms.forceDeleteTemporaryDestinations=true
```

There are four major sections you can specify in the configuration property file: 

* Core Pulsar connection related settings
  * service URLs - web and broker
  * authentication
  * TLS
  * No prefix
  * https://pulsar.apache.org/docs/en/reference-configuration/#client

* Other Pulsar client connection related settings
  * exclude settings from above
  * prefix: **client**
  * https://pulsar.apache.org/docs/en/client-libraries-java/#client

* Pulsar Producer related settings
  * prefix: **producer**
  * https://pulsar.apache.org/docs/en/client-libraries-java/#configure-producer

* Pulsar Consumer related Settings
  * prefix: **consumer**
  * https://pulsar.apache.org/docs/en/client-libraries-java/#configure-consumer

* Pulsar JMS specific settings
  * prefix: **jms**
  * https://docs.datastax.com/en/fast-pulsar-jms/docs/1.1/pulsar-jms-reference.html#_configuration_options

# 3. Pulsar Native Client API Demo 

In order to use the Pulsar Native Client AP, includes the following dependency in your program:
(gradle dependency)
```
implementation group: 'org.apache.pulsar', name: 'pulsar-client', version: '2.8.0'
```

## 3.1. Simple Demo

The main demo program for demonstrating message publishing (producer) and message consuming (consumer) using Pulsar native client API is **com.example.pulsar.PulsarSimpleDemo**. Its usage is as below:
```text
usage: PulsarSimpleDemo [-f <arg>] [-h] [-n <arg>] [-op <arg>] [-sn <arg>] [-st <arg>] [-t <arg>]
PulsarSimpleDemo Options:
  -f <arg>  Configuration properties file.
  -h        Displays this help message.
  -n <arg>  Number of messages
  -op <arg> Operation type - "Producer" or "Consumer"
  -sn <arg> Subscription name
  -st <arg> Subscription type (default to "Exclusive")
  -t <arg>  Pulsar topic name
```

### 3.1.1.  Run as a Message Producer

An example of publishing 10 messages to a Pulsar topic is as below:
```
% java -cp pulsar_exmples-1.0-SNAPSHOT-all.jar com.example.pulsar.PulsarSimpleDemo \
  -f <path/to/conn.properties> \
  -t persistent://mytenant/ns0/t0 \
  -op producer \
  -n 10
```

### 3.1.2. Run as a Message Consumer

An example of receiving 10 messages from a Pulsar topic is as below:
```
% java -cp pulsar_exmples-1.0-SNAPSHOT-all.jar com.example.pulsar.PulsarSimpleDemo \
  -f <path/to/conn.properties> \
  -t persistent://mytenant/ns0/t0 \
  -op consumer \
  -n 10 \
  -sn mysub \
  -st Shared 
```

# 4. DS FastJMS API Demo

In order to use the DS FastJMS AP, includes the following dependency in your program:
(gradle dependency)
```
implementation group: 'com.datastax.oss', name: 'pulsar-jms-all', version: '1.2.2'
```

## 4.1. Simple Demo (Including JMS Topic Pattern Demo)

The first FastJMS API demo program, **com.example.fastjms.JmsSimpleDemo**, is similar to the Pulsar native client API demo program in the previous section. It follows JMS 2.0 spec and demonstrates how message sending/publishing and receiving/consuming works with DS FastJMS API.
 
Its usage is as below:
```text
usage: JmsSimpleDemo [-dn <arg>] [-dt <arg>] [-f <arg>] [-h] [-n <arg>] [-op <arg>] [-sn <arg>]
JmsSimpleDemo Options:
  -dn <arg> JMS destination name: pulsar topic name
  -dt <arg> JMS destination type - "queue" or "topic"
  -f <arg>  Configuration properties file.
  -h        Displays this help message.
  -n <arg>  Number of messages
  -op <arg> Operation type - "Producer, Consumer, SharedConsumer, DurableConsumer, SharedDurableConsumer"
  -sn <arg> Subscription name
```

**NOTE** that
* For a \"queue\" destination, the valid operation type ("-op") can only be "Producer" (QueueSender) or "Consumer" (QueueReceiver)
* For a \"topic\" destination, the valid operation types can be all of them
  * Producer (TopicPublisher)
  * Consumer (TopicSubscriber)
  * SharedConsumer 
  * DurableConsumer
  * SharedDurableConsumer

### 4.1.1. Run as a Message Producer to a JMS Queue (p2p)

An example of sending 10 messages to a JMS queue is as below:
```
% java -cp pulsar_exmples-1.0-SNAPSHOT-all.jar com.example.fastjms.JmsSimpleDemo \
  -f <path/to/conn.properties> \
  -dn persistent://mytenant/ns0/t1 \
  -dt queue
  -op producer \
  -n 10
```

### 4.1.2. Run as a Message Consumer to a JMS Queue (p2p)

An example of receiving 10 messages from a JMS queue is as below:

```
% java -cp pulsar_exmples-1.0-SNAPSHOT-all.jar com.example.fastjms.JmsSimpleDemo \
  -f <path/to/conn.properties> \
  -dn persistent://mytenant/ns0/t1 \
  -dt queue
  -op consumer \
  -n 10
```

### 4.1.3. Run as a Message Producer to a JMS Topic (pub/sub)

An example of publishing 10 messages to a JMS topic is as below:
```
% java -cp pulsar_exmples-1.0-SNAPSHOT-all.jar com.example.fastjms.JmsSimpleDemo \
  -f <path/to/conn.properties> \
  -dn persistent://mytenant/ns0/t2 \
  -dt topic
  -op producer \
  -n 10
```

### 4.1.4. Run as a Message Consumer to a JMS Topic (pub/sub)

An example of consuming 10 messages from a JMS topic is as below:
```
% java -cp pulsar_exmples-1.0-SNAPSHOT-all.jar com.example.fastjms.JmsSimpleDemo \
  -f <path/to/conn.properties> \
  -dn persistent://mytenant/ns0/t2 \
  -dt topic
  -op consumer \
  -n 10
```

## 4.2. JMS Queue Pattern Demo

The second FastJMS API demo program, **com.example.fastjms.queue_pattern.QueuePatternDemo**, is used to demonstrate JMS queue specific operations in JMS 1.1 spec. Its usage is as below:
```text
usage: QueuePatternDemo [-dn <arg>] [-f <arg>] [-h] [-ms <arg>] [-pn <arg>]
QueuePatternDemo Options:
  -dn <arg> JMS destination name: pulsar topic name
  -f <arg>  Configuration properties file.
  -h        Displays this help message.
  -ms <arg> JMS message selector: "selector pattern"
  -pn <arg> JMS Pattern Name - "QueueBrowser, QueueReceiver, QueueRequestor, QueueSender"
```

### 4.2.1. Send Messages Using JMS QueueSender (Fire and Forget)

An example of sending 10 messages to a JMS queue using JMS QueueSender is as below:
```
% java -cp pulsar_exmples-1.0-SNAPSHOT-all.jar com.example.fastjms.QueuePatternDemo \
  -f <path/to/conn.properties> \
  -dn persistent://public/default/qpatn \
  -pn QueueSender
```

The messages sent out have some properties including a sequence_id integer property. This will be used later with message selector for message filtering
```
- sent message: properties { sequence_id:0, jms_time:1634243810531 }; payload { pVMN9TKwXouGJGyu7CQ7 }
  ... ... 
- sent message: properties { sequence_id:9, jms_time:1634243813925 }; payload { aJrY0Wz92mzjNwr1yUJJ }
```


### 4.2.2. Receive Messages Using JMS QueueReceiver with Message Selector

An example of receiving 4 messages (via message selector) from a JMS queue using JMS QueueReceiver is as below:
```
% java -cp pulsar_exmples-1.0-SNAPSHOT-all.jar com.example.fastjms.QueuePatternDemo \
  -f <path/to/conn.properties> \
  -dn persistent://public/default/qpatn \
  -pn QueueReceiver
  -ms "sequence_id<=3"
```

### 4.2.3. Browse JMS Queue Messages Using JMS QueueBrowser

An example of browsing messages from a JMS queue using JMS QueueBrowser is as below:
```
% java -cp pulsar_exmples-1.0-SNAPSHOT-all.jar com.example.fastjms.QueuePatternDemo \
  -f <path/to/conn.properties> \
  -dn persistent://public/default/qpatn \
  -pn QueueBrowser
```

### 4.2.4. Send Messages and Wait for Responses using JMS QueueRequestor

JMS QueueRequestor example requires 2 parts: 
* one client application sends a messages to a JMS Queue and waits for a response
* one service application receives each message from the JMS Queue, processes it, and sends the response back

An example is as below:
```
% java -cp pulsar_exmples-1.0-SNAPSHOT-all.jar com.example.fastjms.QueuePatternDemo \
  -f <path/to/conn.properties> \
  -dn persistent://public/default/qpatn_req \
  -pn QueueRequestor
```

This example starts a service application in the background listening to a JMS queue. It also acts as a client application sending messages whose payload value is a random integer. When the service application receives a message, it times the message value by 100 and sends the new value as the response back to the client. The log output of this example is as below:
```
2021-10-14 20:29:45.154 [main] - QueueRequestor:: sending 10 messages from queue: persistent://public/default/qpatn_req
2021-10-14 20:29:46.113 [main] -   > sent message: {0}, response message: {0}
2021-10-14 20:29:46.230 [main] -   > sent message: {9}, response message: {900}
2021-10-14 20:29:46.340 [main] -   > sent message: {3}, response message: {300}
2021-10-14 20:29:46.451 [main] -   > sent message: {2}, response message: {200}
2021-10-14 20:29:46.561 [main] -   > sent message: {1}, response message: {100}
```

# 5. More Advanced DS FastJMS API Example

## 5.1. Multiple Queue Consumers - Message Consumption by "Keys"

Please refer to [here](advanced_pattern/topic_to_multiqueue) for more details.