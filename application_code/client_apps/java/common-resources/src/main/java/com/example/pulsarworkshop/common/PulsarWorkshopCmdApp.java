package com.example.pulsarworkshop.common;

import com.example.pulsarworkshop.common.exception.HelpExitException;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

abstract public class PulsarWorkshopCmdApp {

    private final static Logger logger = LoggerFactory.getLogger(PulsarWorkshopCmdApp.class);

    protected String[] rawCmdInputParams;
    protected String pulsarTopicName;
    protected File clientConnfFile;
    protected File clientConfigFile;
    protected boolean useAstraStreaming;

    protected DefaultParser cmdParser;
    protected Options basicCliOptions = new Options();
    protected Options extraCliOptions = new Options();


    public abstract void processInputParams() throws InvalidParamException;
    public abstract void runApp();
    public abstract void termApp();


    public PulsarWorkshopCmdApp(String[] inputParams) {
        this.rawCmdInputParams = inputParams;
        this.cmdParser = new DefaultParser();

        // Basic Command line options
        basicCliOptions.addOption(new Option("h", "help", false, "Displays the usage method."));
        basicCliOptions.addOption(new Option("top", "topic", true, "Pulsar topic name."));
        basicCliOptions.addOption(new Option("con","connFile", true, "\"client.conf\" file path."));
        basicCliOptions.addOption(new Option("cfg", "cfgFile", true, "Extra config properties file path."));
        basicCliOptions.addOption(new Option("as", "astra", false, "Whether to use Astra streaming."));
    }

    public String getPulsarTopicName() { return this.pulsarTopicName; }
    public File getClientConnfFile() { return this.clientConnfFile; }
    public File getClientConfigFile() { return this.clientConfigFile; }

    public void processBasicInputParams(CommandLine cmdLine) throws HelpExitException, InvalidParamException {
        // CLI option for help messages
        if (cmdLine.hasOption("help")) {
            throw new HelpExitException();
        }

        // (Required) CLI option for Pulsar topic
        pulsarTopicName = cmdLine.getOptionValue("top");
        if (StringUtils.isBlank(pulsarTopicName)) {
            throw new InvalidParamException("Empty Pulsar topic name!");
        }

        // (Required) CLI option for client.conf file
        String clntConnFileParam = cmdLine.getOptionValue("con");
        if (StringUtils.isBlank(clntConnFileParam)) {
            throw new InvalidParamException("Must specify the \"client.conf\" file!");
        }
        else {
            try {
                clientConnfFile = new File(clntConnFileParam);
                clientConnfFile.getCanonicalPath();
            } catch (IOException ex) {
                throw new InvalidParamException("Invalid file path for the \"client.conf\" file!");
            }
        }

        // (Optional) CLI option for extra config properties file
        String cfgFileParam = cmdLine.getOptionValue("cfg");
        if (StringUtils.isNotBlank(cfgFileParam)) {
            try {
                clientConnfFile = new File(cfgFileParam);
                clientConnfFile.getCanonicalPath();
            } catch (IOException ex) {
                throw new InvalidParamException("Invalid file path for the client configuration properties file!");
            }
        }

        // (Optional) Whether to use Astra Streaming
        if (cmdLine.hasOption("as")) {
            useAstraStreaming = true;
        }
    }

    public Options getCliOptions() {
        Options options = new Options();
        for (Option opt : basicCliOptions.getOptions()) {
            options.addOption(opt);
        }
        for (Option opt : extraCliOptions.getOptions()) {
            options.addOption(opt);
        }
        return options;
    }

    public void usage(String appNme) {

        PrintWriter printWriter = new PrintWriter(System.out, true);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(printWriter, 150, "appNme",
                "Command Line Options:",
                getCliOptions(), 2, 1, "", true);

        System.out.println();
    }

