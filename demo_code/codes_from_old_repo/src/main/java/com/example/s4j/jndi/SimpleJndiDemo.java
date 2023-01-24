package com.example.s4j.jndi;

import com.example.util.CommonUtil;
import com.example.util.ConfLoaderUtil;
import com.example.util.JmsDemoUtil;
import org.apache.commons.cli.*;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jms.*;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;

public class SimpleJndiDemo {

    static final Logger LOGGER = LogManager.getLogger(SimpleJndiDemo.class);

    /**
     *  Define Command Line Arguments
     */
    static Options options = new Options();

    static {
        Option helpOption = new Option(
                CommonUtil.CMD_OPTION_HELP,false, "Displays this help message.");
        Option cfgOption = new Option(
                CommonUtil.CMD_OPTION_CFG_FILE, true, "Configuration properties file.");
        Option destTypeOption = new Option(
                JmsDemoUtil.CMD_OPTION_DEST_TYPE, true, "JMS destination type - " +
                "\"" + JmsDemoUtil.getValidDestinationTypeList() + "\"");
        Option destNameOption = new Option(
                JmsDemoUtil.CMD_OPTION_DEST_NAME, true, "JMS destination name: pulsar topic name");
        Option msgSelectorOption = new Option(
                JmsDemoUtil.CMD_OPTION_MSG_SELECTOR, true, "JMS message selector: \"selector pattern\"");

        options.addOption(helpOption);
        options.addOption(cfgOption);
        options.addOption(destTypeOption);
        options.addOption(destNameOption);
        options.addOption(msgSelectorOption);
    }

    static void usageAndExit(int errorCode) {

        System.out.println();

        PrintWriter errWriter = new PrintWriter(System.out, true);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(errWriter, 150, "SimpleJndiDemo",
                "SimpleJndiDemo Options:",
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

        // "-dt" option (JMS destination type)
        String destType = cmd.getOptionValue(JmsDemoUtil.CMD_OPTION_DEST_TYPE);
        if (StringUtils.isBlank(destType) ||
                !JmsDemoUtil.isValidDestinationType(destType)) {
            System.err.println("\n[ERROR] JMS destination type is mandatory with possible valid values: \"" +
                    JmsDemoUtil.getValidDestinationTypeList() + "\".\n");
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

        Context jndiContext;
        ConnectionFactory connectionFactory = null;
        JMSContext jmsContext = null;
        Session session;
        Destination destination = null;
        JMSProducer producer;

        try {
            Map<String, Object> jmsConfMap = JmsDemoUtil.getPulsarJmsConfMap(confLoaderUtil, true);
            jndiContext = new InitialContext(MapUtils.toProperties(jmsConfMap));

            connectionFactory = (ConnectionFactory) jndiContext.lookup("ConnectionFactory");

            if ( StringUtils.equalsIgnoreCase(destType, JmsDemoUtil.JMS_DEST_TYPE.topic.label) )
                destination = (Destination) jndiContext.lookup("topics/" + destName);
            else
                destination = (Destination) jndiContext.lookup("queues/" + destName);
        }
        catch (NamingException namingException) {
            namingException.printStackTrace();
        }

        try {
            jmsContext = connectionFactory.createContext();
            producer = jmsContext.createProducer();
            TextMessage message = jmsContext.createTextMessage();
            for (int i = 0; i < 10; i++) {
                message.setText("This is message " + (i + 1));
                LOGGER.info("Sending message: " + message.getText());
                producer.send(destination, message);
            }

        } catch (JMSException e) {
            LOGGER.info("Exception occurred: " + e);
        } finally {
            if (jmsContext != null) {
                jmsContext.close();
            }
        }

        System.exit(0);
    }
}
