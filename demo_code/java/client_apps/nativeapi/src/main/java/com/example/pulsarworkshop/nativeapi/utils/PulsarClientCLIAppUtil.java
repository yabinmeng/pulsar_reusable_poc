package com.example.pulsarworkshop.nativeapi;

import org.apache.commons.cli.*;
import org.apache.commons.compress.archivers.sevenz.CLI;
import org.apache.pulsar.shade.org.checkerframework.checker.units.qual.C;

import java.util.Arrays;
import java.util.stream.Collectors;

public class PulsarClientCLIAppUtil {

    enum CLI_OPTION {
        Help("h"),
        CfgFile("f"),
        MsgNum("n"),
        SvcUrl("svc"),
        ClntName("name"),
        TpName("tp"),
        TpNamePatn("tpn"),
        SubName("csn"),
        SubType("cst");

        public final String label;

        CLI_OPTION(String label) {
            this.label = label;
        }
    }

    static boolean isValidCliOption(String item) {
        return Arrays.stream(CLI_OPTION.values()).anyMatch((t) -> t.name().equals(item));
    }
    public static String getValidCliOptionList() {
        return Arrays.stream(CLI_OPTION.values()).map(Object::toString).collect(Collectors.joining(", "));
    }

    static Options cliOptions = new Options();
    static {
        // Common to both producer and consumer
        Option helpOption = new Option(
                CLI_OPTION.Help.label, false, "Displays this help message.");
        Option cfgOption = new Option(
                CLI_OPTION.CfgFile.label, true, "Configuration properties file.");
        Option msgNumOption = new Option(
                CLI_OPTION.MsgNum.label, true, "Number of messages to process.");
        Option pulsarSvcOption = new Option(
                CLI_OPTION.SvcUrl.label, true,
                "Pulsar native service url (e.g. pulsar://localhost:6650).");
        Option pulsarClntNameOption = new Option(
                CLI_OPTION.ClntName.label, true, "(Optional) Pulsar producer or consumer name.");
        Option topicNameOption = new Option(
                CLI_OPTION.TpName.label, true, "Pulsar topic name(s) (e.g. tenant/namespace/topic). " +
                "For producers, this must be a single topic name; for consumers, this can be a list of topic names" +
                "that are separated by comma");

        // Consumer only
        Option cTopicPtnOption = new Option(
                CLI_OPTION.TpNamePatn.label, true, "(Consumer only) Topic name pattern string");
        // Consumer only
        Option cSubNameOption = new Option(
                CLI_OPTION.SubName.label, true, "(Consumer only) Subscription name");
        // Only relevant when OpType is "consumer"
        Option cSubTypeOption = new Option(
                CLI_OPTION.SubType.label, true,
                "(Consumer only) Subscription type (default to \"Exclusive\")");

        cliOptions.addOption(helpOption);
        cliOptions.addOption(cfgOption);
        cliOptions.addOption(msgNumOption);
        cliOptions.addOption(pulsarSvcOption);
        cliOptions.addOption(pulsarClntNameOption);
        cliOptions.addOption(topicNameOption);

        cliOptions.addOption(cTopicPtnOption);
        cliOptions.addOption(cSubNameOption);
        cliOptions.addOption(cSubTypeOption);
    }

    enum SUBSCRIPTION_TYPE {
        Exclusive("Exclusive"),
        Failover("Failover"),
        Shared("Shared"),
        Key_Shared("Key_Shared");

        public final String label;

        SUBSCRIPTION_TYPE(String label) {
            this.label = label;
        }
    }

    static boolean isValidSubscriptionType(String item) {
        return Arrays.stream(SUBSCRIPTION_TYPE.values()).anyMatch((t) -> t.name().equalsIgnoreCase(item));
    }
    public static String getValidSubscriptionTypeList() {
        return Arrays.stream(SUBSCRIPTION_TYPE.values()).map(Object::toString).collect(Collectors.joining(", "));
    }
}
