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
chkSvcExistence() {
    local whichCmdOutput=$(which ${1})
    if [[ -z "${whichCmdOutput// }" ]]; then
        echo 0
    else
        echo 1
    fi   
}

##
# Read the properties file and returns the value based on the key
# - $1: search key
function getDeployPropVal {
    value=$(grep "${1}" ${WORKSHOP_HOMEDIR}/${DEPLOY_PROP_FILE} | grep -Ev "^#|^$" | cut -d'=' -f2)
    echo $value
}