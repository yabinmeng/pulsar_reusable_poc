#! /bin/bash

###
# Copyright DataStax, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
###

CUR_SCRIPT_FOLDER=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
PULSAR_WORKSHOP_HOMEDIR=$( cd -- "${CUR_SCRIPT_FOLDER}/../.." &> /dev/null && pwd )

source "${PULSAR_WORKSHOP_HOMEDIR}/_bash_utils_/utilities.sh"

### 
# This script is used to deploy a K8s cluster with 1 control plane node 
# and 3 worker nodes based on the selected k8s option. 
#
# Currently, the following K8s options are supported
# - kind
# - gke
# - aks
# - eks
# 

usage() {
   echo
   echo "Usage: deploy_k8s_cluster.sh [-h]"
   echo "                             -clstrName <cluster_name>"
   echo "                             -propFile <deployment_properties_file>"
   echo "       -h : Show usage info"
   echo "       -clstrName : K8s cluster name"
   echo "       -propFile  : K8s deployment properties file"
   echo
}

if [[ $# -eq 0 || $# -gt 4 ]]; then
   usage
   errExit 20
fi

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -clstrName) k8sClstrName=$2; shift ;;
      -propFile) k8sDeployPropFile=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; errExit 30 ;;
   esac
   shift
done

if ! [[ -f ${k8sDeployPropFile// } ]]; then
    echo "[ERROR] Can't find the provided K8s deployment properties file: \"${k8sDeployPropFile}\"!"
    errExit 40; 
fi

kubeCtlExistence=$(chkSysSvcExistence kubectl)
debugMsg "kubeCtlExistence=${kubeCtlExistence}"
if [[ ${kubeCtlExistence} -eq 0 ]]; then
    echo "[ERROR] 'kubectl' isn't installed on the local machine yet; please install it first!"
    errExit 50;
fi

if [[ -z ${k8sClstrName// } ]]; then
    dftClstrName=$(getPropVal ${k8sDeployPropFile} "k8s.cluster.name")
    if [[ -z ${dftClstrName// } ]]; then
        echo "[ERROR] K8s cluster name can't be empty! "
        errExit 60
    else
        k8sClstrName=${dftClstrName}
    fi
fi

k8sOpt=$(getPropVal ${k8sDeployPropFile} "k8s.deploy.option")
if [[ -z ${k8sOpt// } ]]; then
    echo "[ERROR] A K8s deployment option must be provided!"
    errExit 70
fi

if ! [[ " ${K8S_DEPLOY_OPTIONS[@]} " =~ " ${k8sOpt} " ]]; then
    echo "[ERROR] Invalid K8s deployment option value."
    echo "        Must be one of the following values: \"${K8S_DEPLOY_OPTIONS[@]}\""
    errExit 80
fi
echo "PULSAR_WORKSHOP_HOMEDIR is: ${PULSAR_WORKSHOP_HOMEDIR}"
k8sOptDeployHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/k8s/${k8sOpt}"

echo "============================================================== "
echo "= "
echo "= A \"${k8sOpt}\" based K8s cluster with name \"${clstrName}\" will be deployed ...  "
echo "= "

case ${k8sOpt} in

    kind)
        if ! [[ -f "${k8sOptDeployHomeDir}/kind_create.sh" ]]; then
            echo "[ERROR] Can't find the script file to deploy a 'kind' K8s clsuter !"
            errExit 90; 
        fi

        preLoadImage=$(getPropVal ${k8sDeployPropFile} "kind.image.preload")
        if [[ "${preLoadImage}" == "true" ]]; then
            pulsarImage=$(getPropVal ${k8sDeployPropFile} "kind.pulsar.image")
            source ${k8sOptDeployHomeDir}/kind_create.sh -clstrName ${k8sClstrName} -preLoadImage ${pulsarImage}
        else
            source ${k8sOptDeployHomeDir}/kind_create.sh -clstrName ${k8sClstrName}
        fi
        ;;

    gke)
        echo "Deploying GKE cluster via script: $k8sOptDeployHomeDir"
        if ! [[ -f "${k8sOptDeployHomeDir}/gke_create.sh" ]]; then
            echo "[ERROR] Can't find the script file to deploy a 'gke' K8s clsuter!"
            errExit 100; 
        fi

        projectName=$(getPropVal ${k8sDeployPropFile} "gke.project")
        regOrZoneName=$(getPropVal ${k8sDeployPropFile} "gke.reg_or_zone")
        nodeType=$(getPropVal ${k8sDeployPropFile} "gke.node_typ")
        nodeCnt=$(getPropVal ${k8sDeployPropFile} "gke.node_num")

        source ${k8sOptDeployHomeDir}/gke_create.sh \
            -clstrName ${k8sClstrName} \
            -project ${projectName} \
            -regOrZone ${regOrZoneName} \
            -nodeType ${nodeType} \
            -nodeCnt ${nodeCnt}
        ;;

    aks)
        echo "Deployment option ${k8sOpt} is to be implemented ..."
        ;;
        
    eks)
        echo "Deployment option ${k8sOpt} is to be implemented ..."
        ;;
    *)
        echo "[ERROR] Unsupported K8s deployment option : \"${k8sOpt}\""
        ;;
esac

ks8OptClusterCreationErrCode=$?
if [[ "${ks8OptClusterCreationErrCode}" -ne 0 ]]; then
    echo "[ERROR] Failed to create the K8s clsuter !"
    errExit 200; 
fi

nginxIngressEnabled=$(getPropVal ${k8sDeployPropFile} "k8s.nginx.ingress")
if [[ "${nginxIngressEnabled}" == "true" ]]; then
    echo
    echo "--------------------------------------------------------------"
    echo ">> Deploy NGINX ingress controller in the current K8s cluster if needed ... "
    helm upgrade \
         --install ingress-nginx ingress-nginx \
         --repo https://kubernetes.github.io/ingress-nginx \
         --namespace ingress-nginx --create-namespace

    echo
    echo "--------------------------------------------------------------"
    echo ">> Deploy NGINX ingress controller in the current K8s cluster if needed ... "

    kubectl wait --namespace ingress-nginx \
        --for=condition=ready pod \
        --selector=app.kubernetes.io/component=controller \
        --timeout=60s
fi

echo
