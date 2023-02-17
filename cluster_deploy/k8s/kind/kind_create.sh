#! /bin/bash

if [[ -z "${WORKSHOP_HOMEDIR// }" ]]; then
    echo "Workshop home direcotry is not set! Please run \"deploy_k8s_cluster.sh\" instead and"
    echo "   make sure the workshop home directory is properly set in \"_setenv.sh\" file."
    errExit 100;
fi

source ${WORKSHOP_HOMEDIR}/bash/utilities.sh

### 
# This script is used to create a local Kind (K8s) cluster with
# 1 control plan node and 3 worker nodes
# 

usage() {
   echo
   echo "Usage: kind_create.sh [-h]"
   echo "                      [-clstrName <cluster_name>]"
   echo "                      [-preLoad <docker_image_name>]"
   echo "       -h : (Optional) Show usage info."
   echo "       -clstrName : (Optional) Custom Kind cluster name."
   echo "       -preLoadImage : (Optional) Preload the specified docker image"
   echo
}

if [[ $# -eq 0 || $# -gt 4 ]]; then
   usage
   errExit 110
fi

echo

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -clstrName) clstrName=$2; shift ;;
      -preLoadImage) imageNameTag=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; errExit 120 ;;
   esac
   shift
done

dockerExistence=$(chkSysSvcExistence docker)
debugMsg "dockerExistence=${dockerExistence}"
if [[ ${dockerExistence} -eq 0 ]]; then
    echo "[ERROR] Docker engine isn't installed on the local machine yet; please install it first!"
    errExit 130;
fi

kindExistence=$(chkSysSvcExistence kind)
debugMsg "kindExistence=${kindExistence}"
if [[ ${kindExistence} -eq 0 ]]; then
    echo "[ERROR] Kind isn't installed on the local machine yet; please install it first!"
    errExit 140;
fi


echo "--------------------------------------------------------------"
echo ">> Create the Kind cluster with the name \"${clstrName}\" ..."

clusterExistence=$(kind get clusters 2>&1 | grep "${clstrName}")
if [[ -z "${clusterExistence}" ]] || [[ "${clusterExistence}" == "No kind clusters found."  ]]; then
    kindCfgFile="${WORKSHOP_HOMEDIR}/cluster_deploy/kubernetes/kind/cluster-config.yaml"
    if ! [[ -f "${kindCfgFile}" ]]; then
        echo "   [ERROR] Cannot find the Kind cluster configuration file!"
        errExit 150
    fi

    kind create cluster --name "${clstrName}" --config "${WORKSHOP_HOMEDIR}/cluster_deploy/kubernetes/kind/cluster-config.yaml"
    if [[ $? -ne 0 ]]; then
        echo "   [ERROR] Cluster creation failed!"
        errExit 160
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
        errExit 170;
    fi
    
    # Check whether we need to pull the image first
    localImageChk=$(docker image ls | grep "${imageName}" | grep "${imageTag}")
    if [[ -z "${localImageChk// }" ]]; then
        echo "   Pull the specified docker image ..."
        docker image pull "${imageName}:${imageTag}"
    fi

    echo

    echo "   Load the specified docker image (${imageName}:${imageTag}) into the Kind cluster \"${clstrName}\" ..."
    kind load --name ${clstrName} docker-image "${imageName}:${imageTag}"

    if [[ $? -ne 0 ]]; then
        echo "   [ERROR] Docker image preload failed!"
        errExit 180
    fi
fi

## 
## NOTE: not needed, creating the Kind cluster will automatically create 
##       and set the client configs
##
# echo
# echo "--------------------------------------------------------------"
# echo ">> Set current K8s context to: \"kind-${clstrName}\" ..."
# kubectl config set-context "kind-${clstrName}"
