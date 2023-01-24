package com.example.util;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PulsarDemoUtil {

    /////////
    // Command line options specific to Pulsar client
    //
    // -t: pulsar topic name
    public static final String CMD_OPTION_TOPIC_NAME = "t";

    // -st: subscription type (consumer only)
    public static final String CMD_OPTION_SUBSCRIPTION_TYPE = "st";

    /////////
    // Subscription types
    //
    public enum SUBSCRIPTION_TYPE {
        Exclusive("Exclusive"),
        Failover("Failover"),
        Shared("Shared"),
        Key_Shared("Key_Shared");

        public final String label;

        SUBSCRIPTION_TYPE(String label) {
            this.label = label;
        }
    }

    public static boolean isValidSubscriptionType(String item) {
        return Arrays.stream(SUBSCRIPTION_TYPE.values()).anyMatch((t) -> t.name().equals(item));
    }
    public static String getValidSubscriptionTypeList() {
        return Arrays.stream(SUBSCRIPTION_TYPE.values()).map(Object::toString).collect(Collectors.joining(", "));
    }
}
