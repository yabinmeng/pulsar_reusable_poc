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
   echo "Usage: createReadme.sh [-h]"
   echo "                       -scnName <scenario_name>"
   echo "                       [-scnPropFile] <scenario_property_file>"
   echo ""
   echo "       -h              : Show usage info"
   echo "       -scnName        : Demo scenario name."
   echo "       -scnPropFile    : (Optional) Full file path to a scenario property file. Use default if not specified."
}

if [[ $# -eq 0 || $# -gt 6 ]]; then
   usage
   errExit 20
fi

depAppOnly=0
rebuildApp=0
while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -scnName) scnName=$2; shift ;;
      -scnPropFile) scnPropFile=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done
debugMsg "scnName=${scnName}"
debugMsg "scnPropfile=${scnPropFile}"

scnName=$(echo "${scnName}" | sed 's:/*$::')
scnHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/scenarios/${scnName}"
debugMsg "scnHomeDir=${scnHomeDir}"

dftScnPropFile="${scnHomeDir}/scenario.properties"
if ! [[ -n "${scnPropFile}" && -f "${scnPropFile}" ]]; then
   if [[ -f "${dftScnPropFile}" ]]; then
      scnPropFile=${dftScnPropFile}
   fi
fi

outputMsg ">>> Creating README file for the specified scenario \"${scnName}\""
if ! [[ -n "${scnName}" && -d "${scnHomeDir}"  ]]; then
    outputMsg "[ERROR] The specified scenario name doesn't exist!" 4 
    errExit 40
fi
if ! [[ -f "${scnPropFile}" ]]; then
   outputMsg "[ERROR] Can't find the scenario properties file!" 4
   errExit 50
fi

appDeployHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/application_deploy"
appListDefPropfile="${appDeployHomeDir}/app_list_def.properties"
debugMsg "appListDefPropfile=${appListDefPropfile}"

tgtScnReadmeTmplFile="${PULSAR_WORKSHOP_HOMEDIR}/scenarios/template/master_readme_tmpl.md"