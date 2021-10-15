package com.example.pulsar;

import com.example.conn.PulsarClientConn;
import com.example.util.CommonUtil;
import com.example.util.ConfLoaderUtil;
import com.example.util.PulsarDemoUtil;
import org.apache.commons.cli.*;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class PulsarSimpleDemo {

    final static Log logger = LogFactory.getLog(PulsarSimpleDemo.class);

    /**
     *  Define Command Line Arguments
     */
    static Options options = new Options();

    static {
        Option helpOption = new Option(
                CommonUtil.CMD_OPTION_HELP, false, "Displays this help message.");
        Option cfgOption = new Option(
                CommonUtil.CMD_OPTION_CFG_FILE, true, "Configuration properties file.");
        Option opTypeOption = new Option(
                CommonUtil.CMD_OPTION_OPTYPE, true, "Operation type - " +
                "\"" + CommonUtil.OP_TYPE.Producer.label + "\" or " +
                "\"" + CommonUtil.OP_TYPE.Consumer.label + "\"" );
        Option msgNumOption = new Option(
                CommonUtil.CMD_OPTION_MSGNUM, true, "Number of messages");
        Option topicOption = new Option(
                PulsarDemoUtil.CMD_OPTION_TOPIC_NAME, true, "Pulsar topic name");
        // Only relevant when OpType is "consumer"
        Option subNameOption = new Option(
                CommonUtil.CMD_OPTION_SUBSCRIPTION_NAME, true, "Subscription name");
        // Only relevant when OpType is "consumer"
        Option subTypeOption = new Option(
                PulsarDemoUtil.CMD_OPTION_SUBSCRIPTION_TYPE, true, "Subscription type (default to \"Exclusive\")");

        options.addOption(helpOption);
        options.addOption(cfgOption);
        options.addOption(opTypeOption);
        options.addOption(msgNumOption);
        options.addOption(topicOption);
        options.addOption(subNameOption);
        options.addOption(subTypeOption);
    }

    static void usageAndExit(int errorCode) {

        System.out.println();

        PrintWriter errWriter = new PrintWriter(System.out, true);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(errWriter, 150, "PulsarSimpleDemo",
                "PulsarSimpleDemo Options:",
                options, 2, 1, "", true);

        System.out.println();
        System.out.println();

        System.exit(errorCode);
    }
    static void usageAndExit() {
        usageAndExit(0);
    }

    public static void main(String[] args) {

        /*
          Processing command line options
         */
        DefaultParser parser = new DefaultParser();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        }
        catch (ParseException e) {
            System.err.format("\n[ERROR] Failure parsing argument inputs: %s.\n", e.getMessage());
            usageAndExit(10);
        }

        // Print help message
        if ( (cmd != null) && cmd.hasOption(CommonUtil.CMD_OPTION_HELP) ) {
            usageAndExit();
        }

        // "-f" option (configuration file): default to "conn.properties" under the same folder!
        String cfgFile = cmd.getOptionValue(CommonUtil.CMD_OPTION_CFG_FILE);
        if ( StringUtils.isBlank(cfgFile) ) {
            cfgFile = CommonUtil.DFT_PULSAR_CONN_CFG_FILE_NAME;

            try {
                File file = new File(cfgFile);
                String canonicalFilePath = file.getCanonicalPath();
            }
            catch (IOException ioe) {
                System.out.println("Can't read the specified config properties file (" + cfgFile + ")!");
                // ioe.printStackTrace();
                usageAndExit(20);
            }
        }

        // "-op" option (operation type) - producer or consumer
        String opType = cmd.getOptionValue(CommonUtil.CMD_OPTION_OPTYPE);
        if ( StringUtils.isBlank(opType) ||
                !StringUtils.equalsAnyIgnoreCase(
                        opType,
                        CommonUtil.OP_TYPE.Producer.label,
                        CommonUtil.OP_TYPE.Consumer.label) ) {
            System.err.println("\n[ERROR] Incorrect value for \"-op\" option (operation type); possible valid values:  " +
                    "\"" + CommonUtil.OP_TYPE.Producer.label + "\" or " +
                    "\"" + CommonUtil.OP_TYPE.Consumer.label + "r\".\n");
            usageAndExit(30);
        }
        boolean isProducerOp = StringUtils.equalsIgnoreCase(opType, CommonUtil.OP_TYPE.Producer.label);
        boolean isConsumerOp = StringUtils.equalsIgnoreCase(opType, CommonUtil.OP_TYPE.Consumer.label);

        // "-n" option (message number)
        String msgNumCmdStr = cmd.getOptionValue(CommonUtil.CMD_OPTION_MSGNUM);
        int msgNum = NumberUtils.toInt(msgNumCmdStr);
        if ( msgNum < 0 ) {
            System.err.println("\n[ERROR] Incorrect value for \"-n\" option (number of messages); must be positive integers!");
            usageAndExit(40);
        }

        // "-t" option (topic name)
        String topicName = cmd.getOptionValue(PulsarDemoUtil.CMD_OPTION_TOPIC_NAME);
        if ( StringUtils.isBlank(topicName) ) {
            System.err.println("\n[ERROR] Incorrect value for \"-t\" option (pulsar topic name); must be a string " +
                    "in format \"[persistent|non-persistent]://<tenant>/<namespace>/<topic>\".\n");
            usageAndExit(50);
        }


        /*
          Load Pulsar connection properties
         */
        ConfLoaderUtil confLoaderUtil = new ConfLoaderUtil(cfgFile);
        if ( !confLoaderUtil.isConfigLoadSuccess() ) {
            System.exit(60);
        }

        /*
          Establish connection to Pulsar
         */
        Map<String, Object> clientConfMap = confLoaderUtil.getClientConfMap();
        String pulsarBrokerSvcUrl = null;
        if (clientConfMap.containsKey(CommonUtil.CONF_KEY_BROKER_SVC_URL))
            pulsarBrokerSvcUrl = clientConfMap.get(CommonUtil.CONF_KEY_BROKER_SVC_URL).toString();
        if ( StringUtils.isEmpty(pulsarBrokerSvcUrl) ) {
            System.err.println("\n[ERROR] Configuration file must include \"" + CommonUtil.CONF_KEY_BROKER_SVC_URL + "\" property");
            usageAndExit(70);
        }

        Map<String, Object> clientMiscConfMap = confLoaderUtil.getClientMiscConfMap();
        PulsarClientConn clientConn = new PulsarClientConn(pulsarBrokerSvcUrl, clientConfMap, clientMiscConfMap);
        PulsarClient pulsarClient = clientConn.getPulsarClient();
        if (pulsarClient == null) {
            System.out.println("\n[ERROR] Failed to establish connection to Pulsar server with the specified" +
                    " connection properties: { " + clientConn + "}");
            System.exit(80);
        }


        /*
          Create producer or consumer depending on the specified operation type
         */
        if (isProducerOp) {
            try {
                PulsarProducer pulsarProducer = new PulsarProducer(pulsarClient, topicName);
                pulsarProducer.sendMessageSync(msgNum);
                pulsarProducer.close();
            } catch (PulsarClientException e) {
                e.printStackTrace();

                System.out.println("\n[ERROR] Failed to create a Pulsar producer and/or send messages");
                System.exit(90);
            }
        }
        else if (isConsumerOp) {
            // "-sn" option (subscription name) - consumer only
            String subName = cmd.getOptionValue(CommonUtil.CMD_OPTION_SUBSCRIPTION_NAME);
            if (StringUtils.isBlank(subName)) {
                System.err.println("\n[ERROR] value for \"-sn\" option (subscription name) can't be empty for a consumer.");
                usageAndExit(100);
            }

            // "-st" option (subscription type) - consumer only
            String subType = cmd.getOptionValue(PulsarDemoUtil.CMD_OPTION_SUBSCRIPTION_TYPE);
            if (StringUtils.isBlank(subType) ||
                    !StringUtils.equalsAny(subType,
                            PulsarDemoUtil.SUBSCRIPTION_TYPE.Exclusive.label,
                            PulsarDemoUtil.SUBSCRIPTION_TYPE.Failover.label,
                            PulsarDemoUtil.SUBSCRIPTION_TYPE.Shared.label,
                            PulsarDemoUtil.SUBSCRIPTION_TYPE.Key_Shared.label)) {
                System.err.println("\n[ERROR] value for \"-st\" option (subscription type); possible valid values: " +
                        PulsarDemoUtil.getValidSubscriptionTypeList());
                usageAndExit(110);
            }

            try {
                PulsarConsumer pulsarConsumer = new PulsarConsumer(pulsarClient, topicName, subName, subType);
                pulsarConsumer.recvMessageSync(msgNum);
                pulsarConsumer.close();
            } catch (PulsarClientException e) {
                e.printStackTrace();

                System.out.println("\n[ERROR] Failed to create a Pulsar consumer and/or receive messages");
                System.exit(120);
            }
        }

        System.exit(0);
    }
}