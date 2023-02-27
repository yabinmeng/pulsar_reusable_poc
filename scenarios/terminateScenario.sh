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

echo

CUR_SCRIPT_FOLDER=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
PULSAR_WORKSHOP_HOMEDIR=$( cd -- "${CUR_SCRIPT_FOLDER}/.." &> /dev/null && pwd )

source "${PULSAR_WORKSHOP_HOMEDIR}/_bash_utils_/utilities.sh"


usage() {
   echo
   echo "Usage: terminateScenario.sh [-h]"
   echo "                            -scnName <scenario_name>"
   echo "                            [-scnPropFile] <scenario_property_file>"
   echo "                            [-k8sPropFile] <k8s_property_file>"
   echo "                            [-pulsarPropFile] <pulsar_property_file>"
   echo "                            [-keepK8s]"
   echo "       -h : Show usage info"
   echo "       -scnName : Demo scenario name."
   echo "       -scnPropFile : (Optional) Full file path to a scenario property file. Use default if not specified."
   echo "       -k8sPropFile : (Optional) Full file path to a K8s deployment property file (Luna Streaming only). Use default if not specified."
   echo "       -pulsarPropFile : (Optional) Full file path to a Pulsar deployment property file(Luna Streaming only). Use default if not specified."
   echo "       -keepK8s : (Optional) Whether to keep K8s cluster."
   echo
}

if [[ $# -eq 0 || $# -gt 9 ]]; then
   usage
   errExit 20
fi

keepK8s=0
while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -scnName) scnName=$2; shift ;;
      -scnPropFile) scnPropFile=$2; shift ;;
      -k8sPropFile) k8sPropFile=$2; shift ;;
      -pulsarPropFile) pulsarPropFile=$2; shift ;;
      -keepK8s) keepK8s=1; ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done

