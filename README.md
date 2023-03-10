# 1. Overview

## 1.1. What is this repository about?

This repository is designed to make it easy to learn, use, and showcase Apache Pulsar as a powerful and **unified** messaging and streaming processing platform. There are three major components at the core of this repository:

1. A list of highly reusable Apache Pulsar example *client applications* and *functions* that are written in different languages and APIs (see detailed doc [here]()). Examples of the following APIs are included in this repository:
   * Native Pulsar client API (nativeapi)
   * Starlight for JMS API (s4j)
   * Native Kafka client API
   * Native RabbitMQ client API

2. A simple configuration based tool to launch a K8s cluster and an Apache Pulsar cluster.
   * It also allows using Astra Streaming or any existing Apache Pulsar cluster as the running environment.

3. A "scenario" based mechanism that allows you to run any typical messaging processing patterns by simply choosing desired example applications from the reusable application list. 

We'll go through each of these components with more details in the rest of the documents in this repository.

## 1.2. Whom is this repository designed for? 

First, this repository is meant for people who want to learn how to write Apache Pulsar client applications and functions for various messaging and streaming processing use cases. **Note** that the "client applications" can be native to other messaging and streaming technologies such as JMS, Kafka, and RabbitMQ; but they all can connect to an Apache Pulsar cluster. This is exactly why Apache Pulsar is so powerful as a unified messaging and streaming platform.

Second, this repository is meant for people who want to run a quick end-to-end demo of using Apache Pulsar to address certain messaging and streaming processing use cases; but don't want to go through all the details of how to properly prepare, install, and configure an Apache Pulsar cluster. This repository greatly simplifies the task of "cluster provisioning" via a simple configuration based, fully automated script.

---

# 2. Core Concept and Quick Start Guide

Depending on your purpose of using this repository, several quick start guides are provided:

* (**Core Concept**) [Introduction and Overall Framework Structure](doc/overall_structure.md)

* (**Quick Start Guide**) [Using and Building Example Application](doc/app_code.md)

* (**Quick Start Guide**) [Building and Deploying Scenarios](doc/understand_scenario.md)