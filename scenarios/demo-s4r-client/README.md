# 1. Overview

## 1.1. What is this scenario about?
To show RabbitMQ clients can connect and exchanges messages with Pulsar.  Existing RabbitMQ clients, with only configuration changes, like "host", "port", "virtual_host" parameters updated to use Pulsar with Starlight for RabbitMQ.  

For more details on Starlight for RabbitMQ see these links  
[Astra Streaming with RabbitMQ](https://docs.datastax.com/en/streaming/streaming-learning/use-cases-architectures/starlight/rabbitmq/index.html)  

[Luna Streaming with RabbitMQ](https://docs.datastax.com/en/streaming/luna-streaming/2.10_1.x/components/starlight-for-rabbitmq.html)

# 2. Prerequisite
To deploy and run this scenario, Starlight for RabbitMQ must be enabled on the running Pulsar instance or Astra Streaming instance.  See the documentation for [Astra Streaming](https://docs.datastax.com/en/streaming/streaming-learning/use-cases-architectures/starlight/rabbitmq/index.html) and [Luna Streaming](https://docs.datastax.com/en/streaming/luna-streaming/2.10_1.x/components/starlight-for-rabbitmq.html) on how to setup and enable Starlight for RabbitMQ.  

With **Astra Streaming**,it is super easy to enable RabbitMQ.  The Astra Streaming UI **Connect** tab has a RabbitMQ option.  Click the option to enable and download a pre-configured **rabbitmq.conf** for this scenario's clients.  

# 3. Assumptions
A running Pulsar cluster with Starlight for RabbitMQ extension enabled and running properly, is required for this scenario.  It will not create a Pulsar cluster configuration file or enable RabbitMQ options.  Those are beyond the scope of this scenario.

# 4. Deploy and Run RabbitMQ Clients
## 4.1. Update scenario.properties file
Review the **scenario.properties** file to ensure the proper RabbitMQ and Pulsar parameters are set. 

Update the queue name, num of messages, and RabbitMQ conf filename for example.
```
# RabbitMQ on Luna or Pulsar example parameters
#scenario.app.param.s4r_rabbitmq_queue_producer -q my_s4r_queue -num 10 -c appconf/rabbitmq.conf
#scenario.app.param.s4r_rabbitmq_queue_consumer -q my_s4r_queue -num 100 -c appconf/rabbitmq.conf

``` 
## 4.1.1. Client Runtime options
These options for available for both Producer and Consumer RabbitMQ clients.
```

-useAstra true if clients are connecting to Astra Streaming with RabbitMQ enabled. SSL will also be enabled.
-q or -s4rqueue to specify the RabbitMQ queue to produce or consume too
-num or numMsg to specify the number of messages to produce or consume then stop executing 
-c or rabbitmqconf to specify the rabbitmq.conf file to use.  See example file in appconf directory
-m or s4rmessage to specify a message string to send with the Producer.  This option is ignored for consumers
```
## 4.2. Download Astra Streaming RabbitMQ conf file
If using Astra Streaming, download the **rabbitmq.conf** file from the Astra UI.  Place the conf file in the appconf/rabbitmq.conf  

## 4.3. Deploying RabbitMQ clients
```
cd demo-s4r-client
../deployScenario.sh -scnName demo-s4r-client -depAppOnly
``` 
**Note**  the **-depAppOnly** option is enabled.  Deployment errors will occur if this option is not enabled.
## 4.4. Running the RabbitMQ clients
```
cd demo-s4r-client
appexec/run_s4r_rabbitmq_queue_consumer.sh
appexec/run_s4r_rabbitmq_queue_producer.sh
```
## 4.4.1. Example rabbitmq.conf file
Below is an example rabbitmq.conf file to connect to a localhost Pulsar cluster.
```
username=
password=
host=localhost
port=5672
virtual_host=public/default
```
For Astra Streaming RabbitMQ conf file, see the Astra UI **Connect** tab to download a pre-populate file for your Astra Streaming tenant.
