package com.example.pulsarworkshop.common.utils;

import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.stream.Collectors;


public class PulsarClientCLIAppUtil {

    public enum CLI_OPTION {
        Help("h"),
        WorkloadFile("wf"),
        ConfigFile("cf"),
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

    public static boolean isValidCliOption(String item) {
        return Arrays.stream(CLI_OPTION.values()).anyMatch((t) -> t.name().equals(item));
    }
    public static String getValidCliOptionList() {
        return Arrays.stream(CLI_OPTION.values()).map(Object::toString).collect(Collectors.joining(", "));
    }

    public static Options cliOptions = new Options();
    static {
        // Common options ...
        Option helpOption = new Option(
                CLI_OPTION.Help.label, false, "Displays this help message.");
        Option cfgFileOption = new Option(
                CLI_OPTION.ConfigFile.label, true, "Configuration properties file.");
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

        // Producer only options ...
        Option wrkldFileOption = new Option(
                CLI_OPTION.WorkloadFile.label, true, "Raw workload csv file.");

        // Consumer only options ...
        Option cTopicPtnOption = new Option(
                CLI_OPTION.TpNamePatn.label, true, "(Consumer only) Topic name pattern string");
        Option cSubNameOption = new Option(
                CLI_OPTION.SubName.label, true, "(Consumer only) Subscription name");
        Option cSubTypeOption = new Option(
                CLI_OPTION.SubType.label, true,
                "(Consumer only) Subscription type (default to \"Exclusive\")");

        cliOptions.addOption(helpOption);
        cliOptions.addOption(cfgFileOption);
        cliOptions.addOption(msgNumOption);
        cliOptions.addOption(pulsarSvcOption);
        cliOptions.addOption(pulsarClntNameOption);
        cliOptions.addOption(topicNameOption);
        // producer only
        cliOptions.addOption(wrkldFileOption);
        // consumer only
        cliOptions.addOption(cTopicPtnOption);
        cliOptions.addOption(cSubNameOption);
        cliOptions.addOption(cSubTypeOption);
    }

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
        return Arrays.stream(SUBSCRIPTION_TYPE.values()).anyMatch((t) -> t.name().equalsIgnoreCase(item));
    }
    public static String getValidSubscriptionTypeList() {
        return Arrays.stream(SUBSCRIPTION_TYPE.values()).map(Object::toString).collect(Collectors.joining(", "));
    }

    public static File getResourceFile(String resourceFileName) {
        File resourceFile = null;

        String resourceFileName2 = resourceFileName;
        if ( !StringUtils.startsWith(resourceFileName2, "/") ) {
            resourceFileName2 = "/" + resourceFileName2;
        }

        try {
            System.out.println("===> " + resourceFileName2);
            URI uri = PulsarClientCLIAppUtil.class.getResource(resourceFileName2).toURI();
            System.out.println("===> " + uri);
            resourceFile = new File(uri.getPath());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        return resourceFile;
    }
}
