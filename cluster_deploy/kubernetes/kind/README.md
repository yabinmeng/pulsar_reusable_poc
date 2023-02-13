# Overview

**Kind** is an easy way to deploy a "local" K8s cluster using Docker container "nodes". For more detailed information about Kind, please refer the official [document](https://kind.sigs.k8s.io/).

## Prerequisite

1. Make sure Docker engine is installed on your local machine. Please check [Docker Doc](https://docs.docker.com/engine/install/) for more details.

2. Adjust Docker resource limit to meet the recommended resource requirement of running a Kind K8s cluster on which a Pulsar cluster is deployed.
    * 6 CPU
    * 12 GB memory
    * 64 GB disk 

# Manage the Kind cluster

The automation scripts to create and delete a Kind (K8s) cluster are as below:
* Create the cluster: [../../../bash/k8s_kind_crtclstr.sh](../../../bash/k8s_kind_crtclstr.sh)
* Delete the cluster: [../../../bash/k8s_kind_delclstr.sh](../../../bash/k8s_kind_delclstr.sh)

## Create a Kind cluster

1. Follow the [procedure](https://kind.sigs.k8s.io/docs/user/quick-start/#installation) to install `Kind` on your local machine.

2. Run the following command to create a Kind cluster with a default name *kind*. If you want to create a cluster with a non-default name, use the `--name <cluster_name>` command option.
```
kind create cluster --config ./cluster-config.yaml
```

This created Kind cluster has 1 control plane node and 3 worker nodes. You can change the number of the control plane nodes and/or the worker nodes in the [cluster-config.yaml](cluster-config.yaml) file.

## Preload the docker image 

When deploying a container in the Kind cluster, the docker image for the container is fetched from the public docker image repository. Alternatively, you can load a docker image into the Kind cluster before using them to deploy containers. 

For example, the following command load the DataStax Luna Streaming docker image (version 2.10_3.1) into the previously created Kind cluster in advance.
```
kind load docker-image datastax/lunastreaming:2.10_3.1
```

### Verify the loaded docker image

Follow the following steps to verify whether a docker image has been successfully loaded in the Kind cluster

1. Get the Kind node name list
```
kind get nodes
```

2. Log in to any of the listed Kind nodes
```
docker exec -it <kind_node_name> bash
```

3. In the Kind node shell, run the following command to check the loaded docker image info
```
crictl images | grep <docker_image_name>
```

## Delete the Kind cluster

Run the following command to delete the above created Kind K8s cluster. For a cluster with a non-default name, the `--name <cluster_name>` option is needed.
```
kind delete cluster
```