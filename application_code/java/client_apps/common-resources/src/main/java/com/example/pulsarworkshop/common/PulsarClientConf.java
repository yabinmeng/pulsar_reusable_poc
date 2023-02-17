package com.example.pulsarworkshop.common;

import com.example.pulsarworkshop.common.exception.InvalidCfgParamException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.configuration2.Configuration;
import org.apache.commons.configuration2.FileBasedConfiguration;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.builder.FileBasedConfigurationBuilder;
import org.apache.commons.configuration2.builder.fluent.Parameters;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.impl.MultiplierRedeliveryBackoff;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class PulsarClientConf {

    private final static Logger logger = LoggerFactory.getLogger(PulsarClientConf.class);

    ///////
    // Valid configuration categories
    enum CONF_CATEGORY {
        Schema("schema"),
        Client("client"),
        Producer("producer"),
        Consumer("consumer"),
        Reader("reader");

        public final String label;

        CONF_CATEGORY(String label) {
            this.label = label;
        }
    }
    public static boolean isValidConnConfCategory(String item) {
        return Arrays.stream(CONF_CATEGORY.values()).anyMatch(t -> t.label.equals(item));
    }
    public static String getValidConnConfCategoryList() {
        return Arrays.stream(CONF_CATEGORY.values()).map(t -> t.label).collect(Collectors.joining(", "));
    }

    private final Map<String, String> schemaConfMapRaw = new HashMap<>();

    private final Map<String, String> clientConfMapRaw = new HashMap<>();
    private final Map<String, Object> clientConfMapTgt = new HashMap<>();

    private final Map<String, String> producerConfMapRaw = new HashMap<>();
    private final Map<String, Object> producerConfMapTgt = new HashMap<>();

    private final Map<String, String> consumerConfMapRaw = new HashMap<>();
    private final Map<String, Object> consumerConfMapTgt = new HashMap<>();

    private final Map<String, String> readerConfMapRaw = new HashMap<>();
    private final Map<String, Object> readerConfMapTgt = new HashMap<>();

    public PulsarClientConf(File cfgFile) {

        //////////////////
        // Read related Pulsar client configuration settings from a file
        readRawConfFromFile(cfgFile);

        //////////////////
        //  Convert the raw configuration map (<String,String>) to the required map (<String,Object>)
        clientConfMapTgt.putAll(ConfConverter.convertStdRawClientConf(clientConfMapRaw));
        producerConfMapTgt.putAll(ConfConverter.convertStdRawProducerConf(producerConfMapRaw));
        consumerConfMapTgt.putAll(ConfConverter.convertStdRawConsumerConf(consumerConfMapRaw));
        // TODO: Reader API is not enabled at the moment. Revisit when needed

        //////////////////
        // Ignores the following Pulsar client/producer/consumer configurations since
        // they will be explicitly handled via CLI input parameters
        clientConfMapTgt.remove("serviceUrl");
        clientConfMapTgt.remove("authPluginClassName");
        clientConfMapTgt.remove("authParams");
        clientConfMapTgt.remove("enableTls");
        clientConfMapTgt.remove("tlsTrustCertsFilePath");
        clientConfMapTgt.remove("tlsHostnameVerificationEnable");
        clientConfMapTgt.remove("tlsAllowInsecureConnection");

        producerConfMapTgt.remove("topicName");
        producerConfMapTgt.remove("producerName");

        consumerConfMapTgt.remove("topicNames");
        consumerConfMapTgt.remove("topicsPattern");
        consumerConfMapTgt.remove("subscriptionName");
        consumerConfMapTgt.remove("subscriptionType");
        consumerConfMapTgt.remove("consumerName");
    }


    public void readRawConfFromFile(File cfgFile) {
        String canonicalFilePath = "";

        try {
            canonicalFilePath = cfgFile.getCanonicalPath();

            Parameters params = new Parameters();

            FileBasedConfigurationBuilder<FileBasedConfiguration> builder =
                    new FileBasedConfigurationBuilder<FileBasedConfiguration>(PropertiesConfiguration.class)
                            .configure(params.properties()
                                    .setFileName(canonicalFilePath));

            Configuration config = builder.getConfiguration();

            for (Iterator<String> it = config.getKeys(); it.hasNext(); ) {
                String confKey = it.next();
                String confVal = config.getProperty(confKey).toString();

                if (!StringUtils.isBlank(confVal)) {

                    // Get schema specific configuration settings, removing "schema." prefix
                    if (StringUtils.startsWith(confKey, CONF_CATEGORY.Schema.label)) {
                        schemaConfMapRaw.put(confKey.substring(CONF_CATEGORY.Schema.label.length() + 1), confVal);
                    }
                    // Get client connection specific configuration settings, removing "client." prefix
                    // <<< https://pulsar.apache.org/docs/reference-configuration/#client >>>
                    else if (StringUtils.startsWith(confKey, CONF_CATEGORY.Client.label)) {
                        clientConfMapRaw.put(confKey.substring(CONF_CATEGORY.Client.label.length() + 1), confVal);
                    }
                    // Get producer specific configuration settings, removing "producer." prefix
                    // <<< https://pulsar.apache.org/docs/client-libraries-java/#configure-producer >>>
                    else if (StringUtils.startsWith(confKey, CONF_CATEGORY.Producer.label)) {
                        producerConfMapRaw.put(confKey.substring(CONF_CATEGORY.Producer.label.length() + 1), confVal);
                    }
                    // Get consumer specific configuration settings, removing "consumer." prefix
                    // <<< https://pulsar.apache.org/docs/client-libraries-java/#configure-consumer >>>
                    else if (StringUtils.startsWith(confKey, CONF_CATEGORY.Consumer.label)) {
                        consumerConfMapRaw.put(confKey.substring(CONF_CATEGORY.Consumer.label.length() + 1), confVal);
                    }
                    // Get reader specific configuration settings, removing "reader." prefix
                    // <<< https://pulsar.apache.org/docs/2.10.x/client-libraries-java/#configure-reader >>>
                    else if (StringUtils.startsWith(confKey, CONF_CATEGORY.Reader.label)) {
                        readerConfMapRaw.put(confKey.substring(CONF_CATEGORY.Reader.label.length() + 1), confVal);
                    }
                }
            }
        } catch (IOException ioe) {
            logger.error("Can't read the specified config properties file!");
            ioe.printStackTrace();
        } catch (ConfigurationException cex) {
            logger.error("Error loading configuration items from the specified config properties file: " + canonicalFilePath);
            cex.printStackTrace();
        }
    }


    public Map<String, String> getSchemaConfMapRaw() { return  this.schemaConfMapRaw; }
    public Map<String, String> getClientConfMapRaw() { return this.clientConfMapRaw; }
    public Map<String, Object> getClientConfMapTgt() { return this.clientConfMapTgt; }

    public Map<String, String> getProducerConfMapRaw() { return this.producerConfMapRaw; }
    public Map<String, Object> getProducerConfMapTgt() { return this.producerConfMapTgt; }
    public Map<String, String> getConsumerConfMapRaw() { return this.consumerConfMapRaw; }
    public Map<String, Object> getConsumerConfMapTgt() { return this.consumerConfMapTgt; }
    public Map<String, String> getReaderConfMapRaw() { return this.readerConfMapRaw; }
    public Map<String, Object> getReaderConfMapTgt() { return this.readerConfMapTgt; }


    public String toString() {
        return new ToStringBuilder(this).
                append("schemaConfMapRaw", schemaConfMapRaw.toString()).
                append("clientConfMapRaw", clientConfMapRaw.toString()).
                append("producerConfMapRaw", producerConfMapRaw.toString()).
                append("consumerConfMapRaw", consumerConfMapRaw.toString()).
                append("readerConfMapRaw", readerConfMapRaw.toString()).
                toString();
    }

    //////////////////
    // Get Schema related config
    public boolean hasSchemaConfKey(String key) {
        if (key.contains(CONF_CATEGORY.Schema.label))
            return schemaConfMapRaw.containsKey(key.substring(CONF_CATEGORY.Schema.label.length() + 1));
        else
            return schemaConfMapRaw.containsKey(key);
    }
    public String getSchemaConfValueRaw(String key) {
        if (hasSchemaConfKey(key)) {
            if (key.contains(CONF_CATEGORY.Schema.label))
                return schemaConfMapRaw.get(key.substring(CONF_CATEGORY.Schema.label.length() + 1));
            else
                return schemaConfMapRaw.get(key);
        }
        else {
            return "";
        }
    }

    //////////////////
    // Get Pulsar client related config
    public boolean hasClientConfKey(String key) {
        if (key.contains(CONF_CATEGORY.Client.label))
            return clientConfMapRaw.containsKey(key.substring(CONF_CATEGORY.Client.label.length() + 1));
        else
            return clientConfMapRaw.containsKey(key);
    }
    public String getClientConfValueRaw(String key) {
        if (hasClientConfKey(key)) {
            if (key.contains(CONF_CATEGORY.Client.label))
                return clientConfMapRaw.get(key.substring(CONF_CATEGORY.Client.label.length() + 1));
            else
                return clientConfMapRaw.get(key);
        }
        else {
            return "";
        }
    }


    //////////////////
    // Get Pulsar producer related config
    public boolean hasProducerConfKey(String key) {
        if (key.contains(CONF_CATEGORY.Producer.label))
            return producerConfMapRaw.containsKey(key.substring(CONF_CATEGORY.Producer.label.length() + 1));
        else
            return producerConfMapRaw.containsKey(key);
    }
    public String getProducerConfValueRaw(String key) {
        if (hasProducerConfKey(key)) {
            if (key.contains(CONF_CATEGORY.Producer.label))
                return producerConfMapRaw.get(key.substring(CONF_CATEGORY.Producer.label.length()+1));
            else
                return producerConfMapRaw.get(key);
        }
        else {
            return "";
        }
    }


    //////////////////
    // Get Pulsar consumer related config
    public boolean hasConsumerConfKey(String key) {
        if (key.contains(CONF_CATEGORY.Consumer.label))
            return consumerConfMapRaw.containsKey(key.substring(CONF_CATEGORY.Consumer.label.length() + 1));
        else
            return consumerConfMapRaw.containsKey(key);
    }
    public String getConsumerConfValueRaw(String key) {
        if (hasConsumerConfKey(key)) {
            if (key.contains(CONF_CATEGORY.Consumer.label))
                return consumerConfMapRaw.get(key.substring(CONF_CATEGORY.Consumer.label.length() + 1));
            else
                return consumerConfMapRaw.get(key);
        }
        else {
            return "";
        }
    }

    //////////////////
    // Get Pulsar reader related config
    public boolean hasReaderConfKey(String key) {
        if (key.contains(CONF_CATEGORY.Reader.label))
            return readerConfMapRaw.containsKey(key.substring(CONF_CATEGORY.Reader.label.length() + 1));
        else
            return readerConfMapRaw.containsKey(key);
    }
    public String getReaderConfValueRaw(String key) {
        if (hasReaderConfKey(key)) {
            if (key.contains(CONF_CATEGORY.Reader.label))
                return readerConfMapRaw.get(key.substring(CONF_CATEGORY.Reader.label.length() + 1));
            else
                return readerConfMapRaw.get(key);
        }
        else {
            return "";
        }
    }

    /**
     * Class used to convert the raw, string based configurations to the target, Object based configurations
     */
    class ConfConverter {

        enum COMPRESSION_TYPE {
            NONE("NONE"),
            LZ4("LZ4"),
            ZLIB("ZLIB"),
            ZSTD("ZSTD"),
            SNAPPY("SNAPPY");

            public final String label;

            COMPRESSION_TYPE(String label) {
                this.label = label;
            }
        }

        static boolean isValidCompressionType(String item) {
            return Arrays.stream(ConfConverter.COMPRESSION_TYPE.values()).anyMatch(t -> t.label.equals(item));
        }

        static String getValidCompressionTypeList() {
            return Arrays.stream(ConfConverter.COMPRESSION_TYPE.values()).map(t -> t.label).collect(Collectors.joining(", "));
        }

        enum SUBSCRIPTION_INITIAL_POSITION {
            Earliest("Earliest"),
            Latest("Latest");

            public final String label;

            SUBSCRIPTION_INITIAL_POSITION(String label) {
                this.label = label;
            }
        }

        static boolean isValidSubscriptionInitialPosition(String item) {
            return Arrays.stream(ConfConverter.SUBSCRIPTION_INITIAL_POSITION.values()).anyMatch(t -> t.label.equals(item));
        }

        static String getValidSubscriptionInitialPositionList() {
            return Arrays.stream(ConfConverter.SUBSCRIPTION_INITIAL_POSITION.values()).map(t -> t.label).collect(Collectors.joining(", "));
        }

        enum REGEX_SUBSCRIPTION_MODE {
            Persistent("PersistentOnly"),
            NonPersistent("NonPersistentOnly"),
            All("AllTopics");

            public final String label;

            REGEX_SUBSCRIPTION_MODE(String label) {
                this.label = label;
            }
        }

        static boolean isValidRegexSubscriptionMode(String item) {
            return Arrays.stream(ConfConverter.REGEX_SUBSCRIPTION_MODE.values()).anyMatch(t -> t.label.equals(item));
        }

        static String getValidRegexSubscriptionModeList() {
            return Arrays.stream(ConfConverter.REGEX_SUBSCRIPTION_MODE.values()).map(t -> t.label).collect(Collectors.joining(", "));
        }


        // <<< https://pulsar.apache.org/docs/client-libraries-java/#client >>>
        private final static Map<String, String> validStdClientConfKeyTypeMap = Map.ofEntries(
            Map.entry("serviceUrl", "String"),
            Map.entry("authPluginClassName", "String"),
            Map.entry("authParams", "String"),
            Map.entry("operationTimeoutMs", "long"),
            Map.entry("statsIntervalSeconds", "String"),
            Map.entry("numIoThreads", "int"),
            Map.entry("numListenerThreads", "int"),
            Map.entry("useTcpNoDelay", "boolean"),
            Map.entry("enableTls", "boolean"),
            Map.entry("tlsTrustCertsFilePath", "String"),
            Map.entry("tlsAllowInsecureConnection", "boolean"),
            Map.entry("tlsHostnameVerificationEnable", "boolean"),
            Map.entry("concurrentLookupRequest", "int"),
            Map.entry("maxLookupRequest", "int"),
            Map.entry("maxNumberOfRejectedRequestPerConnection", "int"),
            Map.entry("keepAliveIntervalSeconds", "int"),
            Map.entry("connectionTimeoutMs", "int"),
            Map.entry("requestTimeoutMs", "int"),
            Map.entry("defaultBackoffIntervalNanos", "int"),
            Map.entry("maxBackoffIntervalNanos", "long"),
            Map.entry("socks5ProxyAddress", "SocketAddress"),
            Map.entry("socks5ProxyUsername", "String"),
            Map.entry("socks5ProxyPassword", "String"),
            Map.entry("connectionMaxIdleSeconds", "int")
        );
        public static Map<String, Object> convertStdRawClientConf(Map<String, String> pulsarClientConfMapRaw) {
            Map<String, Object> clientConfObjMap = new HashMap<>();
            setConfObjMapForPrimitives(clientConfObjMap, pulsarClientConfMapRaw, validStdClientConfKeyTypeMap);

            /**
             * Non-primitive type processing for Pulsar producer configuration items
             */
            // TODO: Skip the following client configuration items for now because they're not really
            //       needed at the moment. Add support for them when needed.
            //       * socks5ProxyAddress

            return  clientConfObjMap;
        }


        // <<< https://pulsar.apache.org/docs/client-libraries-java/#configure-producer >>>
        private final static Map<String, String> validStdProducerConfKeyTypeMap = Map.ofEntries(
                Map.entry("topicName", "String"),
                Map.entry("producerName", "String"),
                Map.entry("sendTimeoutMs", "long"),
                Map.entry("blockIfQueueFull", "boolean"),
                Map.entry("maxPendingMessages", "int"),
                Map.entry("maxPendingMessagesAcrossPartitions", "int"),
                Map.entry("messageRoutingMode", "MessageRoutingMode"),
                Map.entry("hashingScheme", "HashingScheme"),
                Map.entry("cryptoFailureAction", "ProducerCryptoFailureAction"),
                Map.entry("batchingMaxPublishDelayMicros", "long"),
                Map.entry("batchingMaxMessages", "int"),
                Map.entry("batchingEnabled", "boolean"),
                Map.entry("chunkingEnabled", "boolean"),
                Map.entry("compressionType", "CompressionType"),
                Map.entry("initialSubscriptionName", "string")
        );
        public static Map<String, Object> convertStdRawProducerConf(Map<String, String> pulsarProducerConfMapRaw) {
            Map<String, Object> producerConfObjMap = new HashMap<>();
            setConfObjMapForPrimitives(producerConfObjMap, pulsarProducerConfMapRaw, validStdProducerConfKeyTypeMap);

            /**
             * Non-primitive type processing for Pulsar producer configuration items
             */
            // TODO: Skip the following producer configuration items for now because they're not really
            //       needed at the moment. Add support for them when needed.
            //       * messageRoutingMode
            //       * hashingScheme
            //       * cryptoFailureAction

            // "compressionType"
            // - expecting the following values: 'LZ4', 'ZLIB', 'ZSTD', 'SNAPPY'
            String confKeyName = "compressionType";
            String confVal = pulsarProducerConfMapRaw.get(confKeyName);
            String expectedVal = getValidCompressionTypeList();

            if (StringUtils.isNotBlank(confVal)) {
                if (StringUtils.containsIgnoreCase(expectedVal, confVal)) {
                    CompressionType compressionType = CompressionType.NONE;

                    switch (StringUtils.upperCase(confVal)) {
                        case "LZ4":
                            compressionType = CompressionType.LZ4;
                            break;
                        case "ZLIB":
                            compressionType = CompressionType.ZLIB;
                            break;
                        case "ZSTD":
                            compressionType = CompressionType.ZSTD;
                            break;
                        case "SNAPPY":
                            compressionType = CompressionType.SNAPPY;
                            break;
                    }

                    producerConfObjMap.put(confKeyName, compressionType);
                } else {
                    throw new InvalidCfgParamException(
                            getInvalidConfValStr(confKeyName, confVal,
                                    PulsarClientConf.CONF_CATEGORY.Producer.label, expectedVal));
                }
            }

            return producerConfObjMap;
        }


        // https://pulsar.apache.org/docs/client-libraries-java/#configure-consumer
        private final static Map<String, String> validStdConsumerConfKeyTypeMap = Map.ofEntries(
                Map.entry("topicNames", "Set<String>"),
                Map.entry("topicsPattern", "Pattern"),
                Map.entry("subscriptionName", "String"),
                Map.entry("subscriptionType", "SubscriptionType"),
                Map.entry("receiverQueueSize", "int"),
                Map.entry("acknowledgementsGroupTimeMicros", "long"),
                Map.entry("negativeAckRedeliveryDelayMicros", "long"),
                Map.entry("maxTotalReceiverQueueSizeAcrossPartitions", "int"),
                Map.entry("consumerName", "String"),
                Map.entry("ackTimeoutMillis", "long"),
                Map.entry("tickDurationMillis", "long"),
                Map.entry("priorityLevel", "int"),
                Map.entry("cryptoFailureAction", "ConsumerCryptoFailureAction"),
                Map.entry("properties", "SortedMap<String, String>"),
                Map.entry("readCompacted", "boolean"),
                Map.entry("subscriptionInitialPosition", "SubscriptionInitialPosition"),
                Map.entry("patternAutoDiscoveryPeriod", "int"),
                Map.entry("regexSubscriptionMode", "RegexSubscriptionMode"),
                Map.entry("deadLetterPolicy", "DeadLetterPolicy"),
                Map.entry("autoUpdatePartitions", "boolean"),
                Map.entry("replicateSubscriptionState", "boolean"),
                Map.entry("negativeAckRedeliveryBackoff", "RedeliveryBackoff"),
                Map.entry("ackTimeoutRedeliveryBackoff", "RedeliveryBackoff"),
                Map.entry("autoAckOldestChunkedMessageOnQueueFull", "boolean"),
                Map.entry("maxPendingChunkedMessage", "int"),
                Map.entry("expireTimeOfIncompleteChunkedMessageMillis", "long")
        );

        public static Map<String, Object> convertStdRawConsumerConf(Map<String, String> pulsarConsumerConfMapRaw) {
            Map<String, Object> consumerConfObjMap = new HashMap<>();
            setConfObjMapForPrimitives(consumerConfObjMap, pulsarConsumerConfMapRaw, validStdConsumerConfKeyTypeMap);

            /**
             * Non-primitive type processing for Pulsar consumer configuration items
             */
            // TODO: Skip the following consumer configuration items for now because they're not really
            //       needed right now. Add the support for them when needed.
            //       * cryptoFailureAction

            // "properties" has value type "SortedMap<String, String>"
            // - expecting the value string has the format: a JSON string that includes a set of key/value pairs
            String confKeyName = "properties";
            String confVal = pulsarConsumerConfMapRaw.get(confKeyName);
            String expectedVal = "{\"property1\":\"value1\", \"property2\":\"value2\"}, ...";

            ObjectMapper mapper = new ObjectMapper();

            if (StringUtils.isNotBlank(confVal)) {
                try {
                    Map<String, String> consumerProperties = mapper.readValue(confVal, Map.class);

                    // Empty map value is considered as no value
                    if (!consumerProperties.isEmpty()) {
                        consumerConfObjMap.put(confKeyName, consumerProperties);
                    }

                } catch (Exception e) {
                    throw new InvalidCfgParamException(
                            getInvalidConfValStr(confKeyName, confVal,
                                    PulsarClientConf.CONF_CATEGORY.Consumer.label, expectedVal));
                }
            }

            // "subscriptionInitialPosition"
            // - expecting the following values: 'Latest' (default),
            confKeyName = "subscriptionInitialPosition";
            confVal = pulsarConsumerConfMapRaw.get(confKeyName);
            expectedVal = getValidSubscriptionInitialPositionList();

            if (StringUtils.isNotBlank(confVal)) {
                try {
                    SubscriptionInitialPosition subInitPos = SubscriptionInitialPosition.Latest;
                    if (!StringUtils.isBlank(confVal)) {
                        subInitPos = SubscriptionInitialPosition.valueOf(confVal);
                    }
                    consumerConfObjMap.put(confKeyName, subInitPos);

                } catch (Exception e) {
                    throw new InvalidCfgParamException(
                            getInvalidConfValStr(confKeyName, confVal, "consumer", expectedVal));
                }
            }

            // "regexSubscriptionMode"
            // - expecting the following values: 'PersistentOnly' (default), 'NonPersistentOnly', and 'AllTopics'
            confKeyName = "regexSubscriptionMode";
            confVal = pulsarConsumerConfMapRaw.get(confKeyName);
            expectedVal = getValidRegexSubscriptionModeList();

            if (StringUtils.isNotBlank(confVal)) {
                try {
                    RegexSubscriptionMode regexSubscriptionMode = RegexSubscriptionMode.PersistentOnly;
                    if (!StringUtils.isBlank(confVal)) {
                        regexSubscriptionMode = RegexSubscriptionMode.valueOf(confVal);
                    }
                    consumerConfObjMap.put(confKeyName, regexSubscriptionMode);

                } catch (Exception e) {
                    throw new InvalidCfgParamException(
                            getInvalidConfValStr(confKeyName, confVal,
                                    PulsarClientConf.CONF_CATEGORY.Consumer.label, expectedVal));
                }
            }

            // "deadLetterPolicy"
            // - expecting the value is a JSON string has the format:
            //   {"maxRedeliverCount":"<int_value>","deadLetterTopic":"<topic_name>","initialSubscriptionName":"<sub_name>"}
            confKeyName = "deadLetterPolicy";
            confVal = pulsarConsumerConfMapRaw.get(confKeyName);
            expectedVal = "{" +
                    "\"maxRedeliverCount\":\"<int_value>\" (mandatory)," +
                    "\"retryLetterTopic\":\"<topic_name>\"," +
                    "\"deadLetterTopic\":\"<topic_name>\"," +
                    "\"initialSubscriptionName\":\"<sub_name>\"}";

            if (StringUtils.isNotBlank(confVal)) {
                try {
                    Map<String, String> dlqPolicyMap = mapper.readValue(confVal, Map.class);

                    // Empty map value is considered as no value
                    if (!dlqPolicyMap.isEmpty()) {
                        boolean valid = true;

                        // The JSON key must be one of "maxRedeliverCount", "deadLetterTopic", "initialSubscriptionName"
                        for (String key : dlqPolicyMap.keySet()) {
                            if (!StringUtils.equalsAnyIgnoreCase(key, "maxRedeliverCount",
                                    "retryLetterTopic", "deadLetterTopic", "initialSubscriptionName")) {
                                valid = false;
                                break;
                            }
                        }

                        // DLQ.maxRedeliverCount is mandatory
                        if (valid && !dlqPolicyMap.containsKey("maxRedeliverCount")) {
                            valid = false;
                        }

                        String maxRedeliverCountStr = dlqPolicyMap.get("maxRedeliverCount");
                        if (!NumberUtils.isCreatable(maxRedeliverCountStr)) {
                            valid = false;
                        }

                        if (valid) {
                            DeadLetterPolicy.DeadLetterPolicyBuilder builder = DeadLetterPolicy.builder()
                                    .maxRedeliverCount(NumberUtils.toInt(maxRedeliverCountStr));

                            String retryTopicName = dlqPolicyMap.get("retryLetterTopic");
                            String dlqTopicName = dlqPolicyMap.get("deadLetterTopic");
                            String initialSubName = dlqPolicyMap.get("initialSubscriptionName");

                            if (StringUtils.isNotBlank(retryTopicName))
                                builder.retryLetterTopic(retryTopicName);

                            if (StringUtils.isNotBlank(dlqTopicName))
                                builder.deadLetterTopic(dlqTopicName);

                            if (StringUtils.isNotBlank(initialSubName))
                                builder.initialSubscriptionName(initialSubName);

                            DeadLetterPolicy deadLetterPolicy = builder.build();
                            consumerConfObjMap.put(confKeyName, deadLetterPolicy);
                        } else {
                            throw new InvalidCfgParamException(
                                    getInvalidConfValStr(confKeyName, confVal,
                                            PulsarClientConf.CONF_CATEGORY.Consumer.label, expectedVal));
                        }
                    }
                } catch (Exception e) {
                    throw new InvalidCfgParamException(
                            getInvalidConfValStr(confKeyName, confVal,
                                    PulsarClientConf.CONF_CATEGORY.Consumer.label, expectedVal));
                }
            }

            // "negativeAckRedeliveryBackoff" or "ackTimeoutRedeliveryBackoff"
            // - expecting the value is a JSON string has the format:
            //   {"minDelayMs":"<int_value>", "maxDelayMs":"<int_value>", "multiplier":"<double_value>"}
            String[] redeliveryBackoffConfigSet = {
                    "negativeAckRedeliveryBackoff",
                    "ackTimeoutRedeliveryBackoff"
            };
            expectedVal = "{" +
                    "\"minDelayMs\":\"<int_value>\"," +
                    "\"maxDelayMs\":\"<int_value>\"," +
                    "\"multiplier\":\"<double_value>\"}";

            for (String confKey : redeliveryBackoffConfigSet) {
                confVal = pulsarConsumerConfMapRaw.get(confKey);

                if (StringUtils.isNotBlank(confVal)) {
                    try {
                        Map<String, String> redliveryBackoffMap = mapper.readValue(confVal, Map.class);

                        // Empty map value is considered as no value
                        if (!redliveryBackoffMap.isEmpty()) {
                            boolean valid = true;

                            // The JSON key must be one of "maxRedeliverCount", "deadLetterTopic", "initialSubscriptionName"
                            for (String key : redliveryBackoffMap.keySet()) {
                                if (!StringUtils.equalsAnyIgnoreCase(key,
                                        "minDelayMs", "maxDelayMs", "multiplier")) {
                                    valid = false;
                                    break;
                                }
                            }

                            String minDelayMsStr = redliveryBackoffMap.get("minDelayMs");
                            String maxDelayMsStr = redliveryBackoffMap.get("maxDelayMs");
                            String multiplierStr = redliveryBackoffMap.get("multiplier");

                            if ((StringUtils.isNotBlank(minDelayMsStr) && !NumberUtils.isCreatable(minDelayMsStr)) ||
                                    (StringUtils.isNotBlank(maxDelayMsStr) && !NumberUtils.isCreatable(maxDelayMsStr)) ||
                                    (StringUtils.isNotBlank(multiplierStr) && !NumberUtils.isCreatable(multiplierStr))) {
                                valid = false;
                            }

                            if (valid) {
                                RedeliveryBackoff redeliveryBackoff = MultiplierRedeliveryBackoff.builder()
                                        .minDelayMs(NumberUtils.toLong(minDelayMsStr))
                                        .maxDelayMs(NumberUtils.toLong(maxDelayMsStr))
                                        .multiplier(NumberUtils.toDouble(multiplierStr))
                                        .build();

                                consumerConfObjMap.put(confKey, redeliveryBackoff);

                            } else {
                                throw new InvalidCfgParamException(
                                        getInvalidConfValStr(confKey, confVal,
                                                PulsarClientConf.CONF_CATEGORY.Consumer.label, expectedVal));
                            }
                        }

                    } catch (Exception e) {
                        throw new InvalidCfgParamException(
                                getInvalidConfValStr(confKey, confVal,
                                        PulsarClientConf.CONF_CATEGORY.Consumer.label, expectedVal));
                    }
                }
            }

            return consumerConfObjMap;
        }


        // Utility function
        // - get configuration key names by the value type
        private static List<String> getStdConfKeyNameByValueType(Map<String, String> confKeyTypeMap, String tgtValType) {
            ArrayList<String> confKeyNames = new ArrayList<>();

            for (Map.Entry entry : confKeyTypeMap.entrySet()) {
                if (StringUtils.equalsIgnoreCase(entry.getValue().toString(), tgtValType)) {
                    confKeyNames.add(entry.getKey().toString());
                }
            }

            return confKeyNames;
        }

        // Conversion from Map<String, String> to Map<String, Object> for configuration items with primitive
        // value types
        private static void setConfObjMapForPrimitives(
                Map<String, Object> tgtConfObjMap,
                Map<String, String> srcConfMapRaw,
                Map<String, String> validConfKeyTypeMap) {
            List<String> confKeyList;

            // All configuration items with "String" as the value type
            confKeyList = getStdConfKeyNameByValueType(validConfKeyTypeMap, "String");
            for (String confKey : confKeyList) {
                if (srcConfMapRaw.containsKey(confKey)) {
                    String confVal = srcConfMapRaw.get(confKey);
                    if (StringUtils.isNotBlank(confVal)) {
                        tgtConfObjMap.put(confKey, confVal);
                    }
                }
            }

            // All configuration items with "long" as the value type
            confKeyList = getStdConfKeyNameByValueType(validConfKeyTypeMap, "long");
            for (String confKey : confKeyList) {
                if (srcConfMapRaw.containsKey(confKey)) {
                    String confVal = srcConfMapRaw.get(confKey);
                    if (StringUtils.isNotBlank(confVal)) {
                        tgtConfObjMap.put(confKey, Long.valueOf(confVal));
                    }
                }
            }

            // All configuration items with "int" as the value type
            confKeyList = getStdConfKeyNameByValueType(validConfKeyTypeMap, "int");
            for (String confKey : confKeyList) {
                if (srcConfMapRaw.containsKey(confKey)) {
                    String confVal = srcConfMapRaw.get(confKey);
                    if (StringUtils.isNotBlank(confVal)) {
                        tgtConfObjMap.put(confKey, Integer.valueOf(confVal));
                    }
                }
            }

            // All configuration items with "boolean" as the value type
            confKeyList = getStdConfKeyNameByValueType(validConfKeyTypeMap, "boolean");
            for (String confKey : confKeyList) {
                if (srcConfMapRaw.containsKey(confKey)) {
                    String confVal = srcConfMapRaw.get(confKey);
                    if (StringUtils.isNotBlank(confVal)) {
                        tgtConfObjMap.put(confKey, Boolean.valueOf(confVal));
                    }
                }
            }

            // TODO: So far the above primitive types should be good enough.
            //       Add support for other types when needed
        }

        private static String getInvalidConfValStr(String confKey, String confVal, String configCategory, String expectedVal) {
            return "Incorrect value \"" + confVal + "\" for Pulsar " + configCategory +
                    " configuration item of \"" + confKey + "\". Expecting the following value (format): " + expectedVal;
        }
    }
}
