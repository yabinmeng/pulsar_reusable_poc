#! /bin/bash

source ./utilities.sh

### 
# This script is used to get the latest starlight API releases for
# 1) Starlight for JMS client Jar file
# 2) LunaStreaming version update
# 

if [[ -z "${WORKSHOP_HOMEDIR// }" ]]; then
    echo "Home direcotry is not set! Please make sure it is set properly in \"_setenv.sh\" file."
    exit 10;
fi

STARLIGH_LIBDIR="${WORKSHOP_HOMEDIR}/misc/starlight_libs"


## 
# Download a file from the specified URL
downloadFileFromUrl() {
    if ! [[ -f $2 ]]; then
        curl -s $1 -o $2
    fi
}


## Starlight for JMS (S4J) library files 
# - client jar
# - server-side filtering nar file
s4jGhRelUrlBase="https://github.com/datastax/pulsar-jms/releases"

s4jVersion=$(chkGitHubLatestRelVer "${s4jGhRelUrlBase}/latest")
debugMsg "s4jVersion=${s4jVersion}"

s4jGhRelDownloadUrlBase="${s4jGhRelUrlBase}/download/${s4jVersion}"
s4jClntJarFile="pulsar-jms-all-${s4jVersion}.jar"
s4jSrvFilterNarFile="pulsar-jms-${s4jVersion}.nar"

echo "  Download file \"$s4jClntJarFile\" if not exists ... "
$(downloadFileFromUrl "${s4jGhRelDownloadUrlBase}/${s4jClntJarFile}" "${STARLIGH_LIBDIR}/${s4jClntJarFile}")

echo "  Download file \"$s4jSrvFilterNarFile\" if not exists ... "
$(downloadFileFromUrl "${s4jGhRelDownloadUrlBase}/${s4jSrvFilterNarFile}" "${STARLIGH_LIBDIR}/${s4jSrvFilterNarFile}")



## Starlight for Kafka (S4K) library files
# - server side protocol handler nar file
s4kGhRelUrlBase="https://github.com/datastax/starlight-for-kafka/releases"

s4kVersion=$(chkGitHubLatestRelVer "${s4kGhRelUrlBase}/latest")
debugMsg "s4kVersion=${s4kVersion}"

s4kGhRelDownloadUrlBase="${s4kGhRelUrlBase}/download/v${s4kVersion}"
# S4K used as Pulsar proxy extension
s4kProxyExtensionNarFile="pulsar-kafka-proxy-${s4jVersion}.nar"
# S4K used as Pulsar broker protocol handler
s4kProtocolHandlerNarFile="pulsar-protocol-handler-kafka-${s4jVersion}.nar"

echo "  Download file \"$s4kProxyExtensionNarFile\" if not exists ... "
$(downloadFileFromUrl "${s4kGhRelDownloadUrlBase}/${s4kProxyExtensionNarFile}" "${STARLIGH_LIBDIR}/${s4kProxyExtensionNarFile}")

echo "  Download file \"$s4kProtocolHandlerNarFile\" if not exists ... "
$(downloadFileFromUrl "${s4kGhRelDownloadUrlBase}/${s4kProtocolHandlerNarFile}" "${STARLIGH_LIBDIR}/${s4kProtocolHandlerNarFile}")


## Starlight for Rabbit MQ (S4R) library files
# - server side protocol handler nar file
s4rGhRelUrlBase="https://github.com/datastax/starlight-for-rabbitmq/releases"

s4rVersion=$(chkGitHubLatestRelVer "${s4rGhRelUrlBase}/latest")
debugMsg "s4kVersion=${s4rVersion}"

s4rGhRelDownloadUrlBase="${s4rGhRelUrlBase}/download/v${s4rVersion}"
# For S4R, the same NAR file can be used as both for 
# - Pulsar broker protocol handler
# - Pulsar prxoy extension
s4rProtocolHandlerNarFile="starlight-rabbitmq-${s4rVersion}.nar"

echo "  Download file \"$s4rProtocolHandlerNarFile\" if not exists ... "
$(downloadFileFromUrl "${s4rGhRelDownloadUrlBase}/${s4rProtocolHandlerNarFile}" "${STARLIGH_LIBDIR}/{s4rProtocolHandlerNarFile}")