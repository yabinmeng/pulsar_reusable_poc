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
    echo "Workshop home direcotry is not set; please first run \"source ../_bash_utils_/setenv.sh\" in the current directory!"
    exit 10;
fi

source "${PULSAR_WORKSHOP_HOMEDIR}/_bash_utils_/utilities.sh"

usage() {
   echo
   echo "Usage: deploy_pulsar_cluster.sh [-h]"
   echo "                                -clstrName <cluster_name>"
   echo "                                -propFile <deployment_properties_file>"
   echo "                                [-upgrade]"
   echo "       -h : Show usage info"
   echo "       -clstrName : Pulsar cluster name"
   echo "       -propFile  : Pulsar deployment properties file"
   echo "       -upgrade   : (Optional) Whether to upgrade the existing Pulsar cluster"
   
   echo
}

if [[ $# -eq 0 || $# -gt 5 ]]; then
   usage
   errExit 20
fi

upgradeExistingCluster=0
while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -clstrName) pulsarClstrName=$2; shift ;;
      -propFile) pulsarDeployPropFile=$2; shift ;;
      -upgrade) upgradeExistingCluster=1; ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done

if ! [[ -f ${pulsarDeployPropFile// } ]]; then
    echo "[ERROR] Can't find the provided Pulsar deployment properties file: \"${pulsarDeployPropFile}\"!"
    errExit 40; 
fi

helmExistence=$(chkSysSvcExistence helm)
debugMsg "helmExistence=${helmExistence}"
if [[ ${helmExistence} -eq 0 ]]; then
    echo "[ERROR] 'helm' isn't installed on the local machine yet; please install it first!"
    errExit 50;
fi

if [[ -z ${pulsarClstrName// } ]]; then
    dftClstrName=$(getPropVal ${pulsarDeployPropFile} "pulsar.cluster.name")
    if [[ -z ${dftClstrName// } ]]; then
        echo "[ERROR] Pulsar cluster name cannot be empty! "
        errExit 60
    else
        pulsarClstrName=${dftClstrName}
    fi
fi
# Name must be lowercase
pulsarClstrName=$(echo "${pulsarClstrName}" | tr '[:upper:]' '[:lower:]')

pulsarDeployHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar"
helmChartHomeDir="${pulsarDeployHomeDir}/helm"

helmAuthMethod=$(getPropVal ${pulsarDeployPropFile} "helm.auth.method")
helmTlsEnabled=$(getPropVal ${pulsarDeployPropFile} "helm.tls.enabled")
debugMsg "helmAuthMethod=${helmAuthMethod}"
debugMsg "helmTlsEnabled=${helmTlsEnabled}"

if [[ -z "${helmAuthMethod// }" || "${helmAuthMethod// }" == "jwt" ]]; then
    if [[ -z "${helmTlsEnabled// }" || "${helmTlsEnabled// }" == "false" ]]; then
        # Helm chart with JWT authentication enabled (and authorization), 
        # and NO client-to-server TLS encryption
        helmChartFile="values_jwtAuth.yaml"
    else
        # Helm chart with JWT authentication enabled (and authorization), 
        # and WITH client-to-server TLS encryption
        helmChartFile="values_jwtAuthTls.yaml"
    fi
elif [[ "${helmAuthMethod// }" == "oauth" ]]; then
    if [[ -z "${helmTlsEnabled// }" || "${helmTlsEnabled// }" == "false" ]]; then
        # Helm chart with OAuth authentication enabled (and authorization), 
        # and NO client-to-server TLS encryption
        helmChartFile="values_oauth.yaml"
    else
        # Helm chart with OAuth authentication enabled (and authorization), 
        # and WITH client-to-server TLS encryption
        helmChartFile="values_oauthTls.yaml"
    fi
elif [[ "${helmAuthMethod// }" == "none" ]]; then
    # Basic helm chart with NO security feature enabled 
    # - No authN, No authZ, No TLS encryption
    helmChartFile="values_basic.yaml"    
fi
debugMsg "helmChartFile=${helmChartFile}"


echo "============================================================== "
echo "= "
echo "= Helm chart file \"${helmChartFile}\" will be used to deploy the Pulsar cluster with name \"${pulsarClstrName}\" ...  "
echo "= "


echo
echo "--------------------------------------------------------------"
echo ">> Install \"cert_manager\" as a prerequisite ... "
cmGhRelUrlBase="https://github.com/cert-manager/cert-manager/releases"
cmVersion=$(chkGitHubLatestRelVer "${cmGhRelUrlBase}/latest")
debugMsg "certManagerVersion=${cmVersion}"

kubectl apply -f "https://github.com/cert-manager/cert-manager/releases/download/v${cmVersion}/cert-manager.yaml"

pulsarClusterCreationErrCode=$?
if [[ "${pulsarClusterCreationErrCode}" -ne 0 ]]; then
    echo "[ERROR] Failed to install prerequisite cert manager !"
    errExit 80; 
fi


echo
echo "--------------------------------------------------------------"
echo ">> Add Pulsar helm to the local repository ... "
helm repo add datastax-pulsar https://datastax.github.io/pulsar-helm-chart
helm repo update datastax-pulsar

# Update the Helm chart file with the proper cluster name and docker image release version
pulsarRelease=$(getPropVal ${pulsarDeployPropFile} "pulsar.image")
helmDepUpdt=$(getPropVal ${pulsarDeployPropFile} "helm.dependency.update")
source ${pulsarDeployHomeDir}/update_helm.sh \
    -depUpdt "${helmDepUpdt}" \
    -file "${helmChartFile}" \
    -clstrName "${pulsarClstrName}" \
    -tgtRelease "${pulsarRelease}"
pulsarClusterCreationErrCode=$?
if [[ "${pulsarClusterCreationErrCode}" -ne 0 ]]; then
    echo "[ERROR] Failed to update Pulsar helm chart !"
    errExit 100; 
fi

echo
echo "--------------------------------------------------------------"
echo ">> Check if the Pulsar cluster named \"${pulsarClstrName}\" already exists ... "
clusterExistence=$(helm status ${pulsarClstrName} | grep STATUS | awk -F': ' '{print $2}')
echo "   clusterExistence=${clusterExistence}; upgradeExistingCluster=${upgradeExistingCluster}"

# There is no existing Pulsar cluster with the same name
if [[ "${clusterExistence}" != "deployed" ]]; then
    echo
    echo "--------------------------------------------------------------"
    echo ">> Install a Pulsar cluster named \"${pulsarClstrName}\" ... "
    helm install "${pulsarClstrName}" -f "${helmChartHomeDir}/${helmChartFile}" datastax-pulsar/pulsar
else
    if [[ ${upgradeExistingCluster} -ne 0 ]]; then
        echo
        echo "--------------------------------------------------------------"
        echo ">> Upgrade the existing Pulsar cluster named \"${pulsarClstrName}\" ... "
        helm upgrade --install "${pulsarClstrName}" -f "${helmChartHomeDir}/${helmChartFile}" datastax-pulsar/pulsar
    fi
fi

pulsarClusterCreationErrCode=$?
if [[ "${pulsarClusterCreationErrCode}" -ne 0 ]]; then
    echo "[ERROR] Failed to create/upgrade the Pulsar clsuter !"
    errExit 200; 
fi


echo
echo "--------------------------------------------------------------"
echo ">> Wait for Proxy deployment is ready ... "
## wati for Proxy deployment is ready (this approach doesn't work for K8s services)
kubectl wait --timeout=600s --for condition=Available=True deployment -l=component="proxy"

echo
echo ">> Wait for Proxy service is ready ... "
proxySvcName=$(kubectl get svc -l=component="proxy" -o name)
debugMsg "proxySvcName=${proxySvcName}"
## wait for Proxy service is assigned an external IP
until [ -n "$(kubectl get ${proxySvcName} -o jsonpath='{.status.loadBalancer.ingress[0].ip}')" ]; do
    sleep 2
done

if [[ -n "${proxySvcName// }" ]]; then
    echo
    echo "--------------------------------------------------------------"
    echo ">> Forward Pulsar Proxy ports to localhost ... "
    source ${pulsarDeployHomeDir}/forward_pulsar_proxy_port.sh \
        -act "start" \
        -proxySvc "${proxySvcName}" \
        -tlsEnabled "${helmTlsEnabled}"
fi

echo
