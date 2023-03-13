# 1. Overview

## 1.1. What is this repository about?

The `Reusable PoC framework` (or simply the framework) as presented in this GitHub repository is designed to make it easy to use and showcase Apache Pulsar as a powerful and **unified** messaging and streaming processing platform for common messaging and streaming processing use cases (aka, `PoC scenario`). There are three major components at the core of this framework:

1. A repository of reusable example Apache Pulsar codes, *client applications* and *functions*, that are written in different languages and APIs.

2. A simple configuration based (PoC) scenario building tool that allows you to build a reusable scenario that represents a typical messaging and streaming processing scenario.

3. A reusable (PoC) scenario repository for people who want to run the scenario directly without worrying about creating the underlying infrastructure and/or writing application codes.

**NOTE**: It has to be pointed out though that this repository is `NOT` a Pulsar tutorial. It won't teach you neither Apache Pulsar basics nor messaging and streaming processing fundamentals. 

## 1.2. Whom is this repository designed for? 

First and foremost, this framework is meant to `enable the enablers`. This framework provides a simple and ready-to-use tool to the streaming Data Architects and/or Solutions Architects and allow them to easily showcase the power of Apache Pulsar system without spending lengthy time on environment setup and code writing from the scratch.

Secondly, this framework offers the capability of building a PoC demonstration scenario in a fast and simple configuration based approach. The built scenario follows the common format and workflow and will automatically become a part of a reusable scenario repository. This helps avoid the `reinvent the wheel` problem; and also makes possible of quick-iteration when needed. This will greatly expedite and enhance the customer interaction experience.

Last but not the least, this framework maintains reusable Pulsar example codes. For end-users who have basic understanding of Apache Pulsar, this framework offers them a great way to enhance and expand their knowledge via concrete examples. 

---

# 2. Core Concept and Quick Start Guide

Depending on your purpose of using this repository, several quick start guides are provided:

* [Introduction and Overall Framework Structure](doc/overall_structure.md)

* [Building Example Applications](doc/app_code.md)

* [Building and Deploying Scenarios](doc/understand_scenario.md)

---

For those who want to jump right into the reusable demo scenarios, please check the following link: 

* [Reusable Demo Scenario Repository](doc/reusable_scn_repos.md)