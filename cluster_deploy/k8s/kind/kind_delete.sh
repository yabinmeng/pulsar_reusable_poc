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
PULSAR_WORKSHOP_HOMEDIR=$( cd -- "${CUR_SCRIPT_FOLDER}/../../.." &> /dev/null && pwd )

source "${PULSAR_WORKSHOP_HOMEDIR}/_bash_utils_/utilities.sh"

### 
# This script is used to delete a Kind (K8s) cluster that was created by 
# the "kind_create.sh" script
# 

usage() {
   echo
   echo "Usage: kind_delete.sh [-h] [-clstrName <cluster_name>]"
   echo "       -h : Show usage info"
   echo "       -clstrName : (Optional) Custom Kind cluster name."
   echo
}

if [[ $# -eq 0 || $# -gt 2 ]]; then
   usage
   exit 110
fi

echo

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -clstrName) clstrName=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 120 ;;
   esac
   shift
done

dockerExistence=$(chkSysSvcExistence docker)
debugMsg "dockerExistence=${dockerExistence}"
if [[ ${dockerExistence} -eq 0 ]]; then
    echo "[ERROR] Docker engine isn't installed on the local machine yet; please install it first!"
    exit 130;
fi

kindExistence=$(chkSysSvcExistence kind)
debugMsg "kindExistence=${kindExistence}"
if [[ ${kindExistence} -eq 0 ]]; then
    echo "[ERROR] Kind isn't installed on the local machine yet; please install it first!"
    exit 140;
fi

if [[ -z "${clstrName// }" ]]; then
    tgtClstrName="kind"
else
    tgtClstrName="${clstrName}"
fi

echo
echo "--------------------------------------------------------------"
echo ">> Delete the Kind cluster with the name \"${tgtClstrName}\" ..."

clusterExistence=$(kind get clusters 2>&1 | grep "${tgtClstrName}")
if [[ -n "${clusterExistence}" ]] && [[ "${clusterExistence}" != "No kind clusters found."  ]]; then
    kind delete cluster --name ${tgtClstrName}
    if [[ $? -ne 0 ]]; then
        echo "   [ERROR] Cluster deletion failed!"
        errExit 150
    fi
else
    echo "   [WARN] The Kind cluster with the spcified name does not exist!"
fi

## 
## NOTE: not needed, deleting Kind cluster will automatically unset and 
##       delete the client configs
##
# echo
# echo "--------------------------------------------------------------"
# echo ">> Remove the corresponding K8s client configuration for this cluster ..."
# kubectl config delete-context "kind-${tgtClstrName}"
# kubectl config delete-cluster "kind-${tgtClstrName}"
