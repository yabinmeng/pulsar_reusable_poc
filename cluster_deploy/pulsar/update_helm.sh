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
# This script is used to update Pulsar helm chart templates for
# 1) Chart dependency update
# 2) LunaStreaming version update
# 


usage() {
   echo
   echo "Usage: update_helm.sh [-h]"
   echo "                       -chart </path/to/helm/chart/file>"
   echo "                       [-depUpdt] <true|false>"
   echo "                       [-clstrName <cluster_name>]"
   echo "                       -tgtRelease <version_string>"
   echo "       -h : Show usage info"
   echo "       -chart:    : Helm chart file to update"
   echo "       -depUpdt   : (Optional) Update helm chart dependencies if true (default \"false\")."
   echo "       -clstrName : (Optional) Update Pulsar cluster name (default: \"pulsar\")."
   echo "       -tgtRelease : Update to a specific Pulsar release version."
   echo
}

if [[ $# -eq 0 || $# -gt 8 ]]; then
   usage
   exit 20
fi

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -depUpdt) depUpdt=$2; shift ;;
      -file) chartFile=$2; shift ;;
      -clstrName) clstrName=$2; shift ;;
      -tgtRelease) tgtRelease=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done

debugMsg "chartFile=${chartFile}"
debugMsg "depUpdat=${depUpdt}"
debugMsg "clstrName=${clstrName}"
debugMsg "tgtRelease=${tgtRelease}"

if [[ -z "{tgtRelName// }" || -z  "{tgtRelName// }" ]]; then
   echo "Incorrect specified target docker image release information: \"tgtRelease\". It must be in format <release_name>:<release_version>!"
   exit 40
fi

pulsarDeployHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar"
helmChartHomeDir="${pulsarDeployHomeDir}/helm"

# Create the pulsar helm chart file from the tmlate
echo
echo "--------------------------------------------------------------"
echo ">> Create the helm chart file (${helmChartFile}) from the corresponding template. "
if ! [[ -f "${helmChartHomeDir}/template/${helmChartFile}.tmpl" ]]; then
   echo "  [ERROR] Can't find the required Pulsar helm chart file template!"
   echo "          (\"${helmChartHomeDir}/template/${helmChartFile}.tmpl\")"
   errExit 50   
fi

cp -rf "${helmChartHomeDir}/template/${helmChartFile}.tmpl" "${helmChartHomeDir}/${helmChartFile}"

# Update Pulsar helm chart dependency if needed
if [[ -f "Chart.yaml" ]]; then
   chartYamlFileExists=1
fi

echo
echo "--------------------------------------------------------------"
if [[ "${depUpdt}" == "true" && ${chartYamlFileExists} -eq 1 ]]; then
   echo ">> Update chart depdendency (depUpdt: ${depUpdt}, chartYamlFileExists: ${chartYamlFileExists}) "
   helm dependency update
   if [[ $? -ne 0 ]]; then
      echo "   [ERROR] Chart dependency udpate failed!"
   fi
else
   echo ">> Skip chart depdendency update (depUpdt: ${depUpdt}, chartYamlFileExists: ${chartYamlFileExists}) ... "
fi   

## 
# Update release tag in a particular "values.yaml" file
# - $1: Helm chart name
# - $2: Pulsar cluster name
# - $3: Pulsar image release name in "format <name>:<version>""
function updatePulsarHelmChart() {
   tgtRelInfoArr=($(echo "$2" | awk -F':' '{print $1 " "  $2}'))
   tgtRelName=${tgtRelInfoArr[0]}
   tgtRelVer=${tgtRelInfoArr[1]}
   debugMsg "tgtRelName=${tgtRelName}"
   debugMsg "tgtRelVer=${tgtRelVer}"

   # Update Pulsar cluster name
   sed -i "s/fullnameOverride:.*/fullnameOverride: ${clstrName}/g" $1 

   # Update image name
   curNameArr=()
   for name in $( grep "repository:" $1 | awk -F': ' '{ print $NF}' | uniq); do
      curNameArr+=("${name}")
   done
   for curName in "${curNameArr[@]}"; do
      if [[ "${curName}" != "${tgtRelName}"  ]]; then
         echo "   - replacing release name \"${curName}\" with \"${tgtRelName}\""
         # use '#' as separater because release name contains special character '/'
         sed -i "s#repository: ${curName}#repository: ${tgtRelName}#g" $1 
      fi
   done

   # Update image version
   curVerArr=()
   for version in $( grep "tag:" $1 | awk -F': ' '{ print $NF}' | uniq); do
      curVerArr+=("${version}")
   done

   for curVer in "${curVerArr[@]}"; do
      if [[ "${curVer}" != "${tgtRelVer}"  ]]; then
         echo "   - replacing release version \"${curVer}\" with \"${tgtRelVer}\""
         sed -i "s/tag: ${curVer}/tag: ${tgtRelVer}/g" $1 
      fi
   done
}

echo
echo "--------------------------------------------------------------"
echo ">> Update Pulsar image release to \"${tgtRelease}\""
updatePulsarHelmChart "${helmChartHomeDir}/${helmChartFile}" "${tgtRelease}"
