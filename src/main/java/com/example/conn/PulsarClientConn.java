package com.example.conn;

import com.example.util.CommonUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.ClientBuilder;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

import java.util.HashMap;
import java.util.Map;

public class PulsarClientConn {

    private String brokerSvcUrl;
    private Map<String, Object> clientConfMap;
    private Map<String, Object> clientMiscConfMap;
    private PulsarClient pulsarClient = null;

    public PulsarClientConn(String brokerSvcUrl,
                            Map<String, Object> clientConfMap,
                            Map<String, Object> clientMiscConfMap) {
        this.brokerSvcUrl = brokerSvcUrl;
        this.clientConfMap = clientConfMap;
        this.clientMiscConfMap = clientMiscConfMap;
        createPulsarClient();
    }

    public void createPulsarClient() {
        try {
            ClientBuilder clientBuilder = PulsarClient.builder();
            clientBuilder.loadConf(clientMiscConfMap);
            clientBuilder.serviceUrl(brokerSvcUrl);

            String authPluginClassName = null;
            if (clientConfMap.containsKey(CommonUtil.CONF_KEY_AUTH_PLUGIN)) {
                authPluginClassName = clientConfMap.get(CommonUtil.CONF_KEY_AUTH_PLUGIN).toString();
            }

            String authParams = null;
            if (clientConfMap.containsKey(CommonUtil.CONF_KEY_AUTH_PARAMS)) {
                authParams = clientConfMap.get(CommonUtil.CONF_KEY_AUTH_PARAMS).toString();
            }

            if ( !StringUtils.isEmpty(authPluginClassName) && !StringUtils.isEmpty(authParams) ) {
                clientBuilder.authentication(authPluginClassName, authParams);
            }

            // TODO: add TLS handling

            pulsarClient = clientBuilder.build();

        } catch (PulsarClientException e) {
            e.printStackTrace();
        }
    }

    public PulsarClient getPulsarClient() {
        return pulsarClient;
    }
}
