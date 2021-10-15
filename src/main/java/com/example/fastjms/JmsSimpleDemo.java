package com.example.fastjms;

import com.example.conn.JmsClientConn;
import com.example.util.CommonUtil;
import com.example.util.ConfLoaderUtil;
import com.example.util.JmsDemoUtil;
import org.apache.commons.cli.*;
import org.apache.commons.lang.math.NumberUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jms.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Map;

public class JmsSimpleDemo {

    static final Logger LOGGER = LogManager.getLogger(JmsSimpleDemo.class);

    /**
     *  Define Command Line Arguments
     */
    static Options options = new Options();

    static {
        Option helpOption = new Option(
                CommonUtil.CMD_OPTION_HELP,false, "Displays this help message.");
        Option cfgOption = new Option(
                CommonUtil.CMD_OPTION_CFG_FILE, true, "Configuration properties file.");
        Option opTypeOption = new Option(
                CommonUtil.CMD_OPTION_OPTYPE, true, "Operation type - " +
                "\"" + CommonUtil.getValidOpTypeList() + "\"" );
        Option msgNumOption = new Option(
                CommonUtil.CMD_OPTION_MSGNUM, true, "Number of messages");
        Option destTypeOption = new Option(
                JmsDemoUtil.CMD_OPTION_DEST_TYPE, true, "JMS destination type - " +
                "\"" + JmsDemoUtil.getValidDestinationTypeList() + "\"");
        Option destNameOption = new Option(
                JmsDemoUtil.CMD_OPTION_DEST_NAME, true, "JMS destination name: pulsar topic name");
        // Only relevant when OpType is "SharedConsumer", "DurableConsumer", or "SharedDurableConsumer"
        Option subNameOption = new Option(
                CommonUtil.CMD_OPTION_SUBSCRIPTION_NAME, true, "Subscription name");

        options.addOption(helpOption);
        options.addOption(cfgOption);
        options.addOption(opTypeOption);
        options.addOption(msgNumOption);
        options.addOption(destTypeOption);
        options.addOption(destNameOption);
        options.addOption(subNameOption);
    }

    static void usageAndExit(int errorCode) {

        System.out.println();

        PrintWriter errWriter = new PrintWriter(System.out, true);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(errWriter, 150, "JmsSimpleDemo",
                "JmsSimpleDemo Options:",
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
        } catch (ParseException e) {
            System.err.format("\n[ERROR] Failure parsing argument inputs: %s.\n", e.getMessage());
            usageAndExit(10);
        }

        // Print help message
        if ((cmd != null) && cmd.hasOption(CommonUtil.CMD_OPTION_HELP)) {
            usageAndExit();
        }

        // "-f" option (configuration file): default to "conn.properties" under the same folder!
        String cfgFile = cmd.getOptionValue(CommonUtil.CMD_OPTION_CFG_FILE);
        if (StringUtils.isBlank(cfgFile)) {
            cfgFile = CommonUtil.DFT_PULSAR_CONN_CFG_FILE_NAME;

            try {
                File file = new File(cfgFile);
                String canonicalFilePath = file.getCanonicalPath();
            } catch (IOException ioe) {
                System.out.println("Can't read the specified config properties file (" + cfgFile + ")!");
                // ioe.printStackTrace();
                usageAndExit(20);
            }
        }

        // "-op" option (operation type) - send or recv
        String opType = cmd.getOptionValue(CommonUtil.CMD_OPTION_OPTYPE);
        if ( StringUtils.isBlank(opType) || !CommonUtil.isValidOpType(opType) ) {
            System.err.println("" +
                            "\n[ERROR] Incorrect value for \"-op\" option (operation type); " +
                            "possible valid values:  " + CommonUtil.getValidOpTypeList() + "\".\n");
            usageAndExit(30);
        }
        boolean isSenderOp = StringUtils.equalsIgnoreCase(opType, CommonUtil.OP_TYPE.Producer.label);

        // "-n" option (message number)
        String msgNumCmdStr = cmd.getOptionValue(CommonUtil.CMD_OPTION_MSGNUM);
        int msgNum = NumberUtils.toInt(msgNumCmdStr);
        if ( msgNum < 0 ) {
            System.err.println("\n[ERROR] Incorrect value for \"-n\" option (number of messages); must be positive integers!");
            usageAndExit(40);
        }

        // "-dt" option (JMS destination type)
        String destType = cmd.getOptionValue(JmsDemoUtil.CMD_OPTION_DEST_TYPE);
        if (StringUtils.isBlank(destType) ||
                !JmsDemoUtil.isValidDestinationType(destType)) {
            System.err.println("\n[ERROR] JMS destination type is mandatory with possible valid values: \"" +
                    JmsDemoUtil.getValidDestinationTypeList() + "\".\n");
            usageAndExit(50);
        }

        // "-dn" option (JMS destination name)
        String destName = cmd.getOptionValue(JmsDemoUtil.CMD_OPTION_DEST_NAME);
        if (StringUtils.isBlank(destName)) {
            System.err.println("\n[ERROR] JMS destination name is mandatory!\n");
            usageAndExit(60);
        }


        /*
          Load Pulsar connection properties
         */
        ConfLoaderUtil confLoaderUtil = new ConfLoaderUtil(cfgFile);
        if ( !confLoaderUtil.isConfigLoadSuccess() ) {
            System.exit(70);
        }

        /*
          Get JMS connection factory
         */
        try {
            boolean isQueue = StringUtils.equalsIgnoreCase(destType, JmsDemoUtil.JMS_DEST_TYPE.queue.label);
            Map<String, Object> jmsConfMap = JmsDemoUtil.getPulsarJmsConfMap(confLoaderUtil);
            JmsClientConn jmsClientConn = new JmsClientConn(isQueue, destName, jmsConfMap);
            JMSContext jmsContext = jmsClientConn.getDftJmsContext();
            Destination destination = jmsClientConn.getDftDestination();
            if ( (jmsContext == null) && (destination == null) ) {
                System.out.println("\n[ERROR] Failed to create JMS context or destination: \n" +
                        "        - OpType: " + opType + "\n" +
                        "        - Destination Name: " + destName + "\n" +
                        "        - Destination Type: " + destType + "\n" +
                        "        - JMS configuration: { " + jmsConfMap + "}");
                System.exit(80);
            }


            /*
              Create JMS sender or receiver depending on the specified operation type
             */
            DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
            Date now = Calendar.getInstance().getTime();

            LOGGER.info("---------------------------------------------------------");
            LOGGER.info("JMS Operation Type : {}",
                    isSenderOp ? CommonUtil.OP_TYPE.Producer.label : CommonUtil.OP_TYPE.Consumer.label);
            LOGGER.info("JMS Destination Type : {}", destType);
            LOGGER.info("JMS Destination Name : {}", destName);
            LOGGER.info("Total number of messages to send/receive: {}", msgNum );
            LOGGER.info("---------------------------------------------------------");

            // JMS sender
            if ( isSenderOp ) {
                JMSProducer sender = jmsContext.createProducer();

                for (int i = 0; i < msgNum; i++) {
                    CommonUtil.pause(100);

                    int payloadStrLen = RandomUtils.nextInt(
                            CommonUtil.MIN_RNDM_STRING_LENGTH, CommonUtil.MAX_RNDM_STRING_LENGTH);
                    String msgPayload = CommonUtil.randomString(payloadStrLen);

                    LOGGER.info("  message sent: msg-body={{}}", msgPayload);
                    sender.send(destination, msgPayload);
                }
            }
            // JMS Receiver
            else {
                // "-sn" option (subscription name) - consumer only
                String subName = cmd.getOptionValue(CommonUtil.CMD_OPTION_SUBSCRIPTION_NAME);

                // For "SharedConsumer", "DurableConsumer", or "SharedDurableConsumer",
                if (!StringUtils.equalsIgnoreCase(opType,CommonUtil.OP_TYPE.Consumer.label)) {
                    // 1) subscription name is a must
                    if (StringUtils.isBlank(subName)) {
                        System.err.println("\n[ERROR] value for \"-sn\" option (subscription name) can't be empty for a consumer.");
                        usageAndExit(110);
                    }

                    // 2) destination type must be "topic"
                    if (!StringUtils.equalsIgnoreCase(destType, JmsDemoUtil.JMS_DEST_TYPE.topic.label)) {
                        System.err.println("" +
                                "\n[ERROR] value for \"-dt\" option (destination type) must be a topic for \"" +
                                opType + "\".");
                        usageAndExit(120);
                    }
                }

                JMSConsumer receiver = jmsContext.createConsumer(destination);

                if (StringUtils.equalsIgnoreCase(opType, CommonUtil.OP_TYPE.DurableConsumer.label))
                    receiver = jmsContext.createDurableConsumer((Topic) destination, subName);
                else if (StringUtils.equalsIgnoreCase(opType, CommonUtil.OP_TYPE.SharedConsumer.label))
                    receiver = jmsContext.createSharedConsumer((Topic) destination, subName);
                else if (StringUtils.equalsIgnoreCase(opType, CommonUtil.OP_TYPE.SharedDurableConsumer.label))
                    receiver = jmsContext.createSharedDurableConsumer((Topic) destination, subName);

                int numMessagesConsumed = 0;

                while (msgNum == 0 || numMessagesConsumed < msgNum) {
                    Message message = receiver.receive();
                    message.acknowledge();

                    String msgPayload = message.getBody(String.class);
                    LOGGER.info("  message received: msg-body=" + msgPayload);
                    numMessagesConsumed++;
                }

                receiver.close();
            }

            jmsClientConn.close();
        }
        catch (JMSException jmsException) {
            jmsException.printStackTrace();
        }

        System.exit(0);
    }
}