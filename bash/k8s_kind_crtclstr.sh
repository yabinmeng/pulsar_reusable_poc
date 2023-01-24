#! /bin/bash

source ./_utilities.sh


### 
# This script is used to create a Kind (K8s) cluster with 1 control plane node 
# and 3 worker nodes
# 

if [[ -z "${WORKSHOP_HOMEDIR// }" ]]; then
    echo "Home direcotry is not set! Please make sure it is set properly in \"_setenv.sh\" file."
    errExit 10;
elif ! [[ -n "${DEPLOY_PROP_FILE// }" && -f "${WORKSHOP_HOMEDIR// }/${DEPLOY_PROP_FILE// }" ]]; then
    echo "[ERROR] Deployment properties file is not set or it can't be found!."
    errExit 11;
fi

usage() {
   echo
   echo "Usage: k8s_kind_crtclstr.sh [-h] [-clstrName <cluster_name>] [-preLoad <docker_image_name>]"
   echo "       -h : Show usage info"
   echo "       -clstrName : (Optional) Custom Kind cluster name."
   echo "       -preLoad   : (Optional) Preload the specified docker image"
   echo
}

if [[ $# -gt 4 ]]; then
   usage
   errExit 20
fi

echo

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -clstrName) clstrName=$2; shift ;;
      -preLoad) imageNameTag=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; errExit 30 ;;
   esac
   shift
done

dockerExistence=$(chkSvcExistence docker)
debugMsg "dockerExistence=${dockerExistence}"
if [[ ${dockerExistence} -eq 0 ]]; then
    echo "[ERROR] Docker engine isn't installed on the local machine yet; please install it first!"
    errExit 40;
fi

kindExistence=$(chkSvcExistence kind)
debugMsg "kindExistence=${kindExistence}"
if [[ ${kindExistence} -eq 0 ]]; then
    echo "[ERROR] Kind isn't installed on the local machine yet; please install it first!"
    errExit 50;
fi

if [[ -z "${clstrName// }" ]]; then
    tgtClstrName="kind"
else
    tgtClstrName="${clstrName}"
fi

echo "--------------------------------------------------------------"
echo ">> Create the Kind cluster with the name \"${tgtClstrName}\" ..."

clusterExistence=$(kind get clusters 2>&1 | grep "${tgtClstrName}")
if [[ -z "${clusterExistence}" ]] || [[ "${clusterExistence}" == "No kind clusters found."  ]]; then
    kindCfgFile="${WORKSHOP_HOMEDIR}/cluster_deploy/kubernetes/kind/cluster-config.yaml"
    if ! [[ -f "${kindCfgFile}" ]]; then
        echo "   [ERROR] Cannot find the Kind cluster configuration file!"
        errExit 60
    fi

    kind create cluster --name "${tgtClstrName}" --config "${WORKSHOP_HOMEDIR}/cluster_deploy/kubernetes/kind/cluster-config.yaml"
    if [[ $? -ne 0 ]]; then
        echo "   [ERROR] Cluster creation failed!"
        errExit 70
    else
        echo "   Cluster creation succeeded!"
        echo
    fi
else
    echo "   [WARN] The Kind cluster with the spcified name already exists!"
fi


# Preload docker image
if [[ -n "${imageNameTag// }" ]]; then
    inputImageArr=(${imageNameTag//:/ })
    imageName=${inputImageArr[0]}
    imageTag=${inputImageArr[1]}    
    debugMsg "imageName=${imageName}"
    debugMsg "imageTag=${imageTag}"

    echo
    echo "--------------------------------------------------------------"
    echo ">> Preload the specified docker image: \"${imageNameTag}\" ..."
    if [[ -z "${imageName// }" || -z "${imageTag// }" ]]; then
        echo "   [ERROR] Invalid docker image and version; must be in format \"<image_name>:<image_tag>\""
        errExit 80;
    fi
    
    # Check whether we need to pull the image first
    localImageChk=$(docker image ls | grep "${imageName}" | grep "${imageTag}")
    if [[ -z "${localImageChk// }" ]]; then
        echo "   Pull the specified docker image ..."
        docker image pull "${imageName}:${imageTag}"
    fi

    echo

    echo "   Load the specified docker image (${imageName}:${imageTag}) into the Kind cluster \"${tgtClstrName}\" ..."
    kind load --name ${tgtClstrName} docker-image "${imageName}:${imageTag}"

    if [[ $? -ne 0 ]]; then
        echo "   [ERROR] Docker image preload failed!"
        errExit 90
    fi
fi

## 
## NOTE: not needed, creating the Kind cluster will automatically create 
##       and set the client configs
##
# echo
# echo "--------------------------------------------------------------"
# echo ">> Set current K8s context to: \"kind-${tgtClstrName}\" ..."
# kubectl config set-context "kind-${tgtClstrName}"
