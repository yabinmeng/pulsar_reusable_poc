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

PULSAR_WORKSHOP_HOMEDIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )

source "${PULSAR_WORKSHOP_HOMEDIR}/../_bash_utils_/utilities.sh"

usage() {
   echo
   echo "Usage: terminateScenario.sh [-h]"
   echo "                            -scnName <scenario_name>"
   echo "                            [-keepK8s]"
   echo "       -h : Show usage info"
   echo "       -scnName : Demo scenario name."
   echo "       -keepK8s : Whether to keep K8s cluster."
   echo
}

if [[ $# -eq 0 || $# -gt 3 ]]; then
   usage
   errExit 20
fi

keepK8s=0
while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -scnName) scnName=$2; shift ;;
      -keepK8s) keepK8s=1; ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done


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

termScnExecLogFileNoExt="${scnLogHomeDir}/term_${scnName}_${startDate2}"
# depScnExecLogFileNoExt="${scnLogHomeDir}/term_${scnName}_${startTime2}"
termScnExecLogFile="${depScnExecLogFileNoExt}_main.log"

echo > ${depScnExecLogFile}

outputMsg ">>> Starting demo scenario deployment [name: ${scnName}, time: ${startTime}, application only: ${depAppOnly}]" 0 ${depScnExecLogFile} true
outputMsg "** Main execution log file  : ${depScnExecLogFile}" 4 ${depScnExecLogFile} true
outputMsg "** Scenario properties file : ${scnPropFile}" 4 ${depScnExecLogFile} true


##
# Deploy the underlying infrastructure
# - Only needed when Luna Streaming is the deployment type
# -----------------------------------------

##
# - Check what type of Pulsar infrastructure to use: Astra Streaming or Luna Streaming
useAstraStreaming=$(getPropVal ${scnPropFile} "scenario.use_astra_streaming")

outputMsg "" 0 ${depScnExecLogFile} true
if [[ "${useAstraStreaming}" == "yes" ]]; then
   # Astra Streaming
   outputMsg ">>> Use \"Astra Streaming\" as the demo Pulsar cluster." 0 ${depScnExecLogFile} true
else 
   outputMsg ">>> Use \"Luna Streaming\" as the demo Pulsar cluster" 0 ${depScnExecLogFile} true
fi