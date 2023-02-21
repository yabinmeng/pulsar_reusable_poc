- [1. Overview](#1-overview)
- [2. Demonstration (Demo) Scenarios](#2-demonstration-demo-scenarios)
  - [2.2. Messaging and Processing Capabilities](#22-messaging-and-processing-capabilities)
  - [2.1. Raw Data Sources](#21-raw-data-sources)
    - [2.1.1. IoT Use Case Raw Data Source](#211-iot-use-case-raw-data-source)
  - [Deploy a Demo Scenario](#deploy-a-demo-scenario)
- [3. Deploy Example Applications](#3-deploy-example-applications)
  - [3.1. Design and Develop Example Applications](#31-design-and-develop-example-applications)
  - [3.2. Deploy Applications](#32-deploy-applications)
- [4. Deploy the Infrastructure (Astra Streaming)](#4-deploy-the-infrastructure-astra-streaming)
- [5. Deploy the Infrastructure (Luna Streaming)](#5-deploy-the-infrastructure-luna-streaming)
  - [5.1. Deploy K8s Cluster](#51-deploy-k8s-cluster)
  - [5.2. Deploy the Pulsar cluster](#52-deploy-the-pulsar-cluster)


# 1. Overview

This workshop is a framework that aims to provide a much simplified, complete, end-to-end demo experience to showcase how to use Apache Pulsar as a powerful and unified messaging and streaming processing platform. 

This framework automates the entire deployment and configuration process that ranges from the underlying infrastructure, the Pulsar cluster, to the applications. In particular, the following areas will be covered by this framework:

* Deploy the infrastructure. There are two choices here:
  
  1) Use [Astra Streaming](https://www.datastax.com/products/astra-streaming) as the managed infrastructure environment. This is the easy route and recommended.
         
  2) Use [Luna Streaming](https://www.datastax.com/products/luna-streaming) as the self-deployed infrastructure environment. This is the harder route but if needed, this framework supports the following deployment options:

      * Deploy a K8s cluster of (any one of) the following types: *Kind*, *GKE*, *AKS*, *EKS*
     * Deploy a Pulsar cluster in the above K8s cluster: 
       * Be able to choose different security features (JWT authentication, TLS encryption, OAuth integration, etc.)
       * The support for Starlight APIs support is enabled

* Deploy a set of messaging and streaming processing applications that can interact with Apache Pulsar directly. These applications are written using different sets of messaging and streaming processing APIs and/or protocols.
  
* If the application demo requires Pulsar interaction with a 3rd party external system (e.g. Elasticsearch, C*, etc.), that system will be also deployed. This needs to be evaluated on case-by-case basis and will be automated as much as possible. 

# 2. Demonstration (Demo) Scenarios

This framework is driven by predefined demo scenarios. All scenarios are defined in the **scenarios** folder and each one has its own corresponding sub-folder.

## 2.2. Messaging and Processing Capabilities

Throughout these demo scenarios, we want to showcase a comprehensive list of messaging and streaming processing capabilities of Apache Pulsar, as listed below:

1) Basic message producing
2) Basic message with different subscription types
3) Complex message schema type (e.g. AVRO) processing
4) Message redelivery
5) Dead letter topic processing
6) Message schema compatibility processing
7) Message transaction processing

## 2.1. Raw Data Sources

All demo scenarios in this framework will be based on raw data sources that reflect realistic actual use cases as much as possible. Currently, the following use cases are supported.

### 2.1.1. IoT Use Case Raw Data Source

This data source represents the data reading of IoT devices that contain various types of senors for a specific range of time (e.g. 1 week). The available **Sensor Types** are listed as below. For more detailed description of this data source, please check from [here](https://www.kaggle.com/datasets/garystafford/environmental-sensor-data-132k).
* Carbon monoxide 
* Humidity (%)
* Light detection
* Liquefied petroleum gas (LPG) 
* Motion detection
* Smoke
* Temperature

A copy of this data can be found from [raw_data/sensor_telemetry.csv](raw_data/sensor_telemetry.csv)

## Deploy a Demo Scenario

Each demo scenario aims to showcase one or more messaging/steaming processing capabilities depending on the user's choice. The deployment of a demo scenario is a complete, automated, end-to-end experience to an end user. It covers the following aspects of the deployment: 
1) If needed (e.g. for Luna Streaming), deploy an infrastructure environment (a K8s cluster)
2) If needed (e.g. for Luna Streaming), deploy a Pulsar cluster
3) Deploy a chosen set of messaging/streaming processing applications to run against the Pulsar cluster.

Each demo scenario has its own definition properties file, **demo_deploy.prop**, that determines how the demo will be deployed, including both the underlying infrastructure and application codes.
* The execution of the demo scenario will be triggered by a bash script, **start_demo.sh**.
* If needed, the infrastructure resources deployed for demo scenario can be terminated using the bash script, **term_demo.sh**.

Below is an example of how the scenario demo folder looks like:
```
scenarios/
└── demo-scenario-1
    ├── demo_deploy.prop
    ├── start_demo.sh
    └── term_demo.sh
```



# 3. Deploy Example Applications

This framework contains a list of example application codes under the folder [application_code](application_code). These example applications are organized by the programming languages they're written. Currently, only Java and Python based applications are included in this framework.

In each demonstration scenario, one or more of these example applications may be included and deployed, depending on the requirement of the

## 3.1. Design and Develop Example Applications

The example applications in this framework are developed to reflect the messaging and streaming processing capabilities as we discussed above. The example applications can reflect the same capability using different programming APIs and/or message processing protocols:
* Native Pulsar client API
* Pulsar Spring Boot API
* JMS API (using S4J)
* Native Kafka client API
* Native RabbitMQ client API

**Guiding Principle**: The design of the example applications (in this framework) need to modularized as much as possible. This means that 
1) The functionality of each application need to be scoped concisely (single-functionality as much as possible). 
2) Different applications should have overlapping functionalities to the minimum if possible.

## 3.2. Deploy Applications

# 4. Deploy the Infrastructure (Astra Streaming)

# 5. Deploy the Infrastructure (Luna Streaming)

## 5.1. Deploy K8s Cluster

## 5.2. Deploy the Pulsar cluster




