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

PULSAR_WORKSHOP_HOMEDIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${PULSAR_WORKSHOP_HOMEDIR}/../../_bash_utils_/utilities.sh"

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

usage() {
   echo
   echo "Usage: teardown_k8s_cluster.sh [-h] -clstrName <cluster_name> -k8sOpt <k8s_option_name>"
   echo "       -h : Show usage info"
   echo "       -clstrName : K8s cluster name."
   echo "       -k8sOpt    : K8s deployment option."
   echo
}

if [[ $# -gt 4 ]]; then
   usage
   errExit 30
fi

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -clstrName) clstrName=$2; shift ;;
      -k8sOpt) k8sOpt=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 40 ;;
   esac
   shift
done

if [[ -z ${clstrName// } ]]; then
    clstrName=$(getPropVal ${k8sDeployPropFile} "k8s.cluster.name")
    if [[ -z ${clstrName// } ]]; then
        echo "[ERROR] K8s cluster name cannot be empty! "
        errExit 50
    fi
fi

# TODO: make this as part of the input parameter
k8sDeployPropFile="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/k8s/k8s.properties"

if [[ -z ${k8sOpt// } ]]; then
    k8sOpt=$(getPropVal ${k8sDeployPropFile} "k8s.deploy.option")
    if [[ -z ${k8sOpt// } ]]; then
        echo "[ERROR] A K8s deployment option must be provided!"
        errExit 60
    fi
fi

if [[ ! " ${K8S_DEPLOY_OPTIONS[@]} " =~ " ${k8sOpt} " ]]; then
    echo "[ERROR] Invalid '-k8sOpt' parameter value."
    echo "        Must be one of the following values: \"${K8S_DEPLOY_OPTIONS[@]}\""
    errExit 70
fi



echo "============================================================== "
echo "= "
echo "= A \"${k8sOpt}\" based K8s cluster with name \"${clstrName}\" will be deleted ...  "
echo "= "
echo

echo "You're about to execute a destructive operation that can't be undone."
echo "Are you certain you want to continue? [y|n|yes|no]"
read -r prompt
if [[ "${prompt// }" == "yes" || "${prompt// }" == "y" ]]; then
    case ${k8sOpt} in
        kind)
            source kind/kind_delete.sh -clstrName  ${clstrName}
            ;;

        gke)
            projectName=$(getPropVal ${k8sDeployPropFile} "gke.project")
            regOrZoneName=$(getPropVal ${k8sDeployPropFile} "gke.reg_or_zone")

            source gke/gke_delete.sh \
                -clstrName ${clstrName} \
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
fi

echo
