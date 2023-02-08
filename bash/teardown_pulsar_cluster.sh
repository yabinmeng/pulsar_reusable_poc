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
   echo "Usage: teardown_pulsar_cluster.sh [-h]"
   echo "                                  -clstrName <cluster_name>"
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


echo
echo "--------------------------------------------------------------"
echo ">> Uninstall a Pulsar cluster named \"${clstrName}\" in the current K8s cluster (if exists)... "
helmRepoExistence="$(chkHelmRepoExistence ${clstrName})"
debugMsg "helmRepoExistence=${helmRepoExistence}"

if [[ ${helmRepoExistence} -eq 1 ]]; then
    helm uninstall "${clstrName}"
fi

# Terminate any previously forwarded port, if any
termForwardedPort 6650
termForwardedPort 6651
termForwardedPort 8080
termForwardedPort 8843

echo
