#! /bin/bash

DEBUG=false

echo "Running utilities"
##
# Supported K8s deployment options
# 
K8S_DEPLOY_OPTIONS=("kind" "gke" "aks" "eks")

# 6650: Pulsar native protocol port
# 8080: Pulsar web admin port
# 9092: Kafka client listenting port
# 8081: Kafka schema registry port (this is also TLS port)
PULSAR_PROXY_PORTS=(6650 8080 9092 8081)
# 6651: Pulsar native protocol TLS port
# 8443: Pulsar web admin TLS port
# 9093: Kafka client listenting TLS port
# 8081: Kafka schema registry TLS port
PULSAR_PROXY_PORTS_TLS=(6651 8443 9093)


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
# Two input prarameters:
# - 1st parameter: the property file to scan
# - 2nd parameter: the key to search for
getPropVal() {
    local propFile=$1
    local searchKey=$2
    local value=$(grep "${searchKey}" ${propFile} | grep -Ev "^#|^$" | cut -d'=' -f2)
    echo $value
}

##
# Forward the Pulsar proxy service ports to localhost
# - $1: pulsar proxy service name
# - $2: TLS enabled
# - $3: nohup output file 
startProxyPortForward() {
    local proxySvcName=${1}
    local tlsEnabled=${2}
    local nohupOutFile=${3}
    touch ${nohupOutFile}
    
    echo "   forwarding non-TLS ports (${PULSAR_PROXY_PORTS[@]}) ..."
    for port in "${PULSAR_PROXY_PORTS[@]}"; do
        # echo "   - port ${port}"
        kubectl port-forward ${proxySvcName} ${port}:${port} >> ${nohupOutFile} 2>&1 &
    done

    if [[ "${tlsEnabled}" == "true" ]]; then
        echo "   forwarding TLS ports (${PULSAR_PROXY_PORTS_TLS[@]})"
        for port in "${PULSAR_PROXY_PORTS_TLS[@]}"; do
            # echo "   - port ${port}"
            kubectl port-forward ${proxySvcName} ${port}:${port} >> ${nohupOutFile} 2>&1 &
        done
    fi
}

##
# Terminate the forwarded ports of the Pulsar proxy service
# - $1: pulsar proxy service name
stopProxyPortForward() {
    local proxySvcName=${1}
    for port in "${PULSAR_PROXY_PORTS[@]}" "${PULSAR_PROXY_PORTS_TLS[@]}"; do
        local pid=$(ps -ef | grep port-forward | grep "${port}" | awk '{print $2}')
        if [[ -n ${pid// } ]]; then
            kill -TERM ${pid}
        fi
    done
}

##
# Install cert-manager
installCertManager() {
    certManagerEnabled=$(getPropVal "tools.cert_manager.enabled")
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


repeatSpace() {
    head -c $1 < /dev/zero | tr '\0' ' '
}

# Three parameter: 
# - 1st parameter: the message to print for execution status purpose
# - (Optional) 2nd parameter: the number of the leading spaces
#              if not specified, there is no leading space
# - (Optional) 3rd parameter: the log file to append the message to
#              if not specified or not a valid file, print the message to stdout
# - (Optional) 4th parameter: indicates whether to write output to stdout as well
#              when appending to the file at the same time
#              * true: output to stdout while appending to the log file
#              * false: only append to the log file; do not write output to stdout
outputMsg() {
    if [[ $# -eq 0 || $# -gt 4 ]]; then
        echo "[Error] Incorrect usage of outputMsg()."
    else
        leadingSpaceStr=""
        if [[ -n $2 && $2 -gt 0 ]]; then
            leadingSpaceStr=$(repeatSpace $2)            
        fi

        if [[ -n "$3" && -f "$3" ]]; then
            echo "$leadingSpaceStr$1" >> $3
            if [[ "$4" == "true" ]]; then
                echo "$leadingSpaceStr$1"
            fi
        else
            echo "$leadingSpaceStr$1"
        fi
    fi
}