#! /bin/bash


### 
# This script is used to tear down a K8s cluster that was created by
# "deploy_k8s_cluster.sh" script. 
#
# Currently, the following K8s options are supported
# - minikube
# - kind
# - k3s
# - gke
# - aks
# - eks
# 

source ./_utilities.sh
if [[ -z "${WORKSHOP_HOMEDIR// }" ]]; then
    echo "Home direcotry is not set! Please make sure it is set properly in \"_setenv.sh\" file."
    errExit 10;
elif ! [[ -n "${DEPLOY_PROP_FILE// }" && -f "${WORKSHOP_HOMEDIR// }/${DEPLOY_PROP_FILE// }" ]]; then
    echo "[ERROR] Deployment properties file is not set or it can't be found!."
    errExit 11;
fi

usage() {
   echo
   echo "Usage: teardown_k8s_cluster.sh [-h] -clstrName <cluster_name> -k8sOpt <k8s_option_name>"
   echo "       -h : Show usage info"
   echo "       -clstrName : K8s cluster name."
   echo "       -k8sOpt    : K8s deployment option."
   echo
}

if [[ $# -gt 4 ]]; then
   usage
   errExit 20
fi

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -clstrName) clstrName=$2; shift ;;
      -k8sOpt) k8sOpt=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done

if [[ -z ${clstrName// } ]]; then
    clstrName=$(getDeployPropVal "k8s.cluster.name")
    if [[ -z ${clstrName// } ]]; then
        echo "[ERROR] K8s cluster name cannot be empty! "
        errExit 40
    fi
fi

if [[ -z ${k8sOpt// } ]]; then
    k8sOpt=$(getDeployPropVal "k8s.deploy.option")
    if [[ -z ${k8sOpt// } ]]; then
        echo "[ERROR] A K8s deployment option must be provided!"
        errExit 50
    fi
fi

if [[ ! " ${K8S_DEPLOY_OPTIONS[@]} " =~ " ${k8sOpt} " ]]; then
    echo "[ERROR] Invalid '-k8sOpt' parameter value."
    echo "        Must be one of the following values: \"${K8S_DEPLOY_OPTIONS[@]}\""
    errExit 60
fi



echo "============================================================== "
echo "= "
echo "= A \"${k8sOpt}\" based K8s cluster with name \"${clstrName}\" will be deleted ...  "
echo "= "
echo

echo "You're about to execute a destructive operation that can't be undone."
echo "Are you certain you want to continue? [y|n|yes|no]"
read -r prompt
if [[ "${prompt// }" == "yes" || "${prompt// }" == "y" ]]; then
    case ${k8sOpt} in
        minikube)
            echo "Deployment option ${k8sOpt} is to be implemented ..."
            ;;

        kind)
            source k8s_kind_delclstr.sh -clstrName  ${clstrName}
            ;;

        k3s)
            echo "Deployment option ${k8sOpt} is to be implemented ..."
            ;;

        gk2)
            echo "Deployment option ${k8sOpt} is to be implemented ..."
            ;;

        aks)
            echo "Deployment option ${k8sOpt} is to be implemented ..."
            ;;
            
        eks)
            echo "Deployment option ${k8sOpt} is to be implemented ..."
            ;;
esac
fi

echo
