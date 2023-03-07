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
   echo "Usage: deployScenario.sh [-h]"
   echo "                         -scnName <scenario_name>"
   echo "                         [-scnPropFile] <scenario_property_file>"
   echo "                         [-depAppOnly]"
   echo "                         [-rebuildApp]"
   echo "                         [-appDefFile] <app_definition_file>"
   echo "                         [-k8sClstrName] <k8s_cluster_name>"
   echo "                         [-k8sPropFile] <k8s_property_file>"
   echo "                         [-pulsarClstrName] <pulsar_cluster_name>"
   echo "                         [-pulsarPropFile] <pulsar_property_file>"
   echo ""
   echo "       -h              : Show usage info"
   echo "       -scnName        : Demo scenario name."
   echo "       -scnPropFile    : (Optional) Full file path to a scenario property file. Use default if not specified."
   echo "       -depAppOnly     : (Optional) Skip infrastructure deployment and only deploy applications."
   echo "       -rebuildApp     : (Optional) Whether to rebuild the application repository."
   echo "       -appDefFile     : (Optional) Full file path to an application definition file."
   echo " ----------------------------------------------------------------------------------"
   echo " The following options are only relevant when "
   echo "   scenario.infra_mode=='luna_new'. Otherwise, they're no-op even if specified!"
   echo " ----------------------------------------------------------------------------------"
   echo "       -k8sClstrName   : (Optional) K8s cluster name. Use default if not specified."
   echo "       -k8sPropFile    : (Optional) Full file path to a K8s deployment property file. Use default if not specified."
   echo "       -pulsarClstrName: (Optional) Pulsar cluster name. Use default if not specified."
   echo "       -pulsarPropFile : (Optional) Full file path to a Pulsar deployment property file. Use default if not specified."
   echo
}

if [[ $# -eq 0 || $# -gt 16 ]]; then
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
      -depAppOnly) depAppOnly=1; ;;
      -rebuildApp) rebuildApp=1; ;;
      -appDefFile) appDefFile=$2; shift ;;
      -k8sClstrName) k8sClstrName=$2; shift ;;
      -k8sPropFile) k8sPropFile=$2; shift ;;
      -pulsarClstrName) pulsarClstrName=$2; shift ;;
      -pulsarPropFile) pulsarPropFile=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done
debugMsg "scnName=${scnName}"
debugMsg "scnPropfile=${scnPropFile}"
debugMsg "depAppOnly=${depAppOnly}"
debugMsg "rebuildApp=${rebuildApp}"
debugMsg "appDefFile=${appDefFile}"
debugMsg "k8sClstrName=${k8sClstrName}"
debugMsg "k8sPropFile=${k8sPropFile}"
debugMsg "pulsarClstrName=${pulsarClstrName}"
debugMsg "pulsarPropFile=${pulsarPropfile}"


