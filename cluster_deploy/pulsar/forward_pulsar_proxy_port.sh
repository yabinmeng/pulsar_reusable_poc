#! /bin/bash

###
# Copyright DataStax, Inc.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
###

CUR_SCRIPT_FOLDER=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
PULSAR_WORKSHOP_HOMEDIR=$( cd -- "${CUR_SCRIPT_FOLDER}/../.." &> /dev/null && pwd )

source "${PULSAR_WORKSHOP_HOMEDIR}/_bash_utils_/utilities.sh"

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
   errExit 20
fi

while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -act) actTerm=$2; shift ;;
      -proxySvc) proxySvcName=$2; shift ;;
      -tlsEnabled) tlsEnabled=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done

if ! [[ "${actTerm}" == "start" || "${actTerm}" == "stop" ]]; then
    echo "[ERROR] Invalid value for '-act' option; must be either 'start' or 'stop'!"
    errExit 40;
fi

if [[ -z "${proxySvcName// }" ]]; then
    echo "[ERROR] Pulsar proxy service name must be provided!"
    errExit 50;
fi

if [[ -n "${tlsEnabled// }" && "${tlsEnabled}" != "true" && "${tlsEnabled}" != "false" ]]; then
    echo "[ERROR] Invalid value for '-tlsEnabled' option; must be either 'true' or 'false'!"
    errExit 60;
fi

if [[ "${actTerm}" == "start" ]]; then
    startProxyPortForward \
        ${proxySvcName} \
        ${tlsEnabled} \
        "pulsar_proxy_port_forward.nohup"
else
    stopProxyPortForward ${proxySvcName}
fi