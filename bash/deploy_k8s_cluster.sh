#! /bin/bash

source ./_utilities.sh

### 
# This script is used to deploy a K8s cluster with 1 control plane node 
# and 3 worker nodes based on the selected k8s option. 
#
# Currently, the following K8s options are supported
# - minikube
# - kind
# - k3s
# - gke
# - aks
# - eks
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
   echo "Usage: deploy_k8s_cluster.sh [-h] -clstrName <cluster_name> -k8sOpt <k8s_option_name>"
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

kubeCtlExistence=$(chkSysSvcExistence kubectl)
debugMsg "kubeCtlExistence=${kubeCtlExistence}"
if [[ ${kubeCtlExistence} -eq 0 ]]; then
    echo "[ERROR] 'kubectl' isn't installed on the local machine yet; please install it first!"
    errExit 40;
fi

if [[ -z ${clstrName// } ]]; then
    clstrName=$(getDeployPropVal "k8s.cluster.name")
    if [[ -z ${clstrName// } ]]; then
        echo "[ERROR] K8s cluster name cannot be empty! "
        errExit 50
    fi
fi

if [[ -z ${k8sOpt// } ]]; then
    k8sOpt=$(getDeployPropVal "k8s.deploy.option")
    if [[ -z ${k8sOpt// } ]]; then
        echo "[ERROR] A K8s deployment option must be provided!"
        errExit 60
    fi
fi

if [[ ! " ${K8S_DEPLOY_OPTIONS[@]} " =~ " ${k8sOpt} " ]]; then
    echo "[ERROR] Invalid '-k8sOpt' parameter value."
    echo "        Must be one of the following values: \"${K8S_DEPLOY_OPTIONS[@]}\""
    errExit 70
fi

echo "============================================================== "
echo "= "
echo "= A \"${k8sOpt}\" based K8s cluster with name \"${clstrName}\" will be deployed ...  "
echo "= "

case ${k8sOpt} in
    minikube)
        echo "Deployment option ${k8sOpt} is to be implemented ..."
        ;;

    kind)
        preLoadImage=$(getDeployPropVal "kind.preload")
        if [[ "${preLoadImage}" == "true" ]]; then
            pulsarImage=$(getDeployPropVal "pulsar.image")
            source k8s/kind_create.sh -clstrName ${clstrName} -preLoad ${pulsarImage}
        else
            source k8/kind_create.sh -clstrName ${clstrName}
        fi
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
    *)
        echo "Unsupported K8s deployment option : \"${k8sOpt}\""
        ;;
esac

nginxIngressEnabled=$(getDeployPropVal "k8s.nginx.ingress")
if [[ "${nginxIngressEnabled}" == "true" ]]; then
    echo
    echo "--------------------------------------------------------------"
    echo ">> Deploy NGINX ingress controller in the current K8s cluster if needed ... "
    helm upgrade \
         --install ingress-nginx ingress-nginx \
         --repo https://kubernetes.github.io/ingress-nginx \
         --namespace ingress-nginx --create-namespace

    echo
    echo "--------------------------------------------------------------"
    echo ">> Deploy NGINX ingress controller in the current K8s cluster if needed ... "

    kubectl wait --namespace ingress-nginx \
        --for=condition=ready pod \
        --selector=app.kubernetes.io/component=controller \
        --timeout=60s
fi

echo
