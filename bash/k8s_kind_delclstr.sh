#! /bin/bash

source ./_utilities.sh


### 
# This script is used to delete a Kind (K8s) cluster that was created by 
# the "k8s_kind_crtclstr.sh" script
# 


if [[ -z "${WORKSHOP_HOMEDIR// }" ]]; then
    echo "[ERROR] Home direcotry is not set! Please make sure it is set properly in \"_setenv.sh\" file."
    exit 10;
fi

usage() {
   echo
   echo "Usage: k8s_kind_delclstr.sh [-h] [-clstrName <cluster_name>]"
   echo "       -h : Show usage info"
   echo "       -clstrName : (Optional) Custom Kind cluster name."
   echo
}

if [[ $# -gt 2 ]]; then
   usage
   exit 20
fi

echo

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -clstrName) clstrName=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done

dockerExistence=$(chkSvcExistence docker)
debugMsg "dockerExistence=${dockerExistence}"
if [[ ${dockerExistence} -eq 0 ]]; then
    echo "[ERROR] Docker engine isn't installed on the local machine yet; please install it first!"
    exit 40;
fi

kindExistence=$(chkSvcExistence kind)
debugMsg "kindExistence=${kindExistence}"
if [[ ${kindExistence} -eq 0 ]]; then
    echo "[ERROR] Kind isn't installed on the local machine yet; please install it first!"
    exit 50;
fi

if [[ -z "${clstrName// }" ]]; then
    tgtClstrName="kind"
else
    tgtClstrName="${clstrName}"
fi

echo
echo "--------------------------------------------------------------"
echo ">> Delete the Kind cluster with the name \"${tgtClstrName}\" ..."

clusterExistence=$(kind get clusters 2>&1 | grep "${tgtClstrName}")
if [[ -n "${clusterExistence}" ]] && [[ "${clusterExistence}" != "No kind clusters found."  ]]; then
    kind delete cluster --name ${tgtClstrName}
    if [[ $? -ne 0 ]]; then
        echo "   [ERROR] Cluster deletion failed!"
        errExit 60
    fi
else
    echo "   [WARN] The Kind cluster with the spcified name does not exist!"
fi

## 
## NOTE: not needed, deleting Kind cluster will automatically unset and 
##       delete the client configs
##
# echo
# echo "--------------------------------------------------------------"
# echo ">> Remove the corresponding K8s client configuration for this cluster ..."
# kubectl config delete-context "kind-${tgtClstrName}"
# kubectl config delete-cluster "kind-${tgtClstrName}"