    protected PulsarClient createNativePulsarClient(PulsarConnCfgConf connCfgConf)
    throws PulsarClientException {
        ClientBuilder clientBuilder = PulsarClient.builder();

        Map<String, String> clientConnMap = connCfgConf.getClientConfMap();

        String pulsarSvcUrl = clientConnMap.get("brokerServiceUrl");
        clientBuilder.serviceUrl(pulsarSvcUrl);

        String authPluginClassName = clientConnMap.get("authPlugin");
        String authParams = clientConnMap.get("authParams");
        if ( !StringUtils.isAnyBlank(authPluginClassName, authParams) ) {
            clientBuilder.authentication(authPluginClassName, authParams);
        }

        // For Astra streaming, there is no need for this section.
        // But for Luna streaming, they're required if TLS is expected.
        if ( !useAstraStreaming && StringUtils.contains(pulsarSvcUrl, "pulsar+ssl") ) {
            boolean tlsHostnameVerificationEnable = BooleanUtils.toBoolean(
                    clientConnMap.get("tlsEnableHostnameVerification"));
            clientBuilder.enableTlsHostnameVerification(tlsHostnameVerificationEnable);

            String tlsTrustCertsFilePath =
                    clientConnMap.get("tlsTrustCertsFilePath");
            if (!StringUtils.isBlank(tlsTrustCertsFilePath)) {
                clientBuilder.tlsTrustCertsFilePath(tlsTrustCertsFilePath);
            }

            boolean tlsAllowInsecureConnection = BooleanUtils.toBoolean(
                    clientConnMap.get("tlsAllowInsecureConnection"));
            clientBuilder.allowTlsInsecureConnection(tlsAllowInsecureConnection);
        }

        return clientBuilder.build();
    }

    protected Producer createPulsarProducer(String topicName,
                                            PulsarClient pulsarClient,
                                            PulsarExtraCfgConf pulsarExtraCfgConf)
            throws PulsarClientException {
        ProducerBuilder producerBuilder = pulsarClient.newProducer();

        if (pulsarExtraCfgConf != null) {
            Map producerConfMap = new HashMap();
            producerConfMap.putAll(pulsarExtraCfgConf.getProducerConfMapTgt());

            // Remove the following producer conf parameters since they'll be
            // handled explicitly outside "loadConf()"
            producerConfMap.remove("topicName");

            producerBuilder.loadConf(producerConfMap);
        }

        producerBuilder.topic(topicName);

        return producerBuilder.create();
    }

    public Consumer<?> createPulsarConsumer(String topicName,
                                            PulsarClient pulsarClient,
                                            PulsarExtraCfgConf pulsarExtraCfgConf,
                                            String consumerSubscriptionName,
                                            SubscriptionType consumerSubscriptionType)
            throws PulsarClientException
    {
        ConsumerBuilder<?> consumerBuilder = pulsarClient.newConsumer();

        Map consumerConfMap = new HashMap();
        if (pulsarExtraCfgConf != null) {
            consumerConfMap.putAll(pulsarExtraCfgConf.getConsumerConfMapTgt());

            // Remove the following consumer conf parameters since they'll be
            // handled explicitly outside "loadConf()"
            consumerConfMap.remove("topicNames");
            consumerConfMap.remove("topicsPattern");
            consumerConfMap.remove("subscriptionName");
            consumerConfMap.remove("subscriptionType");

            // TODO: It looks like loadConf() method can't handle the following settings properly.
            //       Do these settings manually for now
            //       - deadLetterPolicy
            //       - negativeAckRedeliveryBackoff
            //       - ackTimeoutRedeliveryBackoff
            consumerConfMap.remove("deadLetterPolicy");
            consumerConfMap.remove("negativeAckRedeliveryBackoff");
            consumerConfMap.remove("ackTimeoutRedeliveryBackoff");

            consumerBuilder.loadConf(consumerConfMap);
        }

        consumerBuilder.topic(topicName);
        consumerBuilder.subscriptionName(consumerSubscriptionName);
        consumerBuilder.subscriptionType(consumerSubscriptionType);

        if (consumerConfMap.containsKey("deadLetterPolicy")) {
            consumerBuilder.deadLetterPolicy(
                    (DeadLetterPolicy) consumerConfMap.get("deadLetterPolicy"));
        }
        if (consumerConfMap.containsKey("negativeAckRedeliveryBackoff")) {
            consumerBuilder.negativeAckRedeliveryBackoff(
                    (RedeliveryBackoff) consumerConfMap.get("negativeAckRedeliveryBackoff"));
        }
        if (consumerConfMap.containsKey("ackTimeoutRedeliveryBackoff")) {
            consumerBuilder.ackTimeoutRedeliveryBackoff(
                    (RedeliveryBackoff) consumerConfMap.get("ackTimeoutRedeliveryBackoff"));
        }

        return consumerBuilder.subscribe();
    }
}
