# Overview

This workshop is a framework that aims to provide a much simplified, complete, end-to-end demo experience to showcase how to use Apache Pulsar as a powerful and unified messaging and streaming processing platform. 

This framework automates the entire deployment and configuration process that ranges from the underlying infrastructure, the Pulsar cluster, to the applications. In particular, the following areas will be covered by this framework:

* Deploy the infrastructure. There are two choices here:
  
  1) Use Astra Streaming as the managed infrastructure environment. This is the easy route and recommended.
         
  2) Use Luna Streaming as the self-deployed infrastructure environment. This is the harder route but if needed, this framework supports the following deployment options:

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

# Demonstration Scenarios

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

## IoT Use Case

All demo scenarios regardless of the underlying Pulsar deployment or the application API, are based on a typical IoT use case about environmental sensor telemetry data ([source link](https://www.kaggle.com/datasets/garystafford/environmental-sensor-data-132k)).

The raw source data we're going to use in these demo scenarios represent the sensor data reading of the following types for a specific range of time (from 07/11/2020 to 07/18/2011). 
* Carbon monoxide 
* Humidity (%)
* Light detection
* Liquefied petroleum gas (LPG) 
* Motion detection
* Smoke
* Temperature

A copy of this data can be found from [raw_data/sensor_telemetry.csv](raw_data/sensor_telemetry.csv)

## Messaging and Processing Capabilities


# Develop and Deploy Applications


# Deploy the Infrastructure

## Use Astra Streaming as the Infrastructure

## Use Luna Streaming as the Infrastructure

### Deploy K8s Cluster

### Deploy the Pulsar cluster