startDate=$(date +'%Y-%m-%d')
startDate2=${startDate//[: -]/}

# 2023-02-18 09:24:56
startTime=$(date +'%Y-%m-%d %T')
# 20230218092525
startTime2=${startTime//[: -]/}

# trims trailing slash character if there is any
scnName=$(echo "${scnName}" | sed 's:/*$::')
scnHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/scenarios/${scnName}"

scnLogHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/scenarios/logs/${startDate}"
if ! [[ -d "${scnLogHomeDir}" ]]; then
   mkdir -p ${scnLogHomeDir}
fi
scnExecMainLogFileNoExt="${scnLogHomeDir}/deploy_${scnName}"
scnExecMainLogFile="${scnExecMainLogFileNoExt}_main.log"
scnExecPostDeployLogFile="${scnExecMainLogFileNoExt}_post_deploy.log"

dftScnPropFile="${scnHomeDir}/scenario.properties"
if ! [[ -n "${scnPropFile}" && -f "${scnPropFile}" ]]; then
   if [[ -f "${dftScnPropFile}" ]]; then
      scnPropFile=${dftScnPropFile}
   fi
fi
echo "scnHomeDir is ${scnHomeDir}"
outputMsg ">>> Starting demo scenario deployment [name: ${scnName}, time: ${startTime}, application only: ${depAppOnly}]" 0 ${scnExecMainLogFile} true
if ! [[ -n "${scnName}" && -d "${scnHomeDir}"  ]]; then
    outputMsg "[ERROR] The specified scenario name doesn't exist!" 4 ${scnExecMainLogFile} true
    errExit 40
fi
if ! [[ -f "${scnPropFile}" ]]; then
   outputMsg "[ERROR] Can't find the default scenario properties file; must specify one explicitly!" 4 ${scnExecMainLogFile} true
   errExit 50
fi

echo > ${scnExecMainLogFile}
outputMsg "** Main execution log file  : ${scnExecMainLogFile}" 4 ${scnExecMainLogFile} true
outputMsg "** Scenario properties file : ${scnPropFile}" 4 ${scnExecMainLogFile} true


##
# Deploy the underlying infrastructure
# - Only needed when Luna Streaming is the deployment type
# -----------------------------------------

##
# - Check what type of Pulsar infrastructure to use: Astra Streaming or Luna Streaming
infraDeployMode=$(getPropVal ${scnPropFile} "scenario.infra_mode")
useAstra=0

outputMsg "" 0 ${scnExecMainLogFile} true
if [[ "${infraDeployMode}" == "astra" ]]; then
   useAstra=1
   outputMsg ">>> Use 'Astra Streaming' as the underlying Pulsar cluster infrastructure." 0 ${scnExecMainLogFile} true
elif [[ "${infraDeployMode}" == "luna_existing" ]]; then
   outputMsg ">>> Use an existing 'Luna Streaming' cluster as the underlying Pulsar cluster infrastructure." 0 ${scnExecMainLogFile} true
elif [[ "${infraDeployMode}" == "luna_new" ]]; then
   outputMsg ">>> Create a new K8s cluster and a 'Luna Streaming' cluster as the underlying Pulsar cluster infrastructure." 0 ${scnExecMainLogFile} true
else
    echo "[ERROR] Unsupported \"scenario.infra_mode\" setting."
    echo "        Must be one of the following values: \"${SCN_DEPLOY_MODES[@]}\""
    errExit 60;
fi


##
# Get the effective file name between the provided one and the default one
#
getEffectiveFile() {
   local providedPropFile=$1
   local defaultPropFile=$2

   if ! [[ -n "${providedPropFile}" && -f "${providedPropFile}" ]]; then
      if ! [[ -n "${defaultPropFile}" && -f "${defaultPropFile}" ]]; then
         echo ""
      else 
         echo "${defaultPropFile}"
      fi
   else
      echo "${providedPropFile}"
   fi
}

##
# Get the effective cluster name between the provided one and the default one
#
getEffectiveClusterName() {
   local providedName=$1
   local defaultName=$2

   if [[ -z "${providedName}" ]]; then
      if [[ -z "${defaultName}" ]]; then
         echo ""
      else 
         echo "${defaultName}"
      fi
   else
      echo "${providedName}"
   fi
}

##
# Process the return code of a called script
#
procScriptRtnCode() {
   local scriptRtnCode=$1
   local logFileName=$2
   local leadingSpaceNum=$3
   local errExitCode=$4
   local errExitDesc=$5

   if [[ ${scriptRtnCode} -ne 0 ]]; then
      outputMsg "[ERROR] ${errExitDesc} (error code: ${scriptRtnCode})!" ${leadingSpaceNum} ${logFileName} true
      errExit ${errExitCode}
   # else
   #    outputMsg "[SUCCESS]" ${leadingSpaceNum} ${logFileName} true
   fi
}


if [[ ${depAppOnly} -eq 0 ]]; then
   ##
   # Astra Streaming or Existing Luna Streaming
   # - Download "client.conf"
   ##
   if [[ "${infraDeployMode}" == "astra" || "${infraDeployMode}" == "luna_existing" ]]; then
      echo "    - Please prepare the proper \"client.conf\" file to the current demo scenario folder:"
      echo "      ${scnHomeDir}"
      echo 
      while ! [[ "${promptAnswer}" == "yes" || "${promptAnswer}" == "y" || 
               "${promptAnswer}" == "quit" || "${promptAnswer}" == "q" ]]; do
         read -s -p "      Have you completed preparing the proper \"client.conf\" file? [yes|y|quit|q] " promptAnswer
         echo
      done

      if [[ "${promptAnswer}" == "quit" || "${promptAnswer}" == "q"  ]]; then
         errExit 1
      fi
   ##
   # New Luna Streaming
   # - Deploy a K8s cluster
   # - Deploy a Pulsar cluster
   # - Forward Pulsar proxy ports to localhost
   ## 
   else
      #
      # Deploy a self-managed K8s cluster
      #
      dftK8sPropFile="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/k8s/k8s.properties"
      k8sPropFile=$(getEffectiveFile ${k8sPropFile} ${dftK8sPropFile})
      if [[ -z "${k8sPropFile}" ]]; then
         outputMsg "[ERROR] Must specify a valid K8s deployment property file!" 3 ${scnExecMainLogFile} true
         errExit 100
      fi

      dftK8sClstrName=$(getPropVal ${k8sPropFile} "k8s.cluster.name")
      k8sClstrName=$(getEffectiveClusterName ${k8sClstrName} ${dftK8sClstrName})  
      if [[ -z "${k8sClstrName}" ]]; then
         outputMsg "[ERROR] Must specify a K8s cluster name!" 3 ${scnExecMainLogFile} true
         errExit 110
      fi

      k8sDeployScript="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/k8s/deploy_k8s_cluster.sh"
      k8sDeployExecLogFile="${scnExecMainLogFileNoExt}_k8s.log"

      outputMsg "- Deploying a K8s clsuter named \"${k8sClstrName}\" ..." 4 ${scnExecMainLogFile} true
      outputMsg "* K8s deployment log file : ${k8sDeployExecLogFile}" 6 ${scnExecMainLogFile} true
      outputMsg "* K8s properties file     : ${k8sPropFile}" 6 ${scnExecMainLogFile} true

      if ! [[ -f "${k8sDeployScript}" ]]; then
         outputMsg "[ERROR] Can't find the K8s cluster deployment script file" 6 ${scnExecMainLogFile} true
         errExit 120
      else
         eval '"${k8sDeployScript}" -clstrName ${k8sClstrName} -propFile ${k8sPropFile}' > ${k8sDeployExecLogFile} 2>&1

         procScriptRtnCode $? ${scnExecMainLogFile} 6 \
            130 "Failed to execute K8s cluster deployment script"
      fi

      outputMsg "" 0 ${scnExecMainLogFile} true

      #
      # Deploy a Pulsar cluster on the K8s cluster just created
      #
      dftPulsarPropFile="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar/pulsar.properties"
      pulsarPropFile=$(getEffectiveFile ${pulsarPropFile} ${dftPulsarPropFile})
      if [[ -z "${pulsarPropFile}" ]]; then
         outputMsg "[ERROR] Must specify a valid Pulsar deployment property file!" 3 ${scnExecMainLogFile} true
         errExit 200
      fi

      dftPulsarClstrName=$(getPropVal ${pulsarPropFile} "pulsar.cluster.name")
      pulsarClstrName=$(getEffectiveClusterName ${pulsarClstrName} ${dftPulsarClstrName})  
      if [[ -z "${pulsarClstrName}" ]]; then
         outputMsg "[ERROR] Must specify a Pulsar cluster name!" 3 ${scnExecMainLogFile} true
         errExit 210
      fi

      pulsarDeployScript="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar/deploy_pulsar_cluster.sh"
      pulsarDeployExecLogFile="${scnExecMainLogFileNoExt}_pulsar.log"

      outputMsg "- Deploying a Pulsar cluster named \"${pulsarClstrName}\" ..." 4 ${scnExecMainLogFile} true
      outputMsg "** Pulsar deployment log file  : ${pulsarDeployExecLogFile}" 6 ${scnExecMainLogFile} true
      outputMsg "** Pulsar properties file      : ${pulsarPropFile}" 6 ${scnExecMainLogFile} true

      if ! [[ -f "${pulsarDeployScript}" ]]; then
         outputMsg "[ERROR] Can't find the Pulsar cluster deployment script file" 6 ${scnExecMainLogFile} true
         errExit 220
      else
         upgradeExistingPulsar=$(getPropVal ${scnPropFile} "ls.upgrade.existing.pulsar")
         if [[ "${upgradeExistingPulsar}" == "false" ]]; then
            eval '"${pulsarDeployScript}" -clstrName ${pulsarClstrName} -propFile ${pulsarPropFile} -genClntConfFile ${scnHomeDir}' > \
               ${pulsarDeployExecLogFile} 2>&1
         else
            eval '"${pulsarDeployScript}" -clstrName ${pulsarClstrName} -propFile ${pulsarPropFile} -upgrade -genClntConfFile ${scnHomeDir}' > \
               ${pulsarDeployExecLogFile} 2>&1
         fi

         procScriptRtnCode $? ${scnExecMainLogFile} 6 \
            230 "Failed to execute Pulsar cluster deployment script"
      fi

      outputMsg "" 0 ${scnExecMainLogFile} true

      #
      # Forward Pulsar Proxy service ports to localhost
      #
      k8sProxyPortForwardScript="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar/forward_pulsar_proxy_port.sh"
      k8sProxyPortForwardLogFile="${scnExecMainLogFileNoExt}_port_forward.log"

      outputMsg "- Forward Pulsar Proxy service ports to localhost ..." 4 ${scnExecMainLogFile} true
      outputMsg "** Port forwarding log file : ${k8sProxyPortForwardLogFile}" 6 ${scnExecMainLogFile} true
      
      if ! [[ -f "${k8sProxyPortForwardScript}" ]]; then
         outputMsg "[ERROR] Can't find the Pulsar Proxy K8s port forward script file" 6 ${scnExecMainLogFile} true
         errExit 240
      else
         outputMsg "> Wait for Proxy deployment is ready ..." 6 ${scnExecMainLogFile} true
         kubectl wait --timeout=600s --for condition=Available=True deployment -l=component="proxy" >> ${scnExecMainLogFile}

         outputMsg "> Wait for Proxy service is ready ..." 6 ${scnExecMainLogFile} true
         proxySvcName=$(kubectl get svc -l=component="proxy" -o name)
         debugMsg "proxySvcName=${proxySvcName}"
         ## wait for Proxy service is assigned an external IP
         until [ -n "$(kubectl get ${proxySvcName} -o jsonpath='{.status.loadBalancer.ingress[0].ip}')" ]; do
            sleep 1
         done

         # Start port forwarding for Pulsar Proxy service
         if [[ -n "${proxySvcName// }" ]]; then
            outputMsg "> Start port forwarding ..." 6 ${scnExecMainLogFile} true
            helmTlsEnabled=$(getPropVal ${pulsarPropFile} "helm.tls.enabled")
            eval '"${k8sProxyPortForwardScript}" -act start -proxySvc ${proxySvcName} -tlsEnabled ${helmTlsEnabled}' > ${k8sProxyPortForwardLogFile} 2>&1

            procScriptRtnCode $? ${scnExecMainLogFile} 6 \
               250 "Failed to execute Pulsar cluster deployment script"
         fi
      fi
   fi
fi


##
# Deploy the demo applications to be used in this scenario 
# -----------------------------------------
outputMsg "" 0 ${scnExecMainLogFile} true

appDeployHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/application_deploy"

# Generate scenario specific application definition file if not sepcified explicitly
if [[ -z "${appDefFile}" ]]; then
   appDefFile="${scnHomeDir}/app_def.properties"
   appDefTemplateFile="${appDeployHomeDir}/template/app_def.properties.tmpl"
   cp -rf ${appDefTemplateFile} ${appDefFile}
fi

appDeployScript="${appDeployHomeDir}/deploy_demo_apps.sh"
appDeployExecLogFile="${scnExecMainLogFileNoExt}_demoapp.log"

outputMsg ">>> Deploying demo applications ..." 0 ${scnExecMainLogFile} true
outputMsg "* App deployment log file : ${appDeployExecLogFile}" 4 ${scnExecMainLogFile} true
outputMsg "* App definition file     : ${appDefFile}" 4 ${scnExecMainLogFile} true

if ! [[ -f "${appDeployScript}" ]]; then
   outputMsg "[ERROR] Can't find the demo app deployment script file" 3 ${scnExecMainLogFile} true
   errExit 310
else
   appIdListStr=$(getPropVal ${scnPropFile} "scenario.app.ids")
   IFS=',' read -r -a appIdArr <<< "${appIdListStr}"

   for appId in "${appIdArr[@]}"; do
      appParamStr=$(getPropVal ${scnPropFile} "scenario.app.param.${appId}")
      # appParamStr may contain '/'
      appParamStr2=$(echo ${appParamStr} | sed 's/\//\\\//g')
      sed -i "/${appId}/s/<PARAM_LIST_TMPL>/${appParamStr2}/g" ${appDefFile}
   done

   eval '"${appDeployScript}" -scnName ${scnName} -appIdList ${appIdListStr} -appDefFile ${appDefFile} -buildRepo ${rebuildApp} -useAstra ${useAstra}' \
      > ${appDeployExecLogFile} 2>&1

   procScriptRtnCode $? ${scnExecMainLogFile} 4 \
      320 "Failed to execute demo app deployment script" 
fi


##
# - Check if there is a post deployment script to execute. for example,
#   a bash script to create the required tenants/namespaces/topics/subscriptions
#   that are going to be used in the demo
scnPostDeployScript="${scnHomeDir}/post_deploy.sh"
if [[ -f "${scnPostDeployScript// }"  ]]; then
   outputMsg "" 0 ${scnExecMainLogFile} true
   outputMsg ">>> Post deployment script file is detected: ${scnPostDeployScript}" 0 ${scnExecMainLogFile} true
   outputMsg "* Post deplopyment log file : ${scnExecPostDeployLogFile}" 4 ${scnExecMainLogFile} true

   eval '"${scnPostDeployScript}" ${scnName}' > ${scnExecPostDeployLogFile} 2>&1

   procScriptRtnCode $? ${scnExecMainLogFile} 4 \
      400 "Failed to execute scenario post deployment script"
fi

# 2022-08-19 11:40:23
outputMsg "" 0 ${scnExecMainLogFile} true
endTime=$(date +'%Y-%m-%d %T')
outputMsg ">> Finishing demo scenario deployment [name: ${scnName}, time: ${endTime}]" 0 ${scnExecMainLogFile} true

echo
