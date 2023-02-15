- [1. Overview](#1-overview)
- [2. Demonstration Scenarios](#2-demonstration-scenarios)
  - [2.1. Use Cases (Raw Data Sources)](#21-use-cases-raw-data-sources)
    - [2.1.1. IoT Use Case](#211-iot-use-case)
  - [2.2. Messaging and Processing Capabilities](#22-messaging-and-processing-capabilities)
- [3. Develop and Deploy Applications](#3-develop-and-deploy-applications)
- [4. Deploy the Infrastructure](#4-deploy-the-infrastructure)
  - [4.1. Use Astra Streaming as the Infrastructure](#41-use-astra-streaming-as-the-infrastructure)
  - [4.2. Use Luna Streaming as the Infrastructure](#42-use-luna-streaming-as-the-infrastructure)
    - [4.2.1. Deploy K8s Cluster](#421-deploy-k8s-cluster)
    - [4.2.2. Deploy the Pulsar cluster](#422-deploy-the-pulsar-cluster)


# 1. Overview

This workshop is a framework that aims to provide a much simplified, complete, end-to-end demo experience to showcase how to use Apache Pulsar as a powerful and unified messaging and streaming processing platform. 

This framework automates the entire deployment and configuration process that ranges from the underlying infrastructure, the Pulsar cluster, to the applications. In particular, the following areas will be covered by this framework:

* Deploy the infrastructure. There are two choices here:
  
  1) Use [Astra Streaming](https://www.datastax.com/products/astra-streaming) as the managed infrastructure environment. This is the easy route and recommended.
         
  2) Use [Luna Streaming](https://www.datastax.com/products/luna-streaming) as the self-deployed infrastructure environment. This is the harder route but if needed, this framework supports the following deployment options:

      * Deploy a K8s cluster of (any one of) the following types:
        * Kind
        * GKE
        * AKS
        * EKS
     * Deploy a Pulsar cluster in the above K8s cluster: 
       * Be able to choose different security features (JWT authentication, TLS encryption, OAuth integration, etc.)
       * Starlight API support is enabled
          * Starlight for Kafka (S4K) API
          * Starlight for RabbitMQ (S4R) API
          * Broker side message filtering
          * ***Note***: Starlight for JMS (S4J) API is a client side API and nothing needs to be done on the server side.

* Deploy a set of messaging and streaming processing applications that can interact with Apache Pulsar directly. 
   * These applications are written using different sets of messaging and streaming processing APIs and/or protocols.
      * Native Pulsar client API
      * Pulsar Spring Boot API
      * JMS API (using S4J)
      * Native Kafka client API
      * Native RabbitMQ client API
   * These applications can be written in different programming languages, such as Java and Python. 
  
* If the application demo requires Pulsar interaction with a 3rd party external system (e.g. Elasticsearch, C*, etc.), that system will be also deployed. This needs to be evaluated on case-by-case basis and will be automated as much as possible. 

# 2. Demonstration Scenarios

From the user's perspective, this framework will be driven by pre-defined demo scenarios. All scenarios are defined in the **scenarios** folder and each one has its own sub-folder:
* Each demo scenario has a global definition properties file, **demo_deploy.prop**, that determines how the demo (both infrastructure and application code) will be deployed. 
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

## 2.1. Use Cases (Raw Data Sources)

All demo scenarios in this framework will be based on raw data sources that match actual use cases as much as possible. Currently, the following use cases are supported.

### 2.1.1. IoT Use Case

This use case (raw data source) represents the data reading of IoT devices that contain various types of senors for a specific range of time (e.g. 1 week). The available **Sensor Types** are listed as below. For more detailed description of this data source can be found from [here](https://www.kaggle.com/datasets/garystafford/environmental-sensor-data-132k).
* Carbon monoxide 
* Humidity (%)
* Light detection
* Liquefied petroleum gas (LPG) 
* Motion detection
* Smoke
* Temperature

A copy of this data can be found from [raw_data/sensor_telemetry.csv](raw_data/sensor_telemetry.csv)

## 2.2. Messaging and Processing Capabilities

With the above use case, we want to demonstrate a comprehensive list of messaging and streaming processing capabilities as listed below:

* 

# 3. Develop and Deploy Applications


# 4. Deploy the Infrastructure

## 4.1. Use Astra Streaming as the Infrastructure

## 4.2. Use Luna Streaming as the Infrastructure

### 4.2.1. Deploy K8s Cluster

### 4.2.2. Deploy the Pulsar cluster




