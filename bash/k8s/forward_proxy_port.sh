#! /bin/bash

source ./_utilities.sh

### 
# This script is used to start or stop K8s port forward to locahhost 
# for the Pulsar proxy servcie
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
   echo "Usage: forward_proxy_port.sh [-h]"
   echo "                             -act <start|stop>"
   echo "                             -proxySvc <proxy_servie_name>"
   echo "                             -tlsEnabled <true|false>"
   echo "       -h : Show usage info"
   echo "       -act: start or stop port forwarding"
   echo "       -proxySvc: Pulsar proxy servcie name"
   echo "       -tlsEnabled: Whether TLS port needs to be forwarded"
   echo
}

if [[ $# -eq 0 || $# -gt 6 ]]; then
   usage
   errExit 30
fi

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -act) actTerm=$2; shift ;;
      -proxySvc) proxySvcName=$2; shift ;;
      -tlsEnabled) tlsEnabled=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 40 ;;
   esac
   shift
done

if ! [[ "${actTerm}" == "start" || "${actTerm}" == "stop" ]]; then
    echo "[ERROR] Invalid value for '-act' option; must be either 'start' or 'stop'!"
    errExit 50;
fi

if [[ -z "${proxySvcName// }" ]]; then
    echo "[ERROR] Pulsar proxy service name must be provided!"
    errExit 60;
fi

if [[ -n "${tlsEnabled// }" && "${tlsEnabled}" != "true" && "${tlsEnabled}" != "false" ]]; then
    echo "[ERROR] Invalid value for '-tlsEnabled' option; must be either 'true' or 'false'!"
    errExit 60;
fi

if [[ "${actTerm}" == "start" ]]; then
    startProxyPortForward \
        ${proxySvcName} \
        ${tlsEnabled} \
        "${WORKSHOP_HOMEDIR}/logs/proxy_port_forward.nohup"
else
    stopProxyPortForward ${proxySvcName}
fi