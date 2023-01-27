#! /bin/bash

source ./_utilities.sh


### 
# This script is used to update Pulsar helm chart templates for
# 1) Chart dependency update
# 2) LunaStreaming version update
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
   echo "Usage: update_helm.sh [-h]"
   echo "                             -chart </path/to/helm/chart/file>"
   echo "                             [-depUpdt]"
   echo "                             [-clstrName <cluster_name>]"
   echo "                             -tgtRelease <version_string>"
   echo "       -h : Show usage info"
   echo "       -chart:    : Helm chart file to update"
   echo "       -depUpdt   : (Optional) Update helm chart dependencies. Skip dependency update if not specified."
   echo "       -clstrName : (Optional) Update Pulsar cluster name (default: \"pulsar\")."
   echo "       -tgtRelease : Update to a specific Pulsar release version."
   echo
}

if [[ $# -gt 7 ]]; then
   usage
   exit 20
fi

echo

depUpdt=0
while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -depUpdt) depUpdt=1 ;;
      -file) chartFile=$2; shift ;;
      -clstrName) clstrName=$2; shift ;;
      -tgtRelease) tgtRelease=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; exit 30 ;;
   esac
   shift
done

debugMsg "chartFile=${chartFile}"
debugMsg "depUpdat=${depUpdt}"
debugMsg "clstrName=${clstrName}"
debugMsg "tgtRelease=${tgtRelease}"

if [[ -z "{tgtRelName// }" || -z  "{tgtRelName// }" ]]; then
   echo "Incorrect specified target docker image release information: \"tgtRelease\". It must be in format <release_name>:<release_version>!"
   exit 40
fi

HELM_HOMEDIR="${WORKSHOP_HOMEDIR}/cluster_deploy/pulsar_helm"
chgWorkingDir "${HELM_HOMEDIR}"

if ! [[ -f "${chartFile}" ]]; then
   echo "--------------------------------------------------------------"
   echo ">> Can't find the specified Pulsar helm chart file. Create one from the template ..."
   if ! [[ -f "${helmChartHomeDir}/template/${helmChartFile}.tmpl" ]]; then
      echo "  [ERROR] Can't find the required Pulsar helm chart file template!"
      echo "          (\"${HELM_HOMEDIR}/template/${helmChartFile}.tmpl\")"
      errExit 50
   else
      cp "${HELM_HOMEDIR}/template/${helmChartFile}.tmpl" "${HELM_HOMEDIR}/${helmChartFile}"
   fi
   echo
else
   mkdir -p bkup
   cp "${helmChartFile}" "bkup/${helmChartFile}_$(date +%Y%m%d)"
fi

# Update Pulsar helm chart dependency if needed
if [[ -f "Chart.yaml" ]]; then
   chartYamlFileExists=1
fi

echo "--------------------------------------------------------------"
if [[ ${depUpdt} -eq 1 && ${chartYamlFileExists} -eq 1 ]]; then
   echo ">> Update chart depdendency (depUpdt: ${depUpdt}, chartYamlFileExists: ${chartYamlFileExists}) "
   helm dependency update
   if [[ $? -ne 0 ]]; then
      echo "   [ERROR] Chart dependency udpate failed!"
   fi
else
   echo ">> Skip chart depdendency update (depUpdt: ${depUpdt}, chartYamlFileExists: ${chartYamlFileExists}) ... "
fi   

## 
# Update release tag in a particular "values.yaml" file
# - $1: Helm chart name
# - $2: Pulsar cluster name
# - $3: Pulsar image release name in "format <name>:<version>""
function updatePulsarHelmChart() {
   tgtRelInfoArr=($(echo "$2" | awk -F':' '{print $1 " "  $2}'))
   tgtRelName=${tgtRelInfoArr[0]}
   tgtRelVer=${tgtRelInfoArr[1]}
   debugMsg "tgtRelName=${tgtRelName}"
   debugMsg "tgtRelVer=${tgtRelVer}"

   # Update Pulsar cluster name
   sed -i "s/fullnameOverride:.*/fullnameOverride: ${clstrName}/g" $1 

   # Update image name
   curNameArr=()
   for name in $( grep "repository:" $1 | awk -F': ' '{ print $NF}' | uniq); do
      curNameArr+=("${name}")
   done
   for curName in "${curNameArr[@]}"; do
      if [[ "${curName}" != "${tgtRelName}"  ]]; then
         echo "   - replacing release name \"${curName}\" with \"${tgtRelName}\""
         # use '#' as separater because release name contains special character '/'
         sed -i "s#repository: ${curName}#repository: ${tgtRelName}#g" $1 
      fi
   done

   # Update image version
   curVerArr=()
   for version in $( grep "tag:" $1 | awk -F': ' '{ print $NF}' | uniq); do
      curVerArr+=("${version}")
   done

   for curVer in "${curVerArr[@]}"; do
      if [[ "${curVer}" != "${tgtRelVer}"  ]]; then
         echo "   - replacing release version \"${curVer}\" with \"${tgtRelVer}\""
         sed -i "s/tag: ${curVer}/tag: ${tgtRelVer}/g" $1 
      fi
   done
}

echo
echo "--------------------------------------------------------------"
echo ">> Update Pulsar image release to \"${tgtRelease}\""
updatePulsarHelmChart "${chartFile}" "${tgtRelease}"
