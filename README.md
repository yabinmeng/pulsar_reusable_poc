- [1. Overview](#1-overview)
  - [1.1. Messaging and Processing Capabilities](#11-messaging-and-processing-capabilities)
- [2. Define and Deploy Demonstration Scenarios](#2-define-and-deploy-demonstration-scenarios)
  - [2.1. Define a Demo Scenario](#21-define-a-demo-scenario)
    - [2.1.1. Scenario Definition Property File](#211-scenario-definition-property-file)
    - [2.1.2. Post Deployment Script File](#212-post-deployment-script-file)
  - [2.2. Deploy a Demo Scenario](#22-deploy-a-demo-scenario)
    - [2.2.1. Execute Scenario Deployment](#221-execute-scenario-deployment)
    - [2.2.2. "client.conf" File](#222-clientconf-file)
  - [2.3. Terminate a Demo Scenario](#23-terminate-a-demo-scenario)
- [3. Example Application Repository and Deployment](#3-example-application-repository-and-deployment)
  - [3.1. Example Application Repository](#31-example-application-repository)
  - [3.2. Deploy Example Applications](#32-deploy-example-applications)
    - [3.2.1. Application ID Definition](#321-application-id-definition)
  - [3.3. Design and Develop Example Applications](#33-design-and-develop-example-applications)
- [4. Deploy the Infrastructure (Astra Streaming)](#4-deploy-the-infrastructure-astra-streaming)
- [5. Deploy the Infrastructure (Luna Streaming)](#5-deploy-the-infrastructure-luna-streaming)
  - [5.1. Deploy K8s Cluster](#51-deploy-k8s-cluster)
  - [5.2. Deploy the Pulsar cluster](#52-deploy-the-pulsar-cluster)
- [6. Appendix: Raw Data Source](#6-appendix-raw-data-source)
  - [6.1. IoT Use Case: Weather Sensor Telemetry Data](#61-iot-use-case-weather-sensor-telemetry-data)




# 1. Overview

This workshop is a framework that aims to provide a simplified yet complete end-to-end demo experience to showcase how to use Apache Pulsar as a powerful and unified messaging and streaming processing platform.

This framework automates the entire deployment and configuration process that ranges from the underlying infrastructure, the Pulsar cluster, to the example applications. In particular, this framework covers the automation in the following areas:

* Deploy the infrastructure. There are two choices here:
  1) Use [Astra Streaming](https://www.datastax.com/products/astra-streaming) as the managed infrastructure environment.
     * Since Astra Streaming is a managed environment, there is nothing to "deploy" here other than getting the right Astra Streaming environment connection properties file.
       
 2) Use [Luna Streaming](https://www.datastax.com/products/luna-streaming) as the self-deployed infrastructure environment. This includes deploying both a K8s cluster and a Pulsar cluster:
     * Deploy a K8s cluster of (any one of) the following types: *Kind*, *GKE*, *AKS*, and *EKS*
     * Deploy a Pulsar cluster in the above K8s cluster:
        * Be able to choose different security features (JWT authentication, TLS encryption, OAuth integration, etc.)
        * The support for Starlight APIs is enabled

* Deploy a set of messaging and streaming processing applications that can interact with the Apache Pulsar directly (either Astra Streaming or Luna Streaming). These applications are written using different sets of messaging and streaming processing APIs and/or protocols.
 * If the application demo requires Pulsar interaction with a 3rd party external system (e.g. Elasticsearch, C*, etc.), that system will be also deployed, the process of which may or may not be fully automated and needs to be evaluated on a case-by-case basis.

## 1.1. Messaging and Processing Capabilities

In this framework, we want to showcase a comprehensive list of messaging and streaming processing capabilities of Apache Pulsar, as listed below:

1) Basic message producing
2) Basic message with different subscription types
3) Complex message schema type (e.g. AVRO) processing
4) Message redelivery
5) Dead letter topic processing
6) Message schema compatibility processing
7) Message transaction processing

# 2. Define and Deploy Demonstration Scenarios

This framework is driven by demonstration (demo) scenarios. In one demo scenario, you define what Pulsar example applications you want to showcase and on what platform (Astra Streaming vs Luna Streaming). Then the whole scenario deployment is handled automatically. If the underlying K8s and/or Pulsar cluster is required (e.g. for Luna Streaming), it will also be handled by the scenario deployment script.

All demo scenarios are defined under the **scenarios** folder and each scenario must have a corresponding sub-folder. The tree structure below shows an example of having 2 defined demo scenarios.

```
$ tree scenarios/
scenarios/
├── demo-scenario-1
│   ├── post_deploy.sh
│   └── scenario.properties
├── demo-scenario-2
│   ├── post_deploy.sh
│   └── scenario.properties
├── deployScenario.sh
└── terminateScenario.sh
```

## 2.1. Define a Demo Scenario

Each demo scenario must have a main definition properties file, `scenario.properties`, in its corresponding subfolder. Optionally, it can also have a post deployment bash script, `post_deploy.sh`.
```
scenarios/demo-scenario-1/
├── post_deploy.sh
└── scenario.properties
```

### 2.1.1. Scenario Definition Property File

The `scenarios.properties` file is the core file to define a demo scenario. An example of its content can be found from [scenarios/demo-scenario-1/scenario.properties](scenarios/demo-scenario-1/scenario.properties)

The description of the property items in the `scenario.properties` file are quite straightforward. The only thing that needs a bit more attention is the following property item:
```
scenario.app.ids=nat_prod,nat_cons
```

The above property item `scenario.app.ids` defines the example applications to be deployed as part of this scenario. The value is a set of the example application ID separated by comma.

In the chapter of *Deploy Example Applications*, we'll explain more about the application ID and how it is linked to the actual example application code.

### 2.1.2. Post Deployment Script File

In some cases after the deployment is done, you may want to execute some extra steps before running the example applications. For example, you may want to create the required Pulsar namespaces and topics.

You can put such tasks into the post deployment script, `post_deploy.sh`. The main demo scenario script, `deployScenario.sh`, will call this script as the last step, so those tasks defined in this script will be automatically executed.

## 2.2. Deploy a Demo Scenario

The deployment of a demo scenario is handled by the following bash scripts in the **scenario** folder.

**`deployScenario.sh`**
```
Usage: deployScenario.sh [-h]
                        -scnName <scenario_name>
                        [-depAppOnly]
      -h : Show usage info
      -scnName : Demo scenario name.
      -depAppOnly : (Optional) Skip cluster deployment and only deploy applications.
```

  * The parameter `-scnName` is mandatory, and it must match one of the demo scenario sub-folder names. Otherwise, the script will complain not being able to find a corresponding demo scenario.
  * The parameter `-depAppOnly` is used when only example application deployments are needed and the deployment for the underlying K8s cluster and Pulsar cluster is not necessary.

### 2.2.1. Execute Scenario Deployment

When executing the deployment for a demo scenario, the main deployment script, `deployScenario.sh`, will execute a series of steps and generate corresponding logs.

1) For Astra Streaming deployment, the deployment script will prompt to download the corresponding `client.conf` file that is used to connect to an Astra Streaming tenant.
2) For Luna Streaming deployment, the deployment script will execute a lot more tasks:
   * Deploy a K8s cluster of your choice
   * Deploy a Pulsar cluster on the above K8s cluster
      * This step also generate a `client.conf` file for connecting to the deployed Pulsar cluster
   * Do K8s port forwarding to local host for Pulsar proxy service ports
3) Deploy a set of example applications
4) Execute post deployment script if there is one

Each of the above major steps has its own log file. All the log files are generated under the `scenarios/logs` folder. For each deployed demo scenarios, it has the following set of log files generated during the deployment execution:
* Main deployment log file: `deploy_<scenario_name>_<start_timestamp>_main.log`
* K8s cluster deployment log file (only for Luna Streaming): `deploy_<scenario_name>_<start_timestamp>_k8s_deploy.log`. The later chapter of ***Deploy a K8s Cluster*** has more details about K8s deployment using this framework.
* Pulsar cluster deployment log file (only for Luna Streaming): `deploy_<scenario_name>_<start_timestamp>_pulsar_deploy.log` The later chapter of ***Deploy a Pulsar Cluster*** has more details about K8s deployment using this framework.
* Pulsar proxy service port forwarding log file (only for Luna Streaming): `deploy_<scenario_name>_<start_timestamp>_port_forward.log`
* Post deployment log file (if `post_deploy.sh` file exists): `deploy_<scenario_name>_<start_timestamp>_post_deploy.log`

**NOTE**: The example application deployment doesn't have its own log file. It is part of the main log file.

### 2.2.2. "client.conf" File

In order for the deployed example applications of a particular demo scenario to connect to the target Apache Pulsar cluster (either Astra Streaming or Luna Streaming deployment mode), the deployment script will put a file, `client.conf` in the demo scenario folder, `scenarios/<scenario_name>`

This file follows the exact same format of the regular Pulsar client connection configuration file (see [Pulsar Client Doc](https://pulsar.apache.org/docs/2.10.x/reference-configuration/#client))

## 2.3. Terminate a Demo Scenario

The termination of a demo scenario is handled by the following bash scripts in the **scenario** folder.

**`terminateScenario.sh`**
```
Usage: terminateScenario.sh [-h]
                           -scnName <scenario_name>
                           [-keepK8s]
      -h : Show usage info
      -scnName : Demo scenario name.
      -keepK8s : Whether to keep K8s cluster.
```
  * The parameter `-scnName` is mandatory, similar to that in the deployment scenario.
  * The parameter `-keepK8s` is you want to keep the underlying K8s cluster without tearing it down (Luna Streaming only).
     * Note that this termination script will always tear down the Pulsar cluster against which the applications are running (Luna Streaming only).

Like `deployScenario.sh` file, it also executes a series of sub-tasks and generates corresponding log files under `scenarios/logs` folder, but the file names of all logs related with scenario termination starts with `term_` instead of `deploy_`.

# 3. Example Application Repository and Deployment

## 3.1. Example Application Repository

This framework includes a list of reusable code repositories as the example applications that can be deployed in various demo scenarios. Based on the unique requirement of each demo scenario, different sets of example applications can be chosen and included for a particular demo scenario.

The example code repository is under directory `application_code` and further divided into subdirectories by the programming language (e.g. Java or Python)

## 3.2. Deploy Example Applications



### 3.2.1. Application ID Definition


This framework contains a list of example application codes under the folder [application_code](application_code). These example applications are organized by the programming languages they're written in. Currently, only Java and Python based applications are included in this framework.

In each demonstration scenario, one or more of these example applications may be included and deployed, depending on the requirement of the


## 3.3. Design and Develop Example Applications


The example applications in this framework are developed to reflect the messaging and streaming processing capabilities as we discussed above. The example applications can reflect the same capability using different programming APIs and/or message processing protocols:
* Native Pulsar client API
* Pulsar Spring Boot API
* JMS API (using S4J)
* Native Kafka client API
* Native RabbitMQ client API


**Guiding Principle**: The design of the example applications (in this framework) need to be modularized as much as possible. This means that
1) The functionality of each application needs to be scoped concisely (single-functionality as much as possible).
2) Different applications should have overlapping functionalities to the minimum if possible.


# 4. Deploy the Infrastructure (Astra Streaming)


# 5. Deploy the Infrastructure (Luna Streaming)


## 5.1. Deploy K8s Cluster


## 5.2. Deploy the Pulsar cluster


# 6. Appendix: Raw Data Source

All demo scenarios in this framework will be based on raw data sources that reflect realistic actual use cases as much as possible. Currently, the following use cases are supported.

## 6.1. IoT Use Case: Weather Sensor Telemetry Data

This data source represents the data reading of IoT devices that contain various types of sensors for a specific range of time (e.g. 1 week). The available **Sensor Types** are listed as below. For a more detailed description of this data source, please check from [here](https://www.kaggle.com/datasets/garystafford/environmental-sensor-data-132k).
* Carbon monoxide
* Humidity (%)
* Light detection
* Liquefied petroleum gas (LPG)
* Motion detection
* Smoke
* Temperature

A copy of this data can be found from [raw_data/sensor_telemetry.csv](raw_data/sensor_telemetry.csv)
