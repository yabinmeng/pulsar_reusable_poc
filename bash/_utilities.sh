#! /bin/bash

source ./_setenv.sh

##
# Supported K8s deployment options
# 
K8S_DEPLOY_OPTIONS=("minikube" "kind" "k3s" "gke" "aks" "eks")


##
# Show debug message 
# - $1 : the message to show
debugMsg() {
    if [[ "${DEBUG}" == "true" ]]; then
        if [[ $# -eq 0 ]]; then
            echo
        else
            echo "[Debug] $1"
        fi
    fi
}

##
# Exit bash execution with the specified return value
#
errExit() {
    echo
    exit $1
}

##
# Change the working directory
# - $1: the directory to change to
chgWorkingDir() {
    cd $1
}

## 
# Only applies to GitHub repo with releases
# - $1: the URL that points to the latest release
#       e.g. https://github.com/some/repo/releases/latest
chkGitHubLatestRelVer() {
    local verStr=$(curl -sI $1 | awk -F '/' '/^location/ {print  substr($NF, 1, length($NF)-1)}' | sed 's/[^0-9.]*//g' )
    echo "$verStr"
}

##
# Check if the required executeable (e.g. docker, kind) has been installed locally
#
chkSysSvcExistence() {
    local whichCmdOutput=$(which ${1})
    if [[ -z "${whichCmdOutput// }" ]]; then
        echo 0
    else
        echo 1
    fi   
}

##
# Check if the helm repo has been installed locally
#
chkHelmRepoExistence() {
    local localRepoName="$(helm list | tail +2 | grep ${1} | awk '{print $1}')"
    if [[ -z "${localRepoName// }" ]]; then
        echo 0
    else
        echo 1
    fi   
}

##
# Read the properties file and returns the value based on the key
# - $1: search key
getDeployPropVal() {
    local value=$(grep "${1}" ${WORKSHOP_HOMEDIR}/${DEPLOY_PROP_FILE} | grep -Ev "^#|^$" | cut -d'=' -f2)
    echo $value
}

##
# Terminate a forwarded port
# - $1: the port to terminate
termForwardedPort() {
    local pid=$(ps -ef | grep port-forward | grep "${1}" | awk '{print $2}')
    if [[ -n ${pid// } ]]; then
        kill -TERM ${pid}
    fi
}

##
# Install cert-manager
installCertManager() {
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
}