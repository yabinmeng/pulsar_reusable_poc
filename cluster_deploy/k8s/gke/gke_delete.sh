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

if [[ -z "${PULSAR_WORKSHOP_HOMEDIR}" ]]; then
    echo "Workshop home direcotry is not set; please first run \"source ../../_bash_utils_/setenv.sh\" in the current directory!"
    exit 10;
fi

source "${PULSAR_WORKSHOP_HOMEDIR}/_bash_utils_/utilities.sh"


### 
# This script is used to delete a GKE (K8s) cluster that was created by 
# the "gke_create.sh" script
# 

usage() {
   echo
   echo "Usage: gke_delete.sh [-h]"
   echo "                     -clstrName <cluster_name>"
   echo "                     [-project <gcp_project_name>]"
   echo "                     [-regOrZone] <region_or_zone_name>"
   echo "       -h : Show usage info"
   echo "       -clstrName : (Optional) Custom Kind cluster name."
   echo "       -project : (Optional) GCP project name (use default name if not specified)"
   echo "       -regOrZone : (Optional) GCP region or zone (region:<region_name>, or zone:<zone_name>)"
   echo
}

if [[ $# -eq 0 || $# -gt 6 ]]; then
   usage
   exit 210
fi

echo

isRegional=0
setActiveSvcAcct=0
while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -clstrName) clstrName=$2; shift ;;
      -regOrZone) regOrZoneName=$2; shift ;;
      -project) projectName=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 220 ;;
   esac
   shift
done

debugMsg "clstrName=${clstrName}"
debugMsg "regOrZoneName=${regOrZoneName}"
debugMsg "projectName=${projectName}"

gcloudExistence=$(chkSysSvcExistence gcloud)
debugMsg "gcloudExistence=${gcloudExistence}"
if [[ ${gcloudExistence} -eq 0 ]]; then
    echo "[ERROR] gcloud isn't installed on the local machine yet; please install it first!"
    errExit 230;
fi

validRegOrZoneParam=0
if [[ -n "${regOrZoneName// }" ]]; then
    regOrZoneNameStrArr=(${regOrZoneName//:/ })
    if [[ ${#regOrZoneNameStrArr[@]} -eq 2 ]]; then
        regOrZoneTypeStr=${regOrZoneNameStrArr[0]}
        regOrZoneNameStr=${regOrZoneNameStrArr[1]}    
        debugMsg "regOrZoneTypeStr=${regOrZoneTypeStr}"
        debugMsg "regOrZoneNameStr=${regOrZoneNameStr}"

        if [[ "${regOrZoneTypeStr}" == "region" || "${regOrZoneTypeStr}" == "zone" ]]; then
            validRegOrZoneParam=1
        fi
    fi
fi
if [[ ${validRegOrZoneParam} -eq 0 ]]; then
    echo "[ERROR] Invalid region or zone name string. It must be in format \"region:<region_name>\" or \"zone:<zone_name>\"!"
    errExit 240;
fi

echo
echo "--------------------------------------------------------------"
echo ">> Delete the GKE cluster with the name \"${clstrName}\" ..."

clusterExistence=$(gcloud beta container clusters list 2>&1 | grep "${clstrName}")
if [[ -n "${clusterExistence// }" ]]; then
    gcloud beta container clusters delete ${clstrName}
    if [[ $? -ne 0 ]]; then
        echo "   [ERROR] Cluster deletion failed!"
        errExit 230
    fi
else
    echo "   [WARN] The GKE cluster with the spcified name does not exist!"
fi

## 
## NOTE: not needed, deleting GKE cluster will automatically unset and 
##       delete the client configs
##
# echo
# echo "--------------------------------------------------------------"
# echo ">> Remove the corresponding K8s client configuration for this cluster ..."
# configName="gke_${projectName}_${regOrZoneNameStr}_${clstrName}"
# debugMsg "configName=${configName}"

# kubectl config delete-user "${configName}"
# kubectl config delete-cluster "${configName}"
# kubectl config delete-context "${configName}"
