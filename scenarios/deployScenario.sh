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
   echo "                         [-k8sPropFile] <k8s_property_file>"
   echo "                         [-pulsarPropFile] <pulsar_property_file>"
   echo "                         [-depAppOnly]"
   echo "       -h : Show usage info"
   echo "       -scnName : Demo scenario name."
   echo "       -scnPropFile : (Optional) Full file path to a scenario property file. Use default if not specified."
   echo "       -k8sPropFile : (Optional) Full file path to a K8s deployment property file (Luna Streaming only). Use default if not specified."
   echo "       -pulsarPropFile : (Optional) Full file path to a Pulsar deployment property file(Luna Streaming only). Use default if not specified."
   echo "       -depAppOnly : (Optional) Skip cluster deployment and only deploy applications."
   echo
}

if [[ $# -eq 0 || $# -gt 9 ]]; then
   usage
   errExit 20
fi

depAppOnly=0
while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -scnName) scnName=$2; shift ;;
      -scnPropFile) scnPropFile=$2; shift ;;
      -k8sPropFile) k8sPropFile=$2; shift ;;
      -pulsarPropFile) pulsarPropFile=$2; shift ;;
      -depAppOnly) depAppOnly=1; ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done

scnHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/scenarios/${scnName}"
scnLogHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/scenarios/logs"
dftScnPropFile="${scnHomeDir}/scenario.properties"
scnPostDeployScript="${scnHomeDir}/post_deploy.sh"

if ! [[ -n "${scnName}" && -d "${scnHomeDir}"  ]]; then
    echo "[ERROR] The specified scenario name doesn't exist!."
    errExit 40;
fi

if ! [[ -n "${scnPropFile}" && -f "${scnPropFile}" ]]; then
   scnPropFile=${dftScnPropFile}
fi

