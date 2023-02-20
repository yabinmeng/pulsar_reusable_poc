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

if [[ -z "${PULSAR_WORKSHOP_HOMEDIR}" ]]; then
    echo "Workshop home direcotry is not set; please first run \"source ../_bash_utils_/setenv.sh\" in the current directory!"
    exit 10;
fi

curDir=$(pwd)
source "${PULSAR_WORKSHOP_HOMEDIR}/_bash_utils_/utilities.sh"

usage() {
   echo
   echo "Usage: deployScenario.sh [-h]"
   echo "                        -scnName <scenario_name>"
   echo "       -h : Show usage info"
   echo "       -scnName : Demo scenario name."
   echo
}

if [[ $# -eq 0 || $# -gt 2 ]]; then
   usage
   errExit 20
fi

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -scnName) scnName=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done

scenarioHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/scenarios/${scnName}"
scenarioLogHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/scenarios/logs"
scenarioPropFile="${scenarioHomeDir}/scenario.properties"
scenarioPostDeployScript="${scenarioHomeDir}/post_deploy.sh"

if ! [[ -n "${scnName}" && -d "${scenarioHomeDir}"  ]]; then
    echo "[ERROR] The specified scenario name doesn't exist!."
    errExit 40;
fi

startDate=$(date +'%Y-%m-%d')
startDate2=${startDate//[: -]/}

# 2023-02-18 09:24:56
startTime=$(date +'%Y-%m-%d %T')
# 20230218092525
startTime2=${startTime//[: -]/}

scenarioExecLogFileNoExt="${scenarioLogHomeDir}/${scnName}_${startDate2}"
# scenarioExecLogFileNoExt="${scenarioLogHomeDir}/${scnName}_${startTime2}"
scenarioExecLogFile="${scenarioExecLogFileNoExt}.log"
scenarioExecPostDeployLogFile="${scenarioExecLogFileNoExt}_post_deploy.log"

echo > ${scenarioExecLogFile}

outputMsg ">>> Starting demo scenario deployment [name: ${scnName}, time: ${startTime}]" 0 ${scenarioExecLogFile} true
outputMsg "** Main execution log file  : ${scenarioExecLogFile}" 4 ${scenarioExecLogFile} true
outputMsg "** Scenario properties file : ${scenarioPropFile}" 4 ${scenarioExecLogFile} true


##
# Deploy the underlying infrastructure
# - Only needed when Luna Streaming is the deployment type
# -----------------------------------------

##
# - Check what type of Pulsar infrastructure to use: Astra Streaming or Luna Streaming
useAstraStreaming=$(getPropVal ${scenarioPropFile} "scenario.use_astra_streaming")

outputMsg "" 0 ${scenarioExecLogFile} true
if [[ "${useAstraStreaming}" == "yes" ]]; then
   # Astra Streaming
   outputMsg ">>> Use \"Astra Streaming\" as the demo Pulsar cluster." 0 ${scenarioExecLogFile} true
   echo "    - Please download the proper \"client.conf\" file to the current demo scenario folder:"
   echo "      ${scenarioHomeDir}"
   echo 
   while ! [[ "${promptAnswer}" == "yes" || "${promptAnswer}" == "y" || 
              "${promptAnswer}" == "quit" || "${promptAnswer}" == "q" ]]; do
      read -s -p "      Have you completed downloading the \"client.conf\" file? [yes|y|quit|q] " promptAnswer
      echo
   done

   if [[ "${promptAnswer}" == "quit" || "${promptAnswer}" == "q"  ]]; then
      errExit 1
   fi
else
   # Luna Streaming
   outputMsg ">>> Use \"Luna Streaming\" as the demo Pulsar cluster" 0 ${scenarioExecLogFile} true
   
   ##
   # Deploy a self-managed K8s cluster
   #
   k8sDeployPropFile="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/k8s/k8s.properties"
   k8sDeployScript="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/k8s/deploy_k8s_cluster.sh"
   k8sDeployExecLogFile="${scenarioExecLogFileNoExt}_k8s_deploy.log"
   
   k8sClstrName=$(getPropVal ${k8sDeployPropFile} "k8s.cluster.name")
   if [[ -z ${k8sClstrName// } ]]; then
      k8sClstrName=$(getPropVal ${scenarioPropFile} "scenario.id")
   fi

   outputMsg "- Deploying a K8s clsuter named \"${k8sClstrName}\" ..." 4 ${scenarioExecLogFile} true
   outputMsg "* K8s deployment log file        : ${k8sDeployExecLogFile}" 6 ${scenarioExecLogFile} true
   outputMsg "* K8s deployment properties file : ${k8sDeployPropFile}" 6 ${scenarioExecLogFile} true

   if ! [[ -f "${k8sDeployPropFile}" && -f "${k8sDeployScript}" ]]; then
      outputMsg "[ERROR] Can't find the K8s cluster deployment property file and/or script file" 6 ${scenarioExecLogFile} true
      errExit 100
   else
      eval '"${k8sDeployScript}" -clstrName ${k8sClstrName} -propFile ${k8sDeployPropFile}' > ${k8sDeployExecLogFile} 2>&1

      k8sDeployScriptErrCode=$?
      if [[ ${k8sDeployScriptErrCode} -ne 0 ]]; then
         outputMsg "[ERROR] Failed to execute K8s cluster deployment script (error code: ${k8sDeployScriptErrCode})!" 6 ${scenarioExecLogFile} true
         errExit 110
      else
         outputMsg "[SUCCESS]" 6 ${scenarioExecLogFile} true
      fi
   fi

   outputMsg "" 0 ${scenarioExecLogFile} true

   ##
   # Deploy a Pulsar cluster on the K8s cluster just created
   #
   pulsarDeployPropFile="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar/pulsar.properties"
   pulsarDeployScript="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar/deploy_pulsar_cluster.sh"
   pulsarDeployExecLogFile="${scenarioExecLogFileNoExt}_pulsar_deploy.log"

   pulsarClstrName=$(getPropVal ${pulsarDeployPropFile} "pulsar.cluster.name")
   if [[ -z ${pulsarClstrName// } ]]; then
      pulsarClstrName=$(getPropVal ${scenarioPropFile} "scenario.id")
   fi

   outputMsg "- Deploying a Pulsar cluster named \"${pulsarClstrName}\" ..." 4 ${scenarioExecLogFile} true
   outputMsg "** Pulsar deployment log file        : ${pulsarDeployExecLogFile}" 6 ${scenarioExecLogFile} true
   outputMsg "** Pulsar deployment properties file : ${pulsarDeployPropFile}" 6 ${scenarioExecLogFile} true

   if ! [[ -f "${pulsarDeployPropFile}" && -f "${pulsarDeployScript}" ]]; then
      outputMsg "[ERROR] Can't find the Pulsar cluster deployment property file and/or script file" 6 ${scenarioExecLogFile} true
      errExit 200
   else
      upgradeExistingPulsar=$(getPropVal ${scenarioPropFile} "ls.upgrade.existing.pulsar")
      if [[ "${upgradeExistingPulsar}" == "false" ]]; then
         eval '"${pulsarDeployScript}" -clstrName ${pulsarClstrName} -propFile ${pulsarDeployPropFile} -genClntConfFile ${scenarioHomeDir}' > \
            ${pulsarDeployExecLogFile} 2>&1
      else
         eval '"${pulsarDeployScript}" -clstrName ${pulsarClstrName} -propFile ${pulsarDeployPropFile} -upgrade -genClntConfFile ${scenarioHomeDir}' > \
            ${pulsarDeployExecLogFile} 2>&1
      fi

      pulsarDeployScriptErrCode=$?
      if [[ ${pulsarDeployScriptErrCode} -ne 0 ]]; then
         outputMsg "[ERROR] Failed to execute Pulsar cluster deployment script (error code: ${pulsarDeployScriptErrCode})!" 6 ${scenarioExecLogFile} true
         errExit 210
      else
         outputMsg "[SUCCESS]" 6 ${scenarioExecLogFile} true
      fi
   fi

   outputMsg "" 0 ${scenarioExecLogFile} true

   ##
   # Forward Pulsar Proxy service ports to localhost
   #
   k8sProxyPortForwardScript="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar/forward_pulsar_proxy_port.sh"
   k8sProxyPortForwardLogFile="${scenarioExecLogFileNoExt}_port_forward.log"

   outputMsg "- Forward Pulsar Proxy service ports to localhost ..." 4 ${scenarioExecLogFile} true
   outputMsg "** Port forwarding log file : ${k8sProxyPortForwardLogFile}" 6 ${scenarioExecLogFile} true
   
   outputMsg "> Wait for Proxy deployment is ready ..." 6 ${scenarioExecLogFile} true
   kubectl wait --timeout=600s --for condition=Available=True deployment -l=component="proxy" >> ${scenarioExecLogFile}

   outputMsg "> Wait for Proxy service is ready ..." 6 ${scenarioExecLogFile} true
   proxySvcName=$(kubectl get svc -l=component="proxy" -o name)
   debugMsg "proxySvcName=${proxySvcName}"
   ## wait for Proxy service is assigned an external IP
   until [ -n "$(kubectl get ${proxySvcName} -o jsonpath='{.status.loadBalancer.ingress[0].ip}')" ]; do
      sleep 1
   done

   # Start port forwarding for Pulsar Proxy service
   if [[ -n "${proxySvcName// }" ]]; then
      helmTlsEnabled=$(getPropVal ${pulsarDeployPropFile} "helm.tls.enabled")
      eval '"${k8sProxyPortForwardScript}" -act start -proxySvc ${proxySvcName} -tlsEnabled ${helmTlsEnabled}' > ${k8sProxyPortForwardLogFile} 2>&1
   fi
fi


##
# Deploy the demo applications to be used in this scenario 
# -----------------------------------------
outputMsg "" 0 ${scenarioExecLogFile} true
outputMsg "- Deploying demo applications ..." 3 ${scenarioExecLogFile} true

clntAppCodeHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/application_code"
clntAppDeployHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/application_deploy"
clntAppDefPropFile="${clntAppDeployHomeDir}/client_app_def.properties"
if ! [[ -f "${clntAppDefPropFile}" ]]; then
   outputMsg "[ERROR] Can't find client application definition file or deploy properties file" 3 ${scenarioExecLogFile} true
   errExit 300
fi

scnAppIdListStr=$(getPropVal ${scenarioPropFile} "scenario.app.ids")
IFS=', ' read -r -a scnAppIdArr <<< "${scnAppIdListStr}"

if ! [[ -d "${scenarioHomeDir}/appexec" ]]; then
   mkdir -p "${scenarioHomeDir}/appexec"
fi

for appId in "${scnAppIdArr[@]}"; do
   appDefStr=$(getPropVal ${clntAppDefPropFile} ${appId})

   validApp=1
   if [[ ${validApp} -eq 1 && -z "${appDefStr// }" ]]; then
      validApp=0
      invalidMsg="Can't find corresponding appID (${appId}) in the client app definition file."
   fi
   
   IFS=', ' read -r -a appDefArr <<< "${appDefStr}"
   appLanguage=${appDefArr[0]}
   appPath=${appDefArr[1]}
   appClass=${appDefArr[2]}
   appParam=${appDefArr[3]}

   #
   # TBD: currently this only support java demo applications
   #      add support for other languages in the future
   # 
   if [[ ${validApp} -eq 1 && "${appLanguage}" != "java" ]]; then
      validApp=0
      invalidMsg="Unsupported programming language (${appLanguage}) for appID (${appId})."
   fi

   if [[ ${validApp} -eq 1 ]] && ! [[ -n "${appPath}" && -d "${clntAppCodeHomeDir}/${appPath}" ]]; then
      validApp=0
      invalidMsg="Cna't find the corresponding path for appID (${appId})."
   fi

   if [[ ${validApp} -eq 1 && -z "${appClass}" ]]; then
      validApp=0
      invalidMsg="Empty classname for appID (${appId})."
   fi

   if [[ ${validApp} -eq 1 ]]; then
      outputMsg "> Generating the execution script for application: ${appId}" 5 ${scenarioExecLogFile} true

      appExcFile="${scenarioHomeDir}/appexec/run_${appId}.sh"
      echo > "${appExcFile}"

      echo "#! /bin/bash" >> ${appExcFile}
      echo >> ${appExcFile}
      echo "##" >> ${appExcFile}
      echo "# This is an automatically generated script for " >> ${appExcFile}
      echo "# - Demo scenario Name  : \"${scnName}\"" >> ${appExcFile}
      echo "# - Demo application ID : \"${appId}\"" >> ${appExcFile}
      echo "##" >> ${appExcFile}
      echo >> ${appExcFile}
      echo "java \\" >> ${appExcFile}
      echo "  -jar ${clntAppCodeHomeDir}/${appPath}/target/${appId}-1.0.0-SNAPSHOT.jar \\" >> ${appExcFile}
      if [[ -n "${appParam// }" ]]; then
         echo "  ${appClass} \\" >> ${appExcFile}
         echo "  ${appParam}" >> ${appExcFile}
      else
         echo "  ${appClass}" >> ${appExcFile}
      fi

      chmod +x "${appExcFile}"
   else
      outputMsg "[WARN] ${invalidMsg}" 5 ${scenarioExecLogFile} true
   fi
done




##
# - Check if there is a post deployment script to execute. for example,
#   a bash script to create the required tenants/namespaces/topics/subscriptions
#   that are going to be used in the demo
if [[ -f "${scenarioPostDeployScript// }"  ]]; then
   outputMsg "" 0 ${scenarioExecLogFile} true
   outputMsg ">> Post deployment script file is detected: ${scenarioPostDeployScript}" 0 ${scenarioExecLogFile} true
   outputMsg "   - log file : ${scenarioExecPostDeployLogFile}" 0 ${scenarioExecLogFile} true

   eval '"${scenarioPostDeployScript}" ${scnName}' > ${scenarioExecPostDeployLogFile} 2>&1
fi

# 2022-08-19 11:40:23
outputMsg "" 0 ${scenarioExecLogFile} true
endTime=$(date +'%Y-%m-%d %T')
outputMsg ">> Finishing demo scenario deployment [name: ${scnName}, time: ${endTime}]" 0 ${scenarioExecLogFile} true

echo
