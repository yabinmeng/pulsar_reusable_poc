# 1. Overview

This repository is designed to include a plethora of example codes to showcase how to program with Apache Pulsar as a unified messaging and streaming platform. The objective is to help jumpstart quickly those who want to learn Pulsar programming on varied topics related with different (and typical) messaging and streaming processing capabilities and/or use cases.

The example codes in this repository try to be representative as much as possible from different perspectives:

1. The examples are written in different programming languages such as Java, Python, Go, C++, etc.

2. The examples are written using different APIs (native Pulsar client API, JMS API, native Kafka client API, etc.)
   
3. The examples are written to reflect fundamental messaging processing capabilities, some examples of which are as below
   * Basic message producing
   * Basic message consuming
   * Message redelivery and dead letter topic
   * Message schema processing
   * Message transaction processing
   * etc.
  
4. The examples cover both message publishing/consuming client applications and Pulsar functions (aka, application types)

# 2. Folder Structure

The folder structure below shoes a high level structure of how the example applications in this repository are organized in a way for easy management and lookup of the application codes (e.g. based on their types, languages, etc.).

```
application_code
├── client_apps
│   ├── c++
│   ├── go
│   ├── java
│   └── python
└── functions
    ├── java
    └── python
```

# 3. Application List

The following table shows a full list of applications that are currently maintained in the example application repository:

| Name | Type | Language | Description |
| ---- | ---- | -------- | ----------- |
| native_producer_full_config | pub/sub client app | java | A Pulsar producer written with native Pulsar client API that has full support for Pulsar client and consumer configuration items |
| native_consumer_full_config | pub/sub client app | java | A Pulsar consumer written with native Pulsar client API that has full support for Pulsar client and consumer configuration items |
s4j_queue_sender | pub/sub client app | java | A JMS message sender to a JMS queue using the Starlight for JMS (s4j) API |
s4j_queue_receiver | pub/sub client app | java | A JMS message receiver from a JMS queue using the Starlight for JMS (s4j) API |
s4j_topic_producer | pub/sub client app | java | A JMS message producer to a JMS topic using the Starlight for JMS (s4j) API |
s4j_topic_subscriber | pub/sub client app | java | A JMS message subscriber from a JMS topic using the Starlight for JMS (s4j) API |
s4k_kafka_producer | pub/sub client app | java | A Kafka message producer to a topic via Starlight for Kafka (s4k) protocol handler |
s4k_kafka_consumer | pub/sub client app | java | A Kafka message consumer from a topic via Starlight for Kafka (s4k) protocol handler |
s4r_rabbitmq_queue_producer | pub/sub client app | java | A RabbitMQ message producer to a queue via Starlight for RabbitMQ (s4r) protocol handler |
s4r_rabbitmq_queue_consumer | pub/sub client app | java | A RabbitMQ message consumer from a queue via Starlight for RabbitMQ (s4r) protocol handler |
springboot_producer_simple | pub/sub client app | java | A simple Pulsar producer written using Spring Boot for Pulsar API |
springboot_consumer_simple | pub/sub client app | java | A simple Pulsar consumer written using Spring Boot for Pulsar API |
add-metadata | Pulsar function | java | A simple Pulsar function to add a metadata (message property) to each incoming new message and send it to a new topic |


# Appendix A: Build an Example Application

In order to make the example applications maintained in this repository as consistent as possible (so that we can deliver a unified and uniform experience as much as possible), there are certain guidelines to follow. 

The guidelines would be a bit different based on the example application type (client application vs function) and the application language (java, python, etc.), and we'll go through each of them in the following sections.

## A.1 Build a Java based client application

At the moment, the Java based client applications in this repository are all command line (CLI) based. This means the capability of processing command line parameters is required. 

To make this simpler, we create a common parent class, `PulsarWorkshopCmdApp` for all Java based client applications. When you add a new application, please extend this class and implement the 3 abstract methods.

```
abstract public class PulsarWorkshopCmdApp {
    ...  
    public abstract void processInputParams() throws InvalidParamException;
    public abstract void runApp();
    public abstract void termApp();
    ...
}
```

Please **NOTE** that this class already supports several CLI parameters as the "fundamental" ones that all Java client example application need to have. For example, the Pulsar topic name and the Pulsar client connection file (client.conf) are the two obvious examples of such fundamental parameters.

For a template of how a new Java based example client application can be created, please follow the following example:

* [nativeapi_producer_full_config](../application_code/client_apps/java/native_producer_full_config/)