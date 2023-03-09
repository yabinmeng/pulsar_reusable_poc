
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
PULSAR_WORKSHOP_HOMEDIR=$( cd -- "${CUR_SCRIPT_FOLDER}/.." &> /dev/null && pwd )

source "${PULSAR_WORKSHOP_HOMEDIR}/_bash_utils_/utilities.sh"

usage() {   
   echo
   echo "Usage: deploy_demo_apps.sh [-h]"
   echo "                           -scnName <scenario_name>"
   echo "                           -appIdList <app_id_list>"
   echo "                           -appDefFile <app_definition_file>"
   echo "                           -buildRepo <1_or_0>"
   echo "                           -useAstra <1_or_0>"
   echo ""
   echo "       -h          : Show usage info"
   echo "       -scnName    : Demo scenario name."
   echo "       -appIdList  : Demo application id list string."
   echo "       -appDefFile : Full file path to a application defintion file."
   echo "       -buildRepo  : Whether to build application repository (1: yes, 0: no)."
   echo "       -useAstra   : Whether to use Astra streaming as the underlying infra (1: yes, 0: no)."
   echo
}

if [[ $# -eq 0 || $# -gt 10 ]]; then
   usage
   errExit 20
fi

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -scnName) scnName=$2; shift ;;
      -appIdList) appIdListStr=$2; shift ;;
      -appDefFile) appDefFile=$2; shift ;;
      -buildRepo) buildRepo=$2; shift ;;
      -useAstra) useAstra=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done
debugMsg "scnName=${scnName}"
debugMsg "appIdList=${appIdList}"
debugMsg "appDefFile=${appDefFile}"
debugMsg "buildRepo=${buildRepo}"
debugMsg "useAstra=${useAstra}"

scnHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/scenarios/${scnName}"
demoAppCodeHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/application_code"
demoAppDeployHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/application_deploy"

genExecScript_ClntApp() {
    ##
    # TBD: add supuport for apps written in other languages
    ##
    if [[ "${appLanguage}" == "java" ]]; then
        clientAppJarFileSrc="${demoAppCodeHomeDir}/client_apps/java/${appPath}/target/${appId}-1.0.0.jar"
        clientAppFileTgt="${scnHomeDir}/appexec/package/${appId}.jar"

        if [[ -f "${clientAppJarFileSrc}" ]]; then
            cp -rf ${clientAppJarFileSrc} ${clientAppFileTgt}
        
            echo "java \\" >> ${appExcFile}
            echo "  -cp ${scnHomeDir}/appexec/package/${appId}.jar \\" >> ${appExcFile}
            if [[ -n "${appParam// }" ]]; then
                echo "  com.example.pulsarworkshop.${appClass} \\" >> ${appExcFile}
                echo "  ${appParam}" >> ${appExcFile}
            else
                echo "  ${appClass}" >> ${appExcFile}
            fi

            echo "[SUCCESS]"
        else
            echo "[ERROR] Can't find the corresponding application JAR file."
        fi
    fi
}

genExecScript_Func() {
    local functionName=${1}
    local restApiUrl=${2}
    local jwtToken=${3}
    local caCertFile=${4}
    local functionParams=${5}

    ##
    # TBD: add supuport for apps written in other languages
    ##
    if [[ "${appLanguage}" == "java" ]]; then
        funcJarFileSrc="${demoAppCodeHomeDir}/functions/java/${appPath}/target/${appId}-1.0.0.jar"
        funcJarFileTgt="${scnHomeDir}/appexec/package/${appId}.jar"
        funcCfgJsonFileTgt="${scnHomeDir}/appexec/package/${appId}.config.json"
        funcClassName="com.example.pulsarworkshop.${appClass}"
        
        if [[ -f "${funcJarFileSrc}" ]]; then
            cp -rf ${funcJarFileSrc} ${funcJarFileTgt}
            cp -rf ${demoAppDeployHomeDir}/template/function.config.java.tmpl ${funcCfgJsonFileTgt}

            paramArr=(${functionParams})            
            for p in "${paramArr[@]}"; do
                tempArr=(${p//:/ })
                if [[ "${tempArr[0]}" == "tenant" ]]; then
                    tenantName="${tempArr[1]}"
                elif [[ "${tempArr[0]}" == "namespace" ]]; then
                    namespaceName="${tempArr[1]}"
                elif [[ "${tempArr[0]}" == "inputs" ]]; then
                    inputTopicArr=("${tempArr[1]/,/ /g}" )
                    inputTopicList=$(for t in "${inputTopicArr[@]}"; do echo -n "\"${t}\"",; done| sed 's/,$//')
                elif [[ "${tempArr[0]}" == "output" ]]; then
                    outputTopic="${tempArr[1]}"
                 elif [[ "${tempArr[0]}" == "autoAck" ]]; then
                    autoAck="${tempArr[1]}"
                fi
            done

            if [[ -z "${tenantName}" || -z "${namespaceName}" || ${#inputTopicArr[@]} -eq 0 ]]; then
                echo "[ERROR] Must specify tenant', namespace, and input topic(s) for a Pulsar function."
            else
                sed -i "s/<tenant_name>/${tenantName}/g" ${funcCfgJsonFileTgt}
                sed -i "s/<namespace_name>/${namespaceName}/g" ${funcCfgJsonFileTgt}
                sed -i "s/<func_nam>/${appId}/g" ${funcCfgJsonFileTgt}
                # inputTopicList may contain '/'
                inputTopicList2=$(echo ${inputTopicList} | sed 's/\//\\\//g')
                sed -i "s/<input_topic_list>/${inputTopicList2}/g" ${funcCfgJsonFileTgt}
                # outputTopic may contain '/'
                outputTopic2=$(echo ${outputTopic} | sed 's/\//\\\//g')
                sed -i "s/<output_topic>/${outputTopic2}/g" ${funcCfgJsonFileTgt}
                sed -i "s/<auto_ack>/${autoAck}/g" ${funcCfgJsonFileTgt}
                sed -i "s/<class_name>/${funcClassName}/g" ${funcCfgJsonFileTgt}

                local curlCmd_CrtFunc="curl -v -k -X POST \\
    --write-out '%{http_code}' \\
    --url '${restApiUrl}/admin/v3/functions/${tenantName}/${namespaceName}/${functionName}' \\"
                    
                if [[ -n "${jwtToken// }" ]]; then
                    curlCmd_CrtFunc="${curlCmd_CrtFunc}
    --header 'Authorization: Bearer ${jwtToken}' \\"
                fi

                if [[ -n "${caCertFile// }" ]]; then
                    curlCmd_CrtFunc="${curlCmd_CrtFunc}
    --cacert '${caCertFile}' \\"
                fi

                curlCmd_CrtFunc="${curlCmd_CrtFunc}
    --form \"functionConfig=@${funcCfgJsonFileTgt};type=application/json\" \\
    --form \"data=@${funcJarFileTgt};type=application/octet-stream\""

                echo "${curlCmd_CrtFunc}" >> ${appExcFile}
                echo "[SUCCESS]"
            fi
        else
            echo "[ERROR] Can't find the corresponding function JAR file."
        fi
    fi
}


##
# TODO: The code below will execute "mvn" build even when the
#       specified demo apps are non-Java apps.
##
if [[ ${buildRepo} -eq 1 ]]; then
    mvnExistence=$(chkSysSvcExistence mvn)
    debugMsg "mvnExistence=${mvnExistence}"
    if [[ ${mvnExistence} -eq 0 ]]; then
        echo "[ERROR] 'mvn' isn't installed on the local machine yet; please install it first!"
        errExit 40;
    fi

    curDir=$(pwd)

    echo
    echo "--------------------------------------------------------------"
    echo ">> Build the code repository for Pulsar client applications ... "
    cd ${demoAppCodeHomeDir}/client_apps/java
    mvn clean package

    echo ">> Build the code repository for Pulsar functions ... "
    cd ${demoAppCodeHomeDir}/functions/java
    mvn clean package
    
    cd ${curDir}
fi

if [[ -n "${appIdListStr// }" ]]; then
    if ! [[ -f ${appDefFile// } ]]; then
        echo "[ERROR] Can't find the provided demo app definition file: \"${appDefFile}\"!"
        errExit 40; 
    fi

    echo
    echo "--------------------------------------------------------------"
    echo ">> Generating the execution scripts for the specified demo applications ..."

    if ! [[ -d "${scnHomeDir}/appexec/package" ]]; then
        mkdir -p "${scnHomeDir}/appexec/package"
    fi

    clntConnFile="${scnHomeDir}/client.conf"
    restApiUrl=$(grep -v ^\# ${clntConnFile} | grep webServiceUrl | awk -F= '{print $2}' | tr -d '"')
    jwtTokenStr=$(grep -v ^\# ${clntConnFile} | grep authParams | awk -F: '{print $2}' | tr -d '"')
    trustedCaCertFilePath=$(grep -v ^\# ${clntConnFile} | grep tlsTrustCertsFilePath | awk -F= '{print $2}' | tr -d '"')
    debugMsg "restApiUrl=${restApiUrl}"
    debugMsg "jwtTokenStr=${jwtTokenStr}"
    debugMsg "trustedCaCertFilePath=${trustedCaCertFilePath}"

    IFS=',' read -r -a appIdArr <<< "${appIdListStr}"
    for appId in "${appIdArr[@]}"; do
        echo "   - Demo App ID: ${appId} (type: ${appType})"

        appDefStr=$(getPropVal ${appDefFile} ${appId})
        validApp=1
        if [[ ${validApp} -eq 1 && -z "${appDefStr}" ]]; then
            validApp=0
            invalidMsg="Can't find corresponding appID (${appId}) in the client app definition file."
        fi
        
        IFS='|' read -r -a appDefArr <<< "${appDefStr}"
        appLanguage=${appDefArr[0]}
        appType=${appDefArr[1]}
        appPath=${appDefArr[2]}
        appClass=${appDefArr[3]}
        appParam=${appDefArr[4]}

        if [[ ${validApp} -eq 1 ]] && ! [[ -n "${appPath}" && -d "${demoAppCodeHomeDir}/${appType}s/${appLanguage}/${appPath}" ]]; then
            validApp=0
            invalidMsg="Can't find the corresponding code path for appID (${appId})."
        fi

        if [[ ${validApp} -eq 1 && -z "${appClass}" ]]; then
            validApp=0
            invalidMsg="Unspecified classname for appID (${appId})."
        fi

        if [[ ${validApp} -eq 1 && -z "${appParam}" ]]; then
            validApp=0
            invalidMsg="Unspecified application parameter list for appID (${appId})."
        fi

        if [[ ${validApp} -eq 1 ]]; then
            appExcFile="${scnHomeDir}/appexec/run_${appId}.sh"
        
            echo "#! /bin/bash" > ${appExcFile}
            echo >> ${appExcFile}
            echo "##" >> ${appExcFile}
            echo "# This is an automatically generated script for " >> ${appExcFile}
            echo "# - Demo scenario Name  : \"${scnName}\"" >> ${appExcFile}
            echo "# - App ID : \"${appId}\" (type: \"${appType}\")" >> ${appExcFile}
            echo "##" >> ${appExcFile}
            echo >> ${appExcFile}

            if [[ "${appType}" == "client_app" ]]; then
                if [[ ${useAstra} -eq 1 ]]; then
                    appParam="-as ${appParam}"
                fi
                rtnMsg=$(genExecScript_ClntApp)
            else
                rtnMsg=$(genExecScript_Func \
                    "${appId}" \
                    "${restApiUrl}" \
                    "${jwtTokenStr}" \
                    "${trustedCaCertFilePath}" \
                    "${appParam}")
            fi

            chmod +x "${appExcFile}"

            outputMsg "${rtnMsg}" 5 ${appDeployExecLogFile} true
        else
            outputMsg "[ERROR] ${invalidMsg}" 5 ${appDeployExecLogFile} true
        fi
    done
fi