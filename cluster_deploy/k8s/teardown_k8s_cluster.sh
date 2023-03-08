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
# This script is used to tear down a K8s cluster that was created by
# "deploy_k8s_cluster.sh" script. 
#
# Currently, the following K8s options are supported
# - kind
# - gke
# - aks
# - eks
# 

echo

usage() {
   echo
   echo "Usage: teardown_k8s_cluster.sh [-h]"
   echo "                               -clstrName <cluster_name>"
   echo "                               -propFile <deployment_properties_file>"
   echo "       -h : Show usage info"
   echo "       -clstrName : K8s cluster name."
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
      -propFile) k8sPropFile=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done

dftK8sPropFile="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/k8s/k8s.properties"
if ! [[ -n "${k8sPropFile}" && -f "${k8sPropFile}" ]]; then
    k8sPropFile=${dftK8sPropFile}
fi

if ! [[ -f ${k8sPropFile// } ]]; then
    echo "[ERROR] Can't find the provided K8s deployment properties file: \"${k8sPropFile}\"!"
    errExit 40; 
fi

kubeCtlExistence=$(chkSysSvcExistence kubectl)
debugMsg "kubeCtlExistence=${kubeCtlExistence}"
if [[ ${kubeCtlExistence} -eq 0 ]]; then
    echo "[ERROR] 'kubectl' isn't installed on the local machine yet; please install it first!"
    errExit 50;
fi

if [[ -z ${k8sClstrName// } ]]; then
    k8sClstrName=$(getPropVal ${k8sPropFile} "k8s.cluster.name")
    if [[ -z ${k8sClstrName// } ]]; then
        echo "[ERROR] K8s cluster name cannot be empty! "
        errExit 60
    fi
fi

k8sOpt=$(getPropVal ${k8sPropFile} "k8s.deploy.option")
if [[ -z ${k8sOpt// } ]]; then
    echo "[ERROR] A K8s deployment option must be provided!"
    errExit 70
fi

if [[ ! " ${K8S_DEPLOY_OPTIONS[@]} " =~ " ${k8sOpt} " ]]; then
    echo "[ERROR] Invalid '-k8sOpt' parameter value."
    echo "        Must be one of the following values: \"${K8S_DEPLOY_OPTIONS[@]}\""
    errExit 80
fi

k8sOptDeployHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/k8s/${k8sOpt}"

echo "============================================================== "
echo "= "
echo "= A \"${k8sOpt}\" based K8s cluster with name \"${k8sClstrName}\" will be deleted ...  "
echo "= "
echo

case ${k8sOpt} in
    kind)
        if ! [[ -f "${k8sOptDeployHomeDir}/kind_delete.sh" ]]; then
            echo "[ERROR] Can't find the script file to terminate a 'kind' K8s clsuter !"
            errExit 90; 
        fi

        source ${k8sOptDeployHomeDir}/kind_delete.sh -clstrName  ${k8sClstrName}
        ;;

    gke)
        if ! [[ -f "${k8sOptDeployHomeDir}/gke_delete.sh" ]]; then
            echo "[ERROR] Can't find the script file to deploy a 'gke' K8s clsuter!"
            errExit 100; 
        fi
        
        projectName=$(getPropVal ${k8sPropFile} "gke.project")
        regOrZoneName=$(getPropVal ${k8sPropFile} "gke.reg_or_zone")
        source ${k8sOptDeployHomeDir}/gke_delete.sh \
            -clstrName ${k8sClstrName} \
            -project ${projectName} \
            -regOrZone ${regOrZoneName}
        ;;

    aks)
        echo "Deployment option ${k8sOpt} is to be implemented ..."
        ;;
        
    eks)
        echo "Deployment option ${k8sOpt} is to be implemented ..."
        ;;
esac

echo
