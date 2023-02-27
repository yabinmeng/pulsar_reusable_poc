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
PULSAR_WORKSHOP_HOMEDIR=$( cd -- "${CUR_SCRIPT_FOLDER}/../.." &> /dev/null && pwd )

source "${PULSAR_WORKSHOP_HOMEDIR}/_bash_utils_/utilities.sh"

usage() {
   echo
   echo "Usage: teardown_pulsar_cluster.sh [-h]"
   echo "                                  -clstrName <cluster_name>"
   echo "                                  -propFile <deployment_properties_file>"
   echo "       -h : Show usage info"
   echo "       -clstrName : Pulsar cluster name."
   echo "       -propFile  : Pulsar deployment properties file"
   echo
}

if [[ $# -gt 4 ]]; then
   usage
   errExit 20
fi

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -clstrName) pulsarClstrName=$2; shift ;;
      -propFile) pulsarPropFile=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done

if ! [[ -f ${pulsarPropFile// } ]]; then
    echo "[ERROR] Can't find the provided Pulsar termination properties file: \"${pulsarPropFile}\"!"
    errExit 40; 
fi

helmExistence=$(chkSysSvcExistence helm)
debugMsg "helmExistence=${helmExistence}"
if [[ ${helmExistence} -eq 0 ]]; then
    echo "[ERROR] 'helm' isn't installed on the local machine yet; please install it first!"
    errExit 50;
fi

if [[ -z ${pulsarClstrName// } ]]; then
    pulsarClstrName=$(getPropVal "pulsar.cluster.name")
    if [[ -z ${pulsarClstrName// } ]]; then
        echo "[ERROR] Pulsar cluster name cannot be empty! "
        errExit 60
    fi
fi
# Name must be lowercase
pulsarClstrName=$(echo "${pulsarClstrName}" | tr '[:upper:]' '[:lower:]')
debugMsg "pulsarClstrName=${pulsarClstrName}"

pulsarDeployHomeDir="${PULSAR_WORKSHOP_HOMEDIR}/cluster_deploy/pulsar"
helmChartHomeDir="${pulsarDeployHomeDir}/helm"

helmTlsEnabled=$(getPropVal ${pulsarPropFile} "helm.tls.enabled")
debugMsg "helmTlsEnabled=${helmTlsEnabled}"

proxySvcName=$(kubectl get svc -l=component="proxy" -o name)
debugMsg "proxySvcName=${proxySvcName}"

if [[ -n "${proxySvcName// }" ]]; then
    echo
    echo "--------------------------------------------------------------"
    echo ">> Terminate the forwarded Pulsar Proxy ports ... "
    source ${pulsarDeployHomeDir}/forward_pulsar_proxy_port.sh \
        -act "stop" \
        -proxySvc "${proxySvcName}" \
        -tlsEnabled "${helmTlsEnabled}"
    cd ${curDir}

    echo
    echo "--------------------------------------------------------------"
    echo ">> Uninstall the Pulsar cluster (\"${pulsarClstrName}\") from the K8s cluster ... "
    helmRepoExistence="$(chkHelmRepoExistence ${pulsarClstrName})"
    debugMsg "helmRepoExistence=${helmRepoExistence}"
    if [[ ${helmRepoExistence} -eq 1 ]]; then
        helm uninstall "${pulsarClstrName}" --wait
    fi
else
    echo
    echo "--------------------------------------------------------------"
    echo "[WARN] Doesn't detect Pulsar Proxy service. Likely there is no Pulsar cluster (\"${pulsarClstrName}\") deployed!"
fi

certManagerEnabled=$(getPropVal ${pulsarPropFile} "pulsar.teardown.cert.manager")
if [[ "${certManagerEnabled}" == "true" ]]; then
    cmGhRelUrlBase="https://github.com/cert-manager/cert-manager/releases"
    cmVersion=$(chkGitHubLatestRelVer "${cmGhRelUrlBase}/latest")
    debugMsg "certManagerVersion=${cmVersion}"

    echo
    echo "--------------------------------------------------------------"
    echo ">> Uninstall \"cert_manager\" as required for a secured Pulsar cluster install ... "
    kubectl delete -f "https://github.com/cert-manager/cert-manager/releases/download/v${cmVersion}/cert-manager.yaml"
fi

echo
