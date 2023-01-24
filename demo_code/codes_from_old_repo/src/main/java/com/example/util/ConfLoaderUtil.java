package com.example.util;

import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ConfLoaderUtil {
    private String canonicalFilePath = "";

    public static final String CLIENT_CONF_PREFIX = "client";
    public static final String PRODUCER_CONF_PREFIX = "producer";
    public static final String CONSUMER_CONF_PREFIX = "consumer";
    public static final String JMS_CONF_PREFIX = "jms";

    private boolean loadSuccess;

    // configuration item without prefix
    private final HashMap<String, Object> clientConfMap = new HashMap<>();
    // configuration items with prefix "client" (Pulsar client connection)
    // - https://pulsar.apache.org/docs/en/client-libraries-java/#client
    private final HashMap<String, Object> clientMiscConfMap = new HashMap<>();
    // configuration items with prefix "producer" (Pulsar producer)
    // - https://pulsar.apache.org/docs/en/client-libraries-java/#producer
    private final HashMap<String, Object> producerConfMap = new HashMap<>();
    // configuration items with prefix "consumer" (Pulsar consumer)
    // - https://pulsar.apache.org/docs/en/client-libraries-java/#consumer
    private final HashMap<String, Object> consumerConfMap = new HashMap<>();
    // configuration items with prefix "jms" (Pulsar JMS)
    // - https://docs.datastax.com/en/fast-pulsar-jms/docs/1.1/pulsar-jms-reference.html#_configuration_options
    private final HashMap<String, Object> jmsConfMap = new HashMap<>();

    public ConfLoaderUtil(String fileName) {
        File file = new File(fileName);

        try {
            canonicalFilePath = file.getCanonicalPath();

            Parameters params = new Parameters();

            FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                    new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                            .configure(params.properties()
                                    .setFileName(fileName));

            Configuration config = builder.getConfiguration();

            for (Iterator<String> it = config.getKeys(); it.hasNext();) {
                String confKey = it.next();
                String confVal = config.getProperty(confKey).toString();

                // Ignore configuration items with empty value
                if (!StringUtils.isBlank(confVal)) {
                    if (confKey.startsWith(CLIENT_CONF_PREFIX)) {
                        // Ignore the following Pulsar client parameters
                        // (https://pulsar.apache.org/docs/en/client-libraries-java/#client)
                        //   - serviceUrl
                        //   - authPluginClassName
                        //   - authParams
                        //   - useTls
                        //   - tlsTrustCertsFilePath
                        //   - tlsAllowInsecureConnection
                        //   - tlsHostnameVerificationEnable
                        // These parameters are already covered by MISC items
                        // (https://pulsar.apache.org/docs/en/reference-configuration/#client)
                        if ( ! StringUtils.equalsAnyIgnoreCase(confKey,
                                "serviceUrl",
                                "authPluginClassName",
                                "authParams",
                                "useTls",
                                "tlsTrustCertsFilePath",
                                "tlsAllowInsecureConnection",
                                "tlsHostnameVerificationEnable") ) {
                            clientMiscConfMap.put(confKey.substring(CLIENT_CONF_PREFIX.length() + 1), confVal);
                        }
                    }
                    else if (confKey.startsWith(PRODUCER_CONF_PREFIX)) {
                        // Ignore the following Pulsar client parameter
                        //   - topic
                        // This parameter needs to be provided
                        if ( ! StringUtils.equalsAnyIgnoreCase(confKey, "topic") ) {
                            producerConfMap.put(confKey.substring(PRODUCER_CONF_PREFIX.length() + 1), confVal);
                        }
                    }
                    else if (confKey.startsWith(CONSUMER_CONF_PREFIX)) {
                        // Ignore the following Pulsar client parameter
                        //   - topic
                        // This parameter needs to be provided
                        consumerConfMap.put(confKey.substring(CONSUMER_CONF_PREFIX.length() + 1), confVal);
                    }
                    else if (confKey.startsWith(JMS_CONF_PREFIX)) {
                        jmsConfMap.put(confKey.substring(JMS_CONF_PREFIX.length() + 1), confVal);
                    }
                    else {
                        clientConfMap.put(confKey, confVal);
                    }
                }
            }

            loadSuccess = true;

        } catch (IOException ioe) {
            System.out.println("[ERROR] Can't read the specified config properties file!");
            ioe.printStackTrace();
        } catch (ConfigurationException cex) {
            System.out.println("[ERROR] Failed to load configuration items from the specified config properties file: " + canonicalFilePath);
            cex.printStackTrace();
        }
    }

    public boolean isConfigLoadSuccess() {
        return this.loadSuccess;
    }

    public Map<String, Object> getClientConfMap() {
        return this.clientConfMap;
    }

    public Map<String, Object> getClientMiscConfMap() {
        return this.clientMiscConfMap;
    }

    public Map<String, Object> getProducerConfMap() {
        return this.producerConfMap;
    }

    public Map<String, Object> getConsumerConfMap() {
        return this.consumerConfMap;
    }

    public Map<String, Object> getJmsConfMap() {
        return this.jmsConfMap;
    }
}
