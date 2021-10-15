package com.example.fastjms.queue_pattern;

import com.example.conn.JmsClientConn;
import com.example.util.CommonUtil;
import com.example.util.ConfLoaderUtil;
import com.example.util.JmsDemoUtil;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jms.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class QueuePatternDemo {

    static final Logger LOGGER = LogManager.getLogger(QueuePatternDemo.class);

    /**
     *  Define Command Line Arguments
     */
    static Options options = new Options();

    static {
        Option helpOption = new Option(
                CommonUtil.CMD_OPTION_HELP,false, "Displays this help message.");
        Option cfgOption = new Option(
                CommonUtil.CMD_OPTION_CFG_FILE, true, "Configuration properties file.");
        Option queuePatternOption = new Option(
                JmsDemoUtil.CMD_OPTION_PATTERN_NAME, true, "JMS Pattern Name - " +
                "\"" + JmsDemoUtil.getValidQueuePatternList() + "\"" );
        Option destNameOption = new Option(
                JmsDemoUtil.CMD_OPTION_DEST_NAME, true, "JMS destination name: pulsar topic name");
        Option msgSelectorOption = new Option(
                JmsDemoUtil.CMD_OPTION_MSG_SELECTOR, true, "JMS message selector: \"selector pattern\"");

        options.addOption(helpOption);
        options.addOption(cfgOption);
        options.addOption(queuePatternOption);
        options.addOption(destNameOption);
        options.addOption(msgSelectorOption);
    }

    static void usageAndExit(int errorCode) {

        System.out.println();

        PrintWriter errWriter = new PrintWriter(System.out, true);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(errWriter, 150, "QueuePatternDemo",
                "QueuePatternDemo Options:",
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

        // "-pn" option (queue patten name) - send or recv
        String queuePatternName = cmd.getOptionValue(JmsDemoUtil.CMD_OPTION_PATTERN_NAME);
        if ( StringUtils.isBlank(queuePatternName) || !JmsDemoUtil.isValidQueuePattern(queuePatternName) ) {
            System.err.println("" +
                    "\n[ERROR] Incorrect value for \"" + JmsDemoUtil.CMD_OPTION_PATTERN_NAME +
                    "\" option (operation type); possible valid values:  " +
                    CommonUtil.getValidOpTypeList() + "\".\n");
            usageAndExit(30);
        }

        // "-dn" option (JMS destination name)
        String destName = cmd.getOptionValue(JmsDemoUtil.CMD_OPTION_DEST_NAME);
        if (StringUtils.isBlank(destName)) {
            System.err.println("\n[ERROR] JMS destination name is mandatory!\n");
            usageAndExit(40);
        }

        // "-ms" option (JMS message selector pattern string)
        String msgSelector = cmd.getOptionValue(JmsDemoUtil.CMD_OPTION_MSG_SELECTOR);


        /*
          Load Pulsar connection properties
         */
        ConfLoaderUtil confLoaderUtil = new ConfLoaderUtil(cfgFile);
        if ( !confLoaderUtil.isConfigLoadSuccess() ) {
            System.exit(50);
        }

        /*
          Get JMS connection factory
         */
        try {
            Map<String, Object> jmsConfMap = JmsDemoUtil.getPulsarJmsConfMap(confLoaderUtil);
            JmsClientConn jmsClientConn = new JmsClientConn(true, destName, jmsConfMap);
            QueueConnection queueConnection = (QueueConnection) jmsClientConn.getDftJmsConnection();
            Queue queue = (Queue) jmsClientConn.getDftDestination();
            if ((queueConnection == null) && (queue == null)) {
                System.out.println("\n[ERROR] Failed to create JMS context or destination: \n" +
                        "        - Queue Pattern Name: " + queuePatternName + "\n" +
                        "        - Destination Name: " + destName + "\n" +
                        "        - JMS configuration: { " + jmsConfMap + "}");
                System.exit(50);
            }

            ExecutorService executor = Executors.newFixedThreadPool(1);
            QueueRequestorService queueRequestorService = null;
            QueueConnection queueRequestorServiceConn = null;

            QueuePatternExample queuePatternClient = null;
            if (StringUtils.equalsIgnoreCase(queuePatternName, JmsDemoUtil.JMS_QUEUE_PATTERN.QueueSender.label)) {
                queuePatternClient = new QueueSenderExample(queueConnection, queue);
            } else if (StringUtils.equalsIgnoreCase(queuePatternName, JmsDemoUtil.JMS_QUEUE_PATTERN.QueueReceiver.label)) {
                queuePatternClient = new QueueReceiverExample(queueConnection, queue, msgSelector);
            } else if (StringUtils.equalsIgnoreCase(queuePatternName, JmsDemoUtil.JMS_QUEUE_PATTERN.QueueRequestor.label)) {
                // Start a service to respond messages sent by a QueueRequestor client
                queueRequestorServiceConn = (QueueConnection) jmsClientConn.newJmsConnection();
                queueRequestorService = new QueueRequestorService(queueRequestorServiceConn, queue);
                executor.submit(new Thread(queueRequestorService));

                // QueueRequestor client
                queuePatternClient = new QueueRequestorClient(queueConnection, queue);
            } else if (StringUtils.equalsIgnoreCase(queuePatternName, JmsDemoUtil.JMS_QUEUE_PATTERN.QueueBrowser.label)) {
                queuePatternClient = new QueueBrowserExample(queueConnection, queue, msgSelector);
            }

            if (queuePatternClient != null) {
                try {
                    queuePatternClient.demo();
                } catch (JMSException jmsException) {
                    jmsException.printStackTrace();
                }
            }

            executor.shutdown();
            while (!executor.isTerminated()) {
            }

            if (queueRequestorService != null) {
                queueRequestorService.close();
            }

            if (queueRequestorServiceConn != null) {
                queueRequestorServiceConn.stop();
                queueRequestorServiceConn.close();
            }

            jmsClientConn.close();
        }
        catch (JMSException jmsException) {
            jmsException.printStackTrace();
        }

        System.exit(0);
    }
}
