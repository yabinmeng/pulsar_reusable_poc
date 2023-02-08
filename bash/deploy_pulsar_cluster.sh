#! /bin/bash

source "./_utilities.sh"

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

helmSecEnabled=$(getDeployPropVal "helm.security.enabled")
helmOAuthEnabled=$(getDeployPropVal "helm.oauth")
helmStarlightEnabled=$(getDeployPropVal "helm.starlight.enabled")

debugMsg "helmSecEnabled=${helmSecEnabled}"
debugMsg "helmOAuthEnabled=${helmOAuthEnabled}"
debugMsg "helmStarlightEnabled=${helmStarlightEnabled}"

helmChartHomeDir="${WORKSHOP_HOMEDIR}/cluster_deploy/pulsar_helm"

# Choose the right Pulsar helm chart based on the settings
if [[ "${helmStarlightEnabled}" == "true" ]]; then
    if [[ "${helmSecEnabled}" == "true" ]]; then
        helmChartFile="values_starlight_sec.yaml"
    else
        helmChartFile="values_starlight_nosec.yaml"
    fi
else
    if [[ "${helmSecEnabled}" == "true" ]]; then
        if [[ "${helmOAuthEnabled}" == "true" ]]; then
            helmChartFile="values_sec_oauth.yaml"
        else
            helmChartFile="values_sec.yaml"
        fi
    else
        helmChartFile="values_nosec.yaml"
    fi
fi



echo "============================================================== "
echo "= "
echo "= Helm chart file \"${helmChartFile}\" will be used to deploy the Pulsar cluster with name \"${clstrName}\" ...  "
echo "= "


if [[ "${helmSecEnabled}" == "true" ]]; then
    certManagerEnabled=$(getDeployPropVal "tools.cert_manager.enabled")
    if [[ "${certManagerEnabled}" == "true" ]]; then
        cmGhRelUrlBase="https://github.com/cert-manager/cert-manager/releases"
        cmVersion=$(chkGitHubLatestRelVer "${cmGhRelUrlBase}/latest")
        debugMsg "certManagerVersion=${cmVersion}"

        echo
        echo "--------------------------------------------------------------"
        echo ">> Install \"cert_manager\" as required for a secured Pulsar cluster install ... "
        helm repo add jetstack https://charts.jetstack.io
        helm repo update jetstack
        helm upgrade --install \
             cert-manager jetstack/cert-manager \
             --namespace cert-manager \
             --create-namespace \
             --version "v${cmVersion}" \
             --set installCRDs=true
    fi
fi


echo
echo "--------------------------------------------------------------"
echo ">> Add Pulsar helm to the local repository ... "
helm repo add datastax-pulsar https://datastax.github.io/pulsar-helm-chart
helmRepoUpdt=$(getDeployPropVal "helm.repo.update")
if [[ "${helmRepoUpdt}" == "true" ]]; then
    helm repo update datastax-pulsar
fi

# Update the Helm chart file with the proper cluster name and docker image release version
pulsarRelease=$(getDeployPropVal "pulsar.image")
helmDepUpdt=$(getDeployPropVal "helm.dependency.update")
if [[ "${helmDepUpdt}" == "true" ]]; then
    source pulsar/update_helm.sh \
        -depUpdt \
        -file "${helmChartFile}" \
        -clstrName "${clstrName}" \
        -tgtRelease "${pulsarRelease}"
else
    source pulsar/update_helm.sh \
        -file "${helmChartFile}" \
        -clstrName "${clstrName}" \
        -tgtRelease "${pulsarRelease}"
fi

echo
echo "--------------------------------------------------------------"
echo ">> Install a Pulsar cluster named \"${clstrName}\" in the current K8s cluster ... "
helm upgrade --install "${clstrName}" -f "${helmChartHomeDir}/${helmChartFile}" datastax-pulsar/pulsar

echo
echo "--------------------------------------------------------------"
echo ">> Wait for Proxy pod is ready and then do port forwarding on port 6650 ... "
kubectl wait -n default --timeout=180s --for condition=Available=True deployment -l=component="proxy"
kubectl port-forward $(kubectl get pods -l=component="proxy" -o name) 6650:6650 &
kubectl port-forward $(kubectl get pods -l=component="proxy" -o name) 6651:6651 &
kubectl port-forward $(kubectl get pods -l=component="proxy" -o name) 8080:8080 &
kubectl port-forward $(kubectl get pods -l=component="proxy" -o name) 8843:8843 &

echo