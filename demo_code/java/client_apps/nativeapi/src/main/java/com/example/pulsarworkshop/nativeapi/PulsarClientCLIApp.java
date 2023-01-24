package com.example.pulsarworkshop.nativeapi;

import com.example.pulsarworkshop.utilities.exception.CliOptProcException;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.pulsar.client.api.SubscriptionType;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.stream.Collectors;

public class PulsarCmdApp {

    final boolean producerApp;


    /**
     * For the purpose of command line options parsing and processing
     */
    DefaultParser parser = new DefaultParser();
    CommandLine cmd = null;

    static Options cliOptions = new Options();
    static {
        Option helpOption = new Option(
                "h", false, "Displays this help message.");
        Option cfgOption = new Option(
                "f", true, "Configuration properties file.");
        Option msgNumOption = new Option(
                "n", true, "Number of messages to process.");
        Option topicOption = new Option(
                "tp", true, "Pulsar full topic name (e.g. tenant/namespace/topic).");
        Option pulsarSvcOption = new Option(
                "svc", true, "Pulsar native service url (e.g. pulsar://localhost:6650).");
        // Only relevant when OpType is "consumer"
        Option subNameOption = new Option(
                "sn", true, "Subscription name");
        // Only relevant when OpType is "consumer"
        Option subTypeOption = new Option(
                "st", true, "Subscription type (default to \"Exclusive\")");

        cliOptions.addOption(helpOption);
        cliOptions.addOption(cfgOption);
        cliOptions.addOption(msgNumOption);
        cliOptions.addOption(topicOption);
        cliOptions.addOption(pulsarSvcOption);
        cliOptions.addOption(subNameOption);
        cliOptions.addOption(subTypeOption);
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
        return Arrays.stream(SUBSCRIPTION_TYPE.values()).anyMatch((t) -> t.name().equals(item));
    }
    public static String getValidSubscriptionTypeList() {
        return Arrays.stream(SUBSCRIPTION_TYPE.values()).map(Object::toString).collect(Collectors.joining(", "));
    }

    private File workShopCfgFile;
    private int numMessage;
    private String topicName;
    private String pulsarSvcUrl;

    // Only relevant with consumer
    private String subscriptionName;
    // Only relevant with consumer
    private SubscriptionType subscriptionType = SubscriptionType.Exclusive;

    PulsarCmdApp(boolean producer) {
        this.producerApp = producer;
    }

    public File getWorkShopCfgFile() { return workShopCfgFile; }
    public int getNumMessage() { return numMessage; }
    public String getTopicName() { return topicName; }
    public String getPulsarSvcUrl() { return pulsarSvcUrl; }
    public String getSubscriptionName() { return subscriptionName; }
    public SubscriptionType getSubscriptionType() { return subscriptionType; }

    public static void usageAndExit(int errorCode) {

        System.out.println();

        PrintWriter errWriter = new PrintWriter(System.out, true);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(errWriter, 150, "PulsarCmdApp",
                "PulsarCmdApp Options:",
                cliOptions, 2, 1, "", true);

        System.out.println();
    }

    protected void processInputParameters(String[] inputParams) {
        try {
            cmd = parser.parse(cliOptions, inputParams);
        } catch (ParseException e) {
            throw new CliOptProcException(10, "Failure parsing CLI input parameters!");
        }

        // CLI option for help messages (-h)
        if (cmd.hasOption("h")) {
            usageAndExit(0);
        }

        // CLI option for workshop configuration (-f)
        String cfgFileName = cmd.getOptionValue("f");
        if (StringUtils.isNotBlank(cfgFileName)) {
            try {
                workShopCfgFile = new File(cfgFileName);
                workShopCfgFile.getCanonicalPath();
            }
            catch (IOException | SecurityException  ex) {
                workShopCfgFile = null;
            }
        }
        else {
            // Use default file "workshop.properties" under the "resources" folder
            URL resource = getClass().getClassLoader().getResource("worshop.properties");
            if (resource != null) {
                try {
                    workShopCfgFile = new File(resource.toURI());
                }
                catch (URISyntaxException use) {
                    workShopCfgFile = null;
                }
            }
        }
        if (workShopCfgFile == null) {
            throw new CliOptProcException(20,
                    "The required workshop configuration file is either not found or not valid.");
        }

        // CLI option for message number (-n)
         String msgNumCmdStr = cmd.getOptionValue("n");
        int msgNum = NumberUtils.toInt(msgNumCmdStr);
        if ( msgNum <= 0 ) {
            throw new CliOptProcException(30,
                    "Incorrect value for \"-n\" option (number of messages); it must be a positive integer!");
        }

        // CLI option for topic name (-t)
        topicName = cmd.getOptionValue("tp");
        if ( StringUtils.isBlank(topicName) ) {
            throw new CliOptProcException(40,
                    "Incorrect value for \"-tp\" option (pulsar topic name); " +
                            "it must be a string in format \"<tenant>/<namespace>/<topic>\"!");
        }

        // CLI option for Pulsar service url (-svc)
        pulsarSvcUrl = cmd.getOptionValue("svc");
        if ( StringUtils.isBlank(pulsarSvcUrl) ) {
            throw new CliOptProcException(40,
                    "Incorrect value for \"-svc\" option (pulsar service url); " +
                            "it must be a string in format \"pulsar[+ssl]://<pulsar_svr_hostname>:[6650|6651]\"!");
        }

        // CLI option for subscription name (-sn) - consumer only
        String subName = cmd.getOptionValue("sn");
        if (StringUtils.isBlank(subName)) {
            throw new CliOptProcException(50,
                    "Incorrect value for \"-sn\" option (subscription name); it can't be empty for a consumer.");
        }

        // CLI option for subscription type (-st) - consumer only
        String subType = cmd.getOptionValue("st");
        if (!StringUtils.isBlank(subType)) {
            if (!StringUtils.equalsAnyIgnoreCase(subType,
                    SUBSCRIPTION_TYPE.Exclusive.label,
                    SUBSCRIPTION_TYPE.Failover.label,
                    SUBSCRIPTION_TYPE.Shared.label,
                    SUBSCRIPTION_TYPE.Key_Shared.label)) {
                throw new CliOptProcException(60,
                        "Incorrect value for \"-st\" option (subscription type); " +
                                "it must be one of the valid values: \"" + getValidSubscriptionTypeList() + "\"");
            }
        }
    }
}
