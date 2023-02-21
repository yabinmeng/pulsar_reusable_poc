#! /bin/bash

source "./utilities.sh"

### 
# This script is used to deploy Jaeger open tracing operator and 
# deployment in the current K8s cluster
#
# NOTE: This has challenges for local test
#       - Jaeger agent receives data from UDP port 6831
#       - When deployin Jaeger with the AllInOne strategy, all Jaeger
#         components (Jaeger agent, Jaeger collector, etc.) are running
#         in one Pod in K8s
#       - K8s port-forwarding doesn't work with UDP
#       - If on Linux, this can be addressed by creating a load balancer
#         service
#       - But on Mac, docker doesn't expose the docker network to host
#         so if the K8s cluster is running on Mac, the application running
#         on the local Mac can't communicate with the Jaeger agent
#         (e.g. https://kind.sigs.k8s.io/docs/user/loadbalancer/) 
#
#       - One way to address this is to deploy the application in K8s. This
#         is a bit involved on the application side which requires extra 
#         steps of building the docker image, publishing it, deploying the 
#         application in K8s, etc.
#   
#       - The other simple way is to use docker jaeger container directly
#         (https://www.jaegertracing.io/docs/1.6/getting-started/#all-in-one-docker-image)
#       
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
   echo "Usage: deploy_jaeger_k8s.sh [-h]"
   echo "       -h : Show usage info"
   echo
}

if [[ $# -gt 2 ]]; then
   usage
   errExit 20
fi

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done

echo

kubeCtlExistence=$(chkSysSvcExistence kubectl)
debugMsg "kubeCtlExistence=${kubeCtlExistence}"
if [[ ${kubeCtlExistence} -eq 0 ]]; then
    echo "[ERROR] 'kubectl' isn't installed on the local machine yet; please install it first!"
    errExit 40;
fi

helmExistence=$(chkSysSvcExistence helm)
debugMsg "helmExistence=${helmExistence}"
if [[ ${helmExistence} -eq 0 ]]; then
    echo "[ERROR] 'helm' isn't installed on the local machine yet; please install it first!"
    errExit 50;
fi

echo "============================================================== "
echo "= "
echo "= Jaeger K8s operator (latest) will be installed in the current K8s cluster ...  "
echo "= "

# Install cert manager if needed
installCertManager

jaegerGhRelUrlBase="https://github.com/jaegertracing/jaeger-operator/releases"
jaegerVersion=$(chkGitHubLatestRelVer "${jaegerGhRelUrlBase}/latest")
debugMsg "jaegerVersion=${jaegerVersion}"

echo
echo "--------------------------------------------------------------"
echo ">> Install Jaeger operator verion \"${jaegerVersion}\" ... "
kubectl create namespace observability
kubectl create -f "${jaegerGhRelUrlBase}/download/v${jaegerVersion}/jaeger-operator.yaml" -n observability

echo
echo "--------------------------------------------------------------"
echo ">> Wait for Jaeger operator is ready ... "
kubectl wait -n observability --timeout=30s --for condition=established crd jaegers.jaegertracing.io
kubectl wait -n observability --timeout=30s --for condition=Available=True deployment -l name=jaeger-operator

echo
echo "--------------------------------------------------------------"
echo ">> Deploy a Jaeger instance with the (default) AllInOne strategy ... "
# https://www.jaegertracing.io/docs/1.17/operator/#deployment-strategies
kubectl apply -f - <<EOF
apiVersion: jaegertracing.io/v1
kind: Jaeger
metadata:
  name: simple-ingress
spec:
  ingress:
    enabled: true
EOF

echo
echo "--------------------------------------------------------------"
echo ">> Do port forward for jaeger agent and jaeger query component ... "
## 
# TODO: port forwarding on UDP is not supported yet. Consider using Ingress ... 
# kubectl port-forward "$(kubectl get pods -l=app="jaeger" -o name)" 6831:6831 &
nohup kubectl port-forward "$(kubectl get pods -l=app="jaeger" -o name)" 16686:16686 > jaeger_port_forward.nohup &

echo