startDate=$(date +'%Y-%m-%d')
startDate2=${startDate//[: -]/}

# 2023-02-18 09:24:56
startTime=$(date +'%Y-%m-%d %T')
# 20230218092525
startTime2=${startTime//[: -]/}

scnExecMainLogFileNoExt="${scnLogHomeDir}/deploy_${scnName}_${startDate2}"
# scnExecMainLogFileNoExt="${scnLogHomeDir}/deploy_${scnName}_${startTime2}"
scnExecLogFile="${scnExecMainLogFileNoExt}_main.log"
scnExecPostDeployLogFile="${scnExecMainLogFileNoExt}_post_deploy.log"

echo > ${scnExecLogFile}

outputMsg ">>> Starting demo scenario deployment [name: ${scnName}, time: ${startTime}, application only: ${depAppOnly}]" 0 ${scnExecLogFile} true
outputMsg "** Main execution log file  : ${scnExecLogFile}" 4 ${scnExecLogFile} true
outputMsg "** Scenario properties file : ${scnPropFile}" 4 ${scnExecLogFile} true


##
# Deploy the underlying infrastructure
# - Only needed when Luna Streaming is the deployment type
# -----------------------------------------

##
# - Check what type of Pulsar infrastructure to use: Astra Streaming or Luna Streaming
useAstraStreaming=$(getPropVal ${scnPropFile} "scenario.use_astra_streaming")

outputMsg "" 0 ${scnExecLogFile} true
if [[ "${useAstraStreaming}" == "yes" ]]; then
   # Astra Streaming
   outputMsg ">>> Use \"Astra Streaming\" as the demo Pulsar cluster." 0 ${scnExecLogFile} true
else 
   outputMsg ">>> Use \"Luna Streaming\" as the demo Pulsar cluster" 0 ${scnExecLogFile} true
fi


if [[ ${depAppOnly} -eq 0 ]]; then
   ##
   # Astra Streaming
   # - Download "client.conf"
   ##
   if [[ "${useAstraStreaming}" == "yes" ]]; then
      echo "    - Please download the proper \"client.conf\" file to the current demo scenario folder:"
      echo "      ${scnHomeDir}"
      echo 
      while ! [[ "${promptAnswer}" == "yes" || "${promptAnswer}" == "y" || 
               "${promptAnswer}" == "quit" || "${promptAnswer}" == "q" ]]; do
         read -s -p "      Have you completed downloading the \"client.conf\" file? [yes|y|quit|q] " promptAnswer
         echo
      done

      if [[ "${promptAnswer}" == "quit" || "${promptAnswer}" == "q"  ]]; then
         errExit 1
      fi
   ##
   # Luna Streaming
   # - Deploy a K8s cluster
   # - Deploy a Pulsar cluster
   # - Forward Pulsar proxy ports to localhost
   ## 
   else
      #
      # Deploy a self-managed K8s cluster
      #
      dftK8sPropFile="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/k8s/k8s.properties"
      k8sDeployScript="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/k8s/deploy_k8s_cluster.sh"
      k8sDeployExecLogFile="${scnExecMainLogFileNoExt}_k8s.log"

      if ! [[ -n "${k8sPropFile}" && -f "${k8sPropFile}" ]]; then
         k8sPropFile=${dftK8sPropFile}
      fi
      
      k8sClstrName=$(getPropVal ${k8sPropFile} "k8s.cluster.name")
      if [[ -z ${k8sClstrName// } ]]; then
         k8sClstrName=$(getPropVal ${scnPropFile} "scenario.id")
      fi

      outputMsg "- Deploying a K8s clsuter named \"${k8sClstrName}\" ..." 4 ${scnExecLogFile} true
      outputMsg "* K8s deployment log file : ${k8sDeployExecLogFile}" 6 ${scnExecLogFile} true
      outputMsg "* K8s properties file     : ${k8sPropFile}" 6 ${scnExecLogFile} true

      if ! [[ -f "${k8sPropFile}" && -f "${k8sDeployScript}" ]]; then
         outputMsg "[ERROR] Can't find the K8s cluster deployment property file and/or script file" 6 ${scnExecLogFile} true
         errExit 100
      else
         eval '"${k8sDeployScript}" -clstrName ${k8sClstrName} -propFile ${k8sPropFile}' > ${k8sDeployExecLogFile} 2>&1

         k8sDeployScriptErrCode=$?
         if [[ ${k8sDeployScriptErrCode} -ne 0 ]]; then
            outputMsg "[ERROR] Failed to execute K8s cluster deployment script (error code: ${k8sDeployScriptErrCode})!" 6 ${scnExecLogFile} true
            errExit 110
         else
            outputMsg "[SUCCESS]" 6 ${scnExecLogFile} true
         fi
      fi

      outputMsg "" 0 ${scnExecLogFile} true

      #
      # Deploy a Pulsar cluster on the K8s cluster just created
      #
      dftPulsarPropFile="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar/pulsar.properties"
      pulsarDeployScript="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar/deploy_pulsar_cluster.sh"
      pulsarDeployExecLogFile="${scnExecMainLogFileNoExt}_pulsar.log"

      if ! [[ -n "${pulsarPropFile}" && -f "${pulsarPropFile}" ]]; then
         pulsarPropFile=${dftPulsarPropFile}
      fi

      pulsarClstrName=$(getPropVal ${pulsarPropFile} "pulsar.cluster.name")
      if [[ -z ${pulsarClstrName// } ]]; then
         pulsarClstrName=$(getPropVal ${scnPropFile} "scenario.id")
      fi

      outputMsg "- Deploying a Pulsar cluster named \"${pulsarClstrName}\" ..." 4 ${scnExecLogFile} true
      outputMsg "** Pulsar deployment log file  : ${pulsarDeployExecLogFile}" 6 ${scnExecLogFile} true
      outputMsg "** Pulsar properties file      : ${pulsarPropFile}" 6 ${scnExecLogFile} true

      if ! [[ -f "${pulsarPropFile}" && -f "${pulsarDeployScript}" ]]; then
         outputMsg "[ERROR] Can't find the Pulsar cluster deployment property file and/or script file" 6 ${scnExecLogFile} true
         errExit 200
      else
         upgradeExistingPulsar=$(getPropVal ${scnPropFile} "ls.upgrade.existing.pulsar")
         if [[ "${upgradeExistingPulsar}" == "false" ]]; then
            eval '"${pulsarDeployScript}" -clstrName ${pulsarClstrName} -propFile ${pulsarPropFile} -genClntConfFile ${scnHomeDir}' > \
               ${pulsarDeployExecLogFile} 2>&1
         else
            eval '"${pulsarDeployScript}" -clstrName ${pulsarClstrName} -propFile ${pulsarPropFile} -upgrade -genClntConfFile ${scnHomeDir}' > \
               ${pulsarDeployExecLogFile} 2>&1
         fi

         pulsarDeployScriptErrCode=$?
         if [[ ${pulsarDeployScriptErrCode} -ne 0 ]]; then
            outputMsg "[ERROR] Failed to execute Pulsar cluster deployment script (error code: ${pulsarDeployScriptErrCode})!" 6 ${scnExecLogFile} true
            errExit 210
         else
            outputMsg "[SUCCESS]" 6 ${scnExecLogFile} true
         fi
      fi

      outputMsg "" 0 ${scnExecLogFile} true

      #
      # Forward Pulsar Proxy service ports to localhost
      #
      k8sProxyPortForwardScript="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar/forward_pulsar_proxy_port.sh"
      k8sProxyPortForwardLogFile="${scnExecMainLogFileNoExt}_port_forward.log"

      outputMsg "- Forward Pulsar Proxy service ports to localhost ..." 4 ${scnExecLogFile} true
      outputMsg "** Port forwarding log file : ${k8sProxyPortForwardLogFile}" 6 ${scnExecLogFile} true
      
      outputMsg "> Wait for Proxy deployment is ready ..." 6 ${scnExecLogFile} true
      kubectl wait --timeout=600s --for condition=Available=True deployment -l=component="proxy" >> ${scnExecLogFile}

      outputMsg "> Wait for Proxy service is ready ..." 6 ${scnExecLogFile} true
      proxySvcName=$(kubectl get svc -l=component="proxy" -o name)
      debugMsg "proxySvcName=${proxySvcName}"
      ## wait for Proxy service is assigned an external IP
      until [ -n "$(kubectl get ${proxySvcName} -o jsonpath='{.status.loadBalancer.ingress[0].ip}')" ]; do
         sleep 1
      done

      # Start port forwarding for Pulsar Proxy service
      if [[ -n "${proxySvcName// }" ]]; then
         helmTlsEnabled=$(getPropVal ${pulsarPropFile} "helm.tls.enabled")
         eval '"${k8sProxyPortForwardScript}" -act start -proxySvc ${proxySvcName} -tlsEnabled ${helmTlsEnabled}' > ${k8sProxyPortForwardLogFile} 2>&1
      fi
   fi
fi


##
# Deploy the demo applications to be used in this scenario 
# -----------------------------------------
outputMsg "" 0 ${scnExecLogFile} true
outputMsg "- Deploying demo applications ..." 3 ${scnExecLogFile} true

clntAppCodeHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/application_code"
clntAppDeployHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/application_deploy"
clntAppDefPropFile="${clntAppDeployHomeDir}/client_app_def.properties"
if ! [[ -f "${clntAppDefPropFile}" ]]; then
   outputMsg "[ERROR] Can't find client application definition file or deploy properties file" 3 ${scnExecLogFile} true
   errExit 300
fi

scnAppIdListStr=$(getPropVal ${scnPropFile} "scenario.app.ids")
IFS=', ' read -r -a scnAppIdArr <<< "${scnAppIdListStr}"

if ! [[ -d "${scnHomeDir}/appexec" ]]; then
   mkdir -p "${scnHomeDir}/appexec"
fi

for appId in "${scnAppIdArr[@]}"; do
   appDefStr=$(getPropVal ${clntAppDefPropFile} ${appId})

   validApp=1
   if [[ ${validApp} -eq 1 && -z "${appDefStr// }" ]]; then
      validApp=0
      invalidMsg="Can't find corresponding appID (${appId}) in the client app definition file."
   fi
   
   IFS=',' read -r -a appDefArr <<< "${appDefStr}"
   appLanguage=${appDefArr[0]}
   appPath=${appDefArr[1]}
   appClass=${appDefArr[2]}
   appParam=${appDefArr[3]}

   if [[ "${useAstraStreaming}" == "yes" ]]; then
      appParam="-as ${appParam}"
   fi

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
      outputMsg "> Generating the execution script for application: ${appId}" 5 ${scnExecLogFile} true

      appExcFile="${scnHomeDir}/appexec/run_${appId}.sh"
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
      echo "  -cp ${clntAppCodeHomeDir}/${appPath}/target/${appId}-1.0.0.jar \\" >> ${appExcFile}
      if [[ -n "${appParam// }" ]]; then
         echo "  ${appClass} \\" >> ${appExcFile}
         echo "  ${appParam}" >> ${appExcFile}
      else
         echo "  ${appClass}" >> ${appExcFile}
      fi

      chmod +x "${appExcFile}"
   else
      outputMsg "[WARN] ${invalidMsg}" 5 ${scnExecLogFile} true
   fi
done




##
# - Check if there is a post deployment script to execute. for example,
#   a bash script to create the required tenants/namespaces/topics/subscriptions
#   that are going to be used in the demo
if [[ -f "${scnPostDeployScript// }"  ]]; then
   outputMsg "" 0 ${scnExecLogFile} true
   outputMsg ">> Post deployment script file is detected: ${scnPostDeployScript}" 0 ${scnExecLogFile} true
   outputMsg "   - log file : ${scnExecPostDeployLogFile}" 0 ${scnExecLogFile} true

   eval '"${scnPostDeployScript}" ${scnName}' > ${scnExecPostDeployLogFile} 2>&1
fi

# 2022-08-19 11:40:23
outputMsg "" 0 ${scnExecLogFile} true
endTime=$(date +'%Y-%m-%d %T')
outputMsg ">> Finishing demo scenario deployment [name: ${scnName}, time: ${endTime}]" 0 ${scnExecLogFile} true

echo
