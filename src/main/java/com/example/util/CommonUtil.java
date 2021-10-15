package com.example.util;

import com.fasterxml.uuid.Generators;
import org.apache.commons.lang3.RandomStringUtils;

import java.util.Arrays;
import java.util.UUID;
import java.util.stream.Collectors;

public class CommonUtil {

    //////////
    // Common command line options
    //
    // -h: usage help info
    public static final String CMD_OPTION_HELP = "h";
    // -f: pulsar connection property file
    public static final String CMD_OPTION_CFG_FILE = "f";
    // -op: operation type (producer or consumer)
    public static final String CMD_OPTION_OPTYPE = "op";
    // -n: number of messages
    public static final String CMD_OPTION_MSGNUM = "n";
    // -sn: subscription name (consumer only)
    public static final String CMD_OPTION_SUBSCRIPTION_NAME = "sn";

    public enum OP_TYPE {
        // For JMS, this also means "sender" (Queue), "publisher" (Topic)
        Producer("Producer"),
        // For JMS, this also means "receiver" (Queue), (non-durable, non-shared) "subscriber"/"consumer" (Topic)
        Consumer("Consumer"),
        // JMS specific for topic
        SharedConsumer("SharedConsumer"),
        DurableConsumer("DurableConsumer"),
        SharedDurableConsumer("SharedDurableConsumer");

        public final String label;

        OP_TYPE(String label) {
            this.label = label;
        }
    }
    public static boolean isValidOpType(String item) {
        return Arrays.stream(OP_TYPE.values()).anyMatch((t) -> t.name().equalsIgnoreCase(item));
    }
    public static String getValidOpTypeList() {
        return Arrays.stream(OP_TYPE.values()).map(Object::toString).collect(Collectors.joining(", "));
    }

    //////////
    // Default connection property file name
    //
    public static final String DFT_PULSAR_CONN_CFG_FILE_NAME = "conn.properties";

    // https://pulsar.apache.org/docs/en/reference-configuration/#client
    public static final String CONF_KEY_BROKER_SVC_URL = "brokerServiceUrl";
    public static final String CONF_KEY_WEB_SVC_URL = "webServiceUrl";
    public static final String CONF_KEY_AUTH_PLUGIN = "authPlugin";
    public static final String CONF_KEY_AUTH_PARAMS = "authParams";
    public static final String CONF_KEY_USETLS = "useTls";
    public static final String CONF_KEY_TLS_ALLOW_INSECURE_CONNECTION = "tlsAllowInsecureConnection";
    public static final String CONF_KEY_TLS_ENABLE_HOSTNAME_VERIFICATION = "tlsEnableHostnameVerification";
    public static final String CONF_KEY_TLS_TRUST_CERTS_FILE_PATH = "tlsTrustCertsFilePath";
    public static final String CONF_KEY_TLS_USE_KEYSTORE = "useKeyStoreTls";
    public static final String CONF_KEY_TLS_TRUSTSTORE_TYPE = "tlsTrustStoreType";
    public static final String CONF_KEY_TLS_TRUSTSTORE_PASSWORD = "tlsTrustStorePassword";

    // https://docs.datastax.com/en/fast-pulsar-jms/docs/1.1/pulsar-jms-reference.html#_configuration_options
    public static final String CONF_KEY_JMS_ENABLE_TRANSACT = "enableTransaction";
    public static final String CONF_KEY_JMS_PRODUCER_CFG = "producerConfig";
    public static final String CONF_KEY_JMS_CONSUMER_CFG = "consumerConfig";

    //////////
    // Misc. utility functions
    //
    public static String randomClientIdentifier() {
        UUID uuid = Generators.timeBasedGenerator().generate();
        return uuid.toString();
    }

    public static final int MIN_RNDM_STRING_LENGTH = 15;
    public static final int MAX_RNDM_STRING_LENGTH = 30;

    public static String randomString(int strLen) {
        return RandomStringUtils.random(strLen, true, true);
    }

    public static void pause(int pauseinMillis) {
        try {
            Thread.sleep(pauseinMillis);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