echo "You're about to execute a destructive operation that can't be undone."
echo "Are you certain you want to continue? [y|n|yes|no]"
read -r prompt
if [[ "${prompt// }" == "yes" || "${prompt// }" == "y" ]]; then
   scnHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/scenarios/${scnName}"
   scnLogHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/scenarios/logs"
   scnPropFile="${scnHomeDir}/scenario.properties"

   if ! [[ -n "${scnName}" && -d "${scnHomeDir}"  ]]; then
      echo "[ERROR] The specified scenario name doesn't exist!."
      errExit 40;
   fi

   startDate=$(date +'%Y-%m-%d')
   startDate2=${startDate//[: -]/}

   # 2023-02-18 09:24:56
   startTime=$(date +'%Y-%m-%d %T')
   # 20230218092525
   startTime2=${startTime//[: -]/}

   scnExecMainLogFileNoExt="${scnLogHomeDir}/term_${scnName}_${startDate2}"
   # scnExecMainLogFileNoExt="${scnLogHomeDir}/term_${scnName}_${startTime2}"
   scnExecMainLogFile="${scnExecMainLogFileNoExt}_main.log"

   echo > ${scnExecMainLogFile}

   outputMsg ">>> Starting demo scenario termination [name: ${scnName}, time: ${startTime}]" 0 ${scnExecMainLogFile} true
   outputMsg "** Main execution log file  : ${scnExecMainLogFile}" 4 ${scnExecMainLogFile} true
   outputMsg "** Scenario properties file : ${scnPropFile}" 4 ${scnExecMainLogFile} true


   ##
   # Term the underlying infrastructure
   # - Only needed when Luna Streaming is the deployment type
   # -----------------------------------------

   ##
   # - Check what type of Pulsar infrastructure to use: Astra Streaming or Luna Streaming
   useAstraStreaming=$(getPropVal ${scnPropFile} "scenario.use_astra_streaming")

   outputMsg "" 0 ${scnExecMainLogFile} true
   if [[ "${useAstraStreaming}" == "yes" ]]; then
      # Astra Streaming
      outputMsg ">>> Use \"Astra Streaming\", there is nothing to terminate." 0 ${scnExecMainLogFile} true
   else 
      if [[ ${keepK8s} -eq 0 ]]; then
         outputMsg ">>> Use \"Luna Streaming\", terminating the provisioned Pulsar cluster but keeeping the K8s cluster" 0 ${scnExecMainLogFile} true
      else
         outputMsg ">>> Use \"Luna Streaming\", terminating both the provisioned Pulsar cluster and K8s cluster" 0 ${scnExecMainLogFile} true
      fi
   fi

   ##
   # - Terminate the Pulsar cluster
   dftPulsarPropFile="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar/pulsar.properties"
   pulsarTermScript="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar/teardown_pulsar_cluster.sh"
   pulsarTermExecLogFile="${scnExecMainLogFileNoExt}_pulsar.log"

   if ! [[ -n "${pulsarPropFile}" && -f "${pulsarPropFile}" ]]; then
      pulsarPropFile=${dftPulsarPropFile}
   fi

   pulsarClstrName=$(getPropVal ${pulsarPropFile} "pulsar.cluster.name")
   if [[ -z ${pulsarClstrName// } ]]; then
      pulsarClstrName=$(getPropVal ${scnPropFile} "scenario.id")
   fi

   outputMsg "- Terminating a Pulsar cluster named \"${pulsarClstrName}\" ..." 4 ${scnExecMainLogFile} true
   outputMsg "** Pulsar termination log file : ${pulsarTermExecLogFile}" 6 ${scnExecMainLogFile} true
   outputMsg "** Pulsar properties file      : ${pulsarPropFile}" 6 ${scnExecMainLogFile} true

   if ! [[ -f "${pulsarPropFile}" && -f "${pulsarTermScript}" ]]; then
      outputMsg "[ERROR] Can't find the Pulsar cluster deployment property file and/or script file" 6 ${scnExecMainLogFile} true
      errExit 100
   else
      eval '"${pulsarTermScript}" -clstrName ${pulsarClstrName} -propFile ${pulsarPropFile} ' > \
         ${pulsarTermExecLogFile} 2>&1

      pulsarTermScriptErrCode=$?
      if [[ ${pulsarTermScriptErrCode} -ne 0 ]]; then
         outputMsg "[ERROR] Failed to execute Pulsar cluster termination script (error code: ${pulsarTermScriptErrCode})!" 6 ${scnExecMainLogFile} true
         errExit 110
      else
         outputMsg "[SUCCESS]" 6 ${scnExecMainLogFile} true
      fi
   fi

   ##
   # - Terminate a self-managed K8s cluster
   if [[ ${keepK8s} -eq 0 ]]; then
      outputMsg "" 0 ${scnExecMainLogFile} true

      dftK8sPropFile="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/k8s/k8s.properties"
      k8sTermScript="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/k8s/teardown_k8s_cluster.sh"
      k8sTermExecLogFile="${scnExecMainLogFileNoExt}_k8s.log"

      if ! [[ -n "${k8sPropFile}" && -f "${k8sPropFile}" ]]; then
         k8sPropFile=${dftK8sPropFile}
      fi
      
      k8sClstrName=$(getPropVal ${k8sPropFile} "k8s.cluster.name")
      if [[ -z ${k8sClstrName// } ]]; then
         k8sClstrName=$(getPropVal ${scnPropFile} "scenario.id")
      fi

      outputMsg "- Terminating a K8s clsuter named \"${k8sClstrName}\" ..." 4 ${scnExecMainLogFile} true
      outputMsg "* K8s termination log file        : ${k8sTermExecLogFile}" 6 ${scnExecMainLogFile} true
      outputMsg "* K8s termination properties file : ${k8sPropFile}" 6 ${scnExecMainLogFile} true

      if ! [[ -f "${k8sPropFile}" && -f "${k8sTermScript}" ]]; then
         outputMsg "[ERROR] Can't find the K8s cluster deployment property file and/or script file" 6 ${scnExecMainLogFile} true
         errExit 100
      else
         eval '"${k8sTermScript}" -clstrName ${k8sClstrName} -propFile ${k8sPropFile}' > ${k8sTermExecLogFile} 2>&1

         k8sTermScriptErrCode=$?
         if [[ ${k8sTermScriptErrCode} -ne 0 ]]; then
            outputMsg "[ERROR] Failed to execute K8s cluster termination script (error code: ${k8sTermScriptErrCode})!" 6 ${scnExecMainLogFile} true
            errExit 110
         else
            outputMsg "[SUCCESS]" 6 ${scnExecMainLogFile} true
         fi
      fi
   fi
fi