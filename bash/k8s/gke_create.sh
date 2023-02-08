#! /bin/bash

if [[ -z "${WORKSHOP_HOMEDIR// }" ]]; then
    echo "Workshop home direcotry is not set! Please run \"deploy_k8s_cluster.sh\" instead and"
    echo "   make sure the workshop home directory is properly set in \"_setenv.sh\" file."
    errExit 200;
fi

source ${WORKSHOP_HOMEDIR}/bash/_utilities.sh


### 
# This script is used to create a GKE (K8s) cluster
# 

usage() {
   echo
   echo "Usage: gke_create.sh [-h]"
   echo "                     -clstrName <cluster_name>"
   echo "                     [-project <gcp_project_name>]"
   echo "                     [-regOrZone] <region_or_zone_name>"
   echo "                     [-nodeType <gcp_machine_type>]"
   echo "                     [-nodeCnt <k8s_node_count>]"
   echo "       -h : Show usage info"
   echo "       -clstrName : GKE cluster name"
   echo "       -project : (Optional) GCP project name (use default name if not specified)"
   echo "       -regOrZone : (Optional) GCP region or zone (region:<region_name>, or zone:<zone_name>)"
   echo "       -nodeType : (Optional) GKE node machine type (use default type if not specified)"
   echo "       -nodeCnt : (Optional) GKE node count (use default count if not specified)"
   echo
}

if [[ $# -eq 0 || $# -gt 10 ]]; then
   usage
   errExit 210
fi

echo

isRegional=0
setActiveSvcAcct=0
while [[ "$#" -gt 0 ]]; do
   case $1 in
      -h) usage; exit 0 ;;
      -clstrName) clstrName=$2; shift ;;
      -regOrZone) regOrZoneName=$2; shift ;;
      -project) projectName=$2; shift ;;
      -svcAcct) svcAcct=$2; shift ;;
      -setActiveSvcAcct) setActiveSvcAcct=1; ;;
      -nodeType) nodeType=$2; shift ;;
      -nodeCnt) nodeCnt=$2; shift ;;
      *) echo "[ERROR] Unknown parameter passed: $1"; errExit 220 ;;
   esac
   shift
done

debugMsg "clstrName=${clstrName}"
debugMsg "regOrZoneName=${regOrZoneName}"
debugMsg "projectName=${projectName}"
debugMsg "nodeType=${nodeType}"
debugMsg "nodeCnt=${nodeCnt}"

gcloudExistence=$(chkSysSvcExistence gcloud)
debugMsg "gcloudExistence=${gcloudExistence}"
if [[ ${gcloudExistence} -eq 0 ]]; then
    echo "[ERROR] gcloud isn't installed on the local machine yet; please install it first!"
    errExit 230;
fi

validRegOrZoneParam=0
if [[ -n "${regOrZoneName// }" ]]; then
    regOrZoneNameStrArr=(${regOrZoneName//:/ })
    if [[ ${#regOrZoneNameStrArr[@]} -eq 2 ]]; then
        regOrZoneTypeStr=${regOrZoneNameStrArr[0]}
        regOrZoneNameStr=${regOrZoneNameStrArr[1]}    
        debugMsg "regOrZoneTypeStr=${regOrZoneTypeStr}"
        debugMsg "regOrZoneNameStr=${regOrZoneNameStr}"

        if [[ "${regOrZoneTypeStr}" == "region" || "${regOrZoneTypeStr}" == "zone" ]]; then
            validRegOrZoneParam=1
        fi
    fi
fi
if [[ ${validRegOrZoneParam} -eq 0 ]]; then
    echo "[ERROR] Invalid region or zone name string. It must be in format \"region:<region_name>\" or \"zone:<zone_name>\"!"
    errExit 240;
fi

# trunk-ignore(shellcheck/SC2317)
echo "--------------------------------------------------------------"
echo ">> Create the GKE cluster with the name \"${clstrName}\" ..."

clusterExistence=$(gcloud beta container clusters list 2>&1 | grep "${clstrName}")
if [[ -z "${clusterExistence}" ]]; then
    if [[ "${regOrZoneTypeStr}" == "region" ]]; then
        gcloud beta container clusters create ${clstrName} \
            --project ${projectName}  \
            --region ${regOrZoneNameStr} \
            --machine-type ${nodeType} \
            --num-nodes ${nodeCnt} \
            --addons HorizontalPodAutoscaling,HttpLoadBalancing,GcePersistentDiskCsiDriver \
            --enable-autoupgrade \
            --enable-autorepair \
            --enable-shielded-nodes \
            --max-surge-upgrade 1 \
            --max-unavailable-upgrade 0
    else
        gcloud beta container clusters create ${clstrName} \
            --project ${projectName}  \
            --zone ${regOrZoneNameStr} \
            --machine-type ${nodeType} \
            --num-nodes ${nodeCnt} \
            --addons HorizontalPodAutoscaling,HttpLoadBalancing,GcePersistentDiskCsiDriver \
            --enable-autoupgrade \
            --enable-autorepair \
            --enable-shielded-nodes \
            --max-surge-upgrade 1 \
            --max-unavailable-upgrade 0
    fi

    if [[ $? -ne 0 ]]; then
        echo "   [ERROR] Cluster creation failed!"
        errExit 250
    else
        echo "   Cluster creation succeeded!"
        echo
    fi
else
    echo "   [WARN] The GKE cluster with the spcified name already exists!"
fi

echo
echo "--------------------------------------------------------------"
echo ">> Set current K8s context to GKE cluster \"${clstrName}\" ..."
if [[ "${regOrZoneTypeStr}" == "region" ]]; then
    gcloud container clusters get-credentials ${clstrName} \
        --project ${projectName} \
        --region ${regOrZoneNameStr}
else
    gcloud container clusters get-credentials ${clstrName} \
        --project ${projectName} \
        --zone ${regOrZoneNameStr}
fi