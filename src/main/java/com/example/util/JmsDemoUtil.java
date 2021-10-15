package com.example.util;

import org.apache.commons.lang3.BooleanUtils;

import java.util.*;
import java.util.stream.Collectors;

public class JmsDemoUtil {

    /////////
    // Command line options specific to JMS client
    //
    // -dt: destination type (see "JMS_DEST_TYPE")
    public static final String CMD_OPTION_DEST_TYPE = "dt";
    // -dn: destination name
    public static final String CMD_OPTION_DEST_NAME = "dn";
    // -pn: destination name
    public static final String CMD_OPTION_PATTERN_NAME = "pn";
    // -ms: message selector
    public static final String CMD_OPTION_MSG_SELECTOR = "ms";


    /////////
    //
    public static final String DFT_MSG_SELECTOR_STR = "sequence_id >= 3 and sequence_id < 6";


    /////////
    // Valid JMS destination types
    public enum JMS_DEST_TYPE {
        queue("queue"),
        topic("topic");

        public final String label;

        JMS_DEST_TYPE(String label) {
            this.label = label;
        }
    }
    public static boolean isValidDestinationType(String item) {
        return Arrays.stream(JMS_DEST_TYPE.values()).anyMatch((t) -> t.name().equalsIgnoreCase(item));
    }
    public static String getValidDestinationTypeList() {
        return Arrays.stream(JMS_DEST_TYPE.values()).map(Object::toString).collect(Collectors.joining(", "));
    }

    // Valid JMS pattern name
    public enum JMS_QUEUE_PATTERN {
        QueueBrowser("QueueBrowser"),
        QueueReceiver("QueueReceiver"),
        QueueRequestor("QueueRequestor"),
        QueueSender("QueueSender");

        public final String label;

        JMS_QUEUE_PATTERN(String label) {
            this.label = label;
        }
    }
    public static boolean isValidQueuePattern(String item) {
        return Arrays.stream(JMS_QUEUE_PATTERN.values()).anyMatch((t) -> t.name().equalsIgnoreCase(item));
    }
    public static String getValidQueuePatternList() {
        return Arrays.stream(JMS_QUEUE_PATTERN.values()).map(Object::toString).collect(Collectors.joining(", "));
    }

    // Valid JMS message properties
    public enum JMS_MSG_PROPERTY {
        SEQUENCE_ID("sequence_id"),
        JMS_TIME("jms_time");

        public final String label;

        JMS_MSG_PROPERTY(String label) {
            this.label = label;
        }
    }
    public static boolean isValidMsgProperty(String item) {
        return Arrays.stream(JMS_MSG_PROPERTY.values()).anyMatch((t) -> t.name().equalsIgnoreCase(item));
    }
    public static String getValidMsgPropertyList() {
        return Arrays.stream(JMS_MSG_PROPERTY.values()).map(Object::toString).collect(Collectors.joining(", "));
    }


    /////////
    // Get JMS connection properties in the right format
    public static Map<String, Object> getPulsarJmsConfMap(ConfLoaderUtil confLoaderUtil) {
        Map<String, Object> clientConfMap = confLoaderUtil.getClientConfMap();
        Map<String, Object> clientMiscConfMap = confLoaderUtil.getClientMiscConfMap();
        Map<String, Object> producerConfMap = confLoaderUtil.getProducerConfMap();
        Map<String, Object> consumerConfMap = confLoaderUtil.getConsumerConfMap();
        Map<String, Object> jmsConfMap = confLoaderUtil.getJmsConfMap();

        Map<String, Object>  pulsarJmsConnProperties = new HashMap<>();

        String pulsarSvcUrl;
        if (clientConfMap.containsKey(CommonUtil.CONF_KEY_BROKER_SVC_URL)) {
            pulsarSvcUrl = clientConfMap.get(CommonUtil.CONF_KEY_BROKER_SVC_URL).toString();
            pulsarJmsConnProperties.put(CommonUtil.CONF_KEY_BROKER_SVC_URL, pulsarSvcUrl);
        }

        pulsarJmsConnProperties.putAll(clientConfMap);
        pulsarJmsConnProperties.putAll(clientMiscConfMap);

        pulsarJmsConnProperties.put(CommonUtil.CONF_KEY_JMS_PRODUCER_CFG, producerConfMap);
        pulsarJmsConnProperties.put(CommonUtil.CONF_KEY_JMS_CONSUMER_CFG, consumerConfMap);

        for (String confKey : jmsConfMap.keySet()) {
            Object confVal = jmsConfMap.get(confKey);
            pulsarJmsConnProperties.put(("jms." + confKey), confVal);
        }

        return pulsarJmsConnProperties;
    }
}
