# Overview

## What is this repository about?

This repository is designed to make it easy to learn, use, and showcase Apache Pulsar as a powerful and **unified** messaging and streaming processing platform. There are three major components at the core of this repository:

1. A list of highly reusable Apache Pulsar example *client applications* and *functions* that are written in different languages and APIs (see detailed doc [here]()). Examples of the following APIs are included in this repository:
   * Native Pulsar client API (nativeapi)
   * Starlight for JMS API (s4j)
   * Starlight for Kafka API (s4k)
   * Starlight for RabbitMQ API (s4r)

2. A simple configuration based tool to launch a K8s cluster and an Apache Pulsar cluster (see detailed doc [here]()).
   * It also allows using Astra Streaming or any existing Apache Pulsar cluster as the running environment.

3. A "scenario" based mechanism that allows you to run any typical messaging processing patterns by simply choosing desired example applications from the reusable application list (see detailed doc [here]()). 

We'll go through each of these components with more details in the rest of the documents in this repository.

## Whom is this repository designed for? 

First, this repository is meant for people who want to learn how to write Apache Pulsar client applications and functions for various messaging and streaming processing use cases. **Note** that the "client applications" can be native to other messaging and streaming technologies such as JMS, Kafka, and RabbitMQ; but they all can connect to an Apache Pulsar cluster. This is exactly why Apache Pulsar is so powerful as a unified messaging and streaming platform.

Second, this repository is meant for people who want to run a quick end-to-end demo of using Apache Pulsar to address certain messaging and streaming processing use cases; but don't want to go through all the details of how to properly prepare, install, and configure an Apache Pulsar cluster. This repository greatly simplifies the task of "cluster provisioning" via a simple configuration based, fully automated script.

---

# Quick start guides

Depending on your purpose of using this repository, several quick start guides are provided:

* [Quick start guide for using and building example codes](doc/app_code.md)

* [Quick start guide for building and running a demo scenario with Astra Streaming (or any exiting Luna Streaming/OSS Pulsar cluster)](doc/scenario_astra.md)

* [Quick start guide for building and running a demo scenario with a new Luna Streaming Pulsar cluster](doc/scenario_luna_new.md)