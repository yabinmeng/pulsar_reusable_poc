#! /bin/bash

source ./_utilities.sh

### 
# This script is used to deploy a Pulsar cluster on a K8s cluster whose
# client context configuration is current (kubectl config current-context)
#

if [[ -z "${WORKSHOP_HOMEDIR}" ]]; then
    echo "Workshop home direcotry is not set! Please make sure it is set properly in \"_setenv.sh\"."
    errExit 10;
elif ! [[ -n "${DEPLOY_PROP_FILE}" && -f "${WORKSHOP_HOMEDIR}/${DEPLOY_PROP_FILE}" ]]; then
    echo "[ERROR] Deployment properties file is not set or it can't be found!."
    errExit 20;
fi

usage() {
   echo
   echo "Usage: deploy_pulsar_cluster.sh [-h]"
   echo "                                -clstrName <cluster_name>"
   echo "       -h : Show usage info"
   echo "       -clstrName : Pulsar cluster name."
   echo
}

if [[ $# -gt 2 ]]; then
   usage
   errExit 30
fi

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -clstrName) clstrName=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 40 ;;
   esac
   shift
done

echo
curDir=$(pwd)

helmExistence=$(chkSysSvcExistence helm)
debugMsg "helmExistence=${helmExistence}"
if [[ ${helmExistence} -eq 0 ]]; then
    echo "[ERROR] 'helm' isn't installed on the local machine yet; please install it first!"
    errExit 50;
fi

if [[ -z ${clstrName// } ]]; then
    clstrName=$(getDeployPropVal "pulsar.cluster.name")
    if [[ -z ${clstrName// } ]]; then
        echo "[ERROR] Pulsar cluster name cannot be empty! "
        errExit 60
    fi
fi
# Name must be lowercase
clstrName=$(echo "${clstrName}" | tr '[:upper:]' '[:lower:]')

helmChartHomeDir="${WORKSHOP_HOMEDIR}/cluster_deploy/pulsar_helm"

helmAuthMethod=$(getDeployPropVal "helm.auth.method")
helmTlsEnabled=$(getDeployPropVal "helm.tls.enabled")
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
echo "= Helm chart file \"${helmChartFile}\" will be used to deploy the Pulsar cluster with name \"${clstrName}\" ...  "
echo "= "


certManagerEnabled=$(getDeployPropVal "tools.cert_manager.enabled")
if [[ "${certManagerEnabled}" == "true" ]]; then
    cmGhRelUrlBase="https://github.com/cert-manager/cert-manager/releases"
    cmVersion=$(chkGitHubLatestRelVer "${cmGhRelUrlBase}/latest")
    debugMsg "certManagerVersion=${cmVersion}"

    echo
    echo "--------------------------------------------------------------"
    echo ">> Install \"cert_manager\" as required for a secured Pulsar cluster install ... "
    kubectl apply -f "https://github.com/cert-manager/cert-manager/releases/download/v${cmVersion}/cert-manager.yaml"
fi

echo
echo "--------------------------------------------------------------"
echo ">> Add Pulsar helm to the local repository ... "
helm repo add datastax-pulsar https://datastax.github.io/pulsar-helm-chart
helm repo update datastax-pulsar

# Update the Helm chart file with the proper cluster name and docker image release version
pulsarRelease=$(getDeployPropVal "pulsar.image")
helmDepUpdt=$(getDeployPropVal "helm.dependency.update")
source pulsar/update_helm.sh \
    -depUpdt "${helmDepUpdt}" \
    -file "${helmChartFile}" \
    -clstrName "${clstrName}" \
    -tgtRelease "${pulsarRelease}"
cd ${curDir}


echo
echo "--------------------------------------------------------------"
echo ">> Install a Pulsar cluster named \"${clstrName}\" ... "
helm upgrade --install "${clstrName}" -f "${helmChartHomeDir}/${helmChartFile}" datastax-pulsar/pulsar


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
    source k8s/forward_proxy_port.sh \
        -act "start" \
        -proxySvc "${proxySvcName}" \
        -tlsEnabled "${helmTlsEnabled}"
    cd ${curDir}
fi

echo
