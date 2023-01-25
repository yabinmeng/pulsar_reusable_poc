package com.example.pulsarworkshop.nativeapi;

import com.example.pulsarworkshop.common.exception.HelpExitException;
import com.example.pulsarworkshop.nativeapi.utils.PulsarClientCLIAppUtil;
import com.example.pulsarworkshop.common.PulsarClientConf;
import com.example.pulsarworkshop.common.exception.CliOptProcRuntimeException;
import org.apache.commons.cli.*;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.pulsar.client.api.*;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

public class PulsarClientCLIApp {

    final boolean bProducerApp;


    /**
     * For the purpose of command line options parsing and processing
     */
    DefaultParser parser = new DefaultParser();
    CommandLine cmd = null;


    private File configurartionFile;
    private int numMessage;
    private String pulsarSvcUrl;
    private String pulsarClientName;
    private String topicName;

    // Producer only
    private File rawWorkloadFile;

    // Consumer only
    private List<String> consumerTopicList = new ArrayList<>();
    // Consumer only
    private Pattern consumerTopicsPattern;
    // Consumer only
    private String consumerSubscriptionName;
    // Consumer only
    private SubscriptionType consumerSubscriptionType = SubscriptionType.Exclusive;

    PulsarClientCLIApp(boolean producer) {
        this.bProducerApp = producer;
    }

    File getConfigurartionFile() { return configurartionFile; }
    File getRawWorkloadFile() { return rawWorkloadFile; }
    int getNumMessage() { return numMessage; }
    String getPulsarSvcUrl() { return pulsarSvcUrl; }
    String getPulsarClientName() { return pulsarClientName; }
    String getProducerTopicName() { return topicName; }
    List<String> getConsumerTopicList() { return consumerTopicList; }
    Pattern getConsumerTopicsPattern() { return consumerTopicsPattern; }
    String getConsumerSubscriptionName() { return consumerSubscriptionName; }
    SubscriptionType getConsumerSubscriptionType() { return consumerSubscriptionType; }

    static void usageAndExit(int errorCode) {

        System.out.println();

        PrintWriter errWriter = new PrintWriter(System.out, true);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(errWriter, 150, "PulsarClientCLIApp",
                "PulsarClientCLIApp Options:",
                PulsarClientCLIAppUtil.cliOptions, 2, 1, "", true);

        System.out.println();
    }

    void processInputParameters(String[] inputParams) {
        try {
            cmd = parser.parse(PulsarClientCLIAppUtil.cliOptions, inputParams);
        } catch (ParseException e) {
            throw new CliOptProcRuntimeException(10, "Failed to parse the CLI input parameters!");
        }

        // CLI option for help messages (-h)
        if (cmd.hasOption(PulsarClientCLIAppUtil.CLI_OPTION.Help.label)) {
            throw new HelpExitException();
        }

        // CLI option for workshop configuration (-cf)
        String cfgFileName = cmd.getOptionValue(PulsarClientCLIAppUtil.CLI_OPTION.ConfigFile.label);
        if (StringUtils.isNotBlank(cfgFileName)) {
            try {
                configurartionFile = new File(cfgFileName);
                configurartionFile.getCanonicalPath();
            }
            catch (IOException ex) {
                configurartionFile = null;
            }
        }
//        else {
//            configurartionFile = PulsarClientCLIAppUtil.getResourceFile("workshop.properties");
//        }
        if (configurartionFile == null) {
            throw new CliOptProcRuntimeException(20, "Invalid workshop properties file!" );
        }

        // CLI option for message number (-n)
        String msgNumCmdStr = cmd.getOptionValue(PulsarClientCLIAppUtil.CLI_OPTION.MsgNum.label);
        numMessage = NumberUtils.toInt(msgNumCmdStr);
        if ( (numMessage == 0) || (numMessage < -1) ) {
            throw new CliOptProcRuntimeException(30,
                    "Message number must be a positive integer or -1 (all available raw input)!");
        }

        // CLI option for Pulsar service url (-svc)
        pulsarSvcUrl = cmd.getOptionValue(PulsarClientCLIAppUtil.CLI_OPTION.SvcUrl.label);
        if ( StringUtils.isBlank(pulsarSvcUrl) ) {
            throw new CliOptProcRuntimeException(40, "A valid Pulsar service URL must be in format " +
                            "\"pulsar[+ssl]://<pulsar_svr_hostname>:[6650|6651]\"!");
        }

        // CLI option for Pulsar service url (-name)
        // - empty string is OK (no "producer" or "consumer" name)
        pulsarClientName = cmd.getOptionValue(PulsarClientCLIAppUtil.CLI_OPTION.ClntName.label);

        // CLI option for producer topic name (-tp)
        topicName = cmd.getOptionValue(PulsarClientCLIAppUtil.CLI_OPTION.TpName.label);


        /////////////
        // Producer only ....
        //
        // CLI option for workshop configuration (-wf)
        String workloadFileName = cmd.getOptionValue(PulsarClientCLIAppUtil.CLI_OPTION.WorkloadFile.label);
        if (StringUtils.isNotBlank(workloadFileName)) {
            try {
                rawWorkloadFile = new File(workloadFileName);
                rawWorkloadFile.getCanonicalPath();
            }
            catch (IOException ex) {
                rawWorkloadFile = null;
            }
        }
//        else {
//            rawWorkloadFile = PulsarClientCLIAppUtil.getResourceFile("raw_data_iot_telemetry.csv");
//        }
        if (bProducerApp && rawWorkloadFile == null) {
            throw new CliOptProcRuntimeException(50, "Invalid raw workload input file for a producer!" );
        }


        /////////////
        // Consumer only ....
        //
        // CLI option for consumer subscription name (-csn)
        String topicPatnStr = cmd.getOptionValue(PulsarClientCLIAppUtil.CLI_OPTION.TpNamePatn.label);

        if ( ( bProducerApp && (StringUtils.isBlank(topicName) || StringUtils.contains(topicName, ','))) ) {
            throw new CliOptProcRuntimeException(60, "A single topic name is required for a producer!");
        }
        if ( ( !bProducerApp && StringUtils.isBlank(topicName) && StringUtils.isBlank(topicPatnStr)) ) {
            throw new CliOptProcRuntimeException(70, "Either a list of topic names (separated by comma) or " +
                    "a topic name pattern string is required for a consumer!");
        }

        if (!bProducerApp) {
            CollectionUtils.addAll(consumerTopicList, StringUtils.split(topicName, ','));

            // topic list takes precedence over topic pattern
            if (consumerTopicList.size() == 0) {
                try {
                    consumerTopicsPattern = Pattern.compile(topicPatnStr);
                } catch (PatternSyntaxException pse) {
                    throw new CliOptProcRuntimeException(80, "The specified consumer topic pattern string is not valid!");
                }
            }
        }

        // CLI option for consumer subscription name (-csn)
        consumerSubscriptionName = cmd.getOptionValue(PulsarClientCLIAppUtil.CLI_OPTION.SubName.label);
        if (!bProducerApp && StringUtils.isBlank(consumerSubscriptionName)) {
            throw new CliOptProcRuntimeException(90, "Subscription name is required for a consumer!");
        }

        // CLI option for subscription type (-cst) - consumer only
        String subType = cmd.getOptionValue(PulsarClientCLIAppUtil.CLI_OPTION.SubType.label);
        if ( StringUtils.isBlank(subType) ) {
            subType = PulsarClientCLIAppUtil.SUBSCRIPTION_TYPE.Exclusive.label;
        }
        if ( !bProducerApp &&
             !PulsarClientCLIAppUtil.isValidSubscriptionType(subType) ) {
            throw new CliOptProcRuntimeException(100, "Invalid subscription type for a consumer!");
        }
        else {
            consumerSubscriptionType = SubscriptionType.valueOf(subType);
        }
    }

    PulsarClient createPulsarClient(PulsarClientConf pulsarClientConf)
    throws PulsarClientException
    {
        ClientBuilder clientBuilder = PulsarClient.builder();

        Map clientConfMap = new HashMap();
        clientConfMap.putAll(pulsarClientConf.getClientConfMapTgt());

        // Remove the following producer conf parameters since they'll be
        // handled explicitly outside "loadConf()"
        clientConfMap.remove("serviceUrl");
        clientConfMap.remove("authPluginClassName");
        clientConfMap.remove("authParams");
        clientConfMap.remove("enableTls");
        clientConfMap.remove("tlsTrustCertsFilePath");
        clientConfMap.remove("tlsHostnameVerificationEnable");
        clientConfMap.remove("tlsAllowInsecureConnection");

        clientBuilder.loadConf(clientConfMap);
        clientBuilder.serviceUrl(getPulsarSvcUrl());

        // Pulsar Authentication
        String authPluginClassName = pulsarClientConf.getClientConfValueRaw("authPluginClassName");
        String authParams = pulsarClientConf.getClientConfValueRaw("authParams");

        if ( !StringUtils.isAnyBlank(authPluginClassName, authParams) ) {
            clientBuilder.authentication(authPluginClassName, authParams);
        }

        boolean useTls = StringUtils.contains(pulsarSvcUrl, "pulsar+ssl");
        if ( useTls ) {
            boolean tlsHostnameVerificationEnable = BooleanUtils.toBoolean(
                    pulsarClientConf.getClientConfValueRaw("tlsHostnameVerificationEnable"));
            clientBuilder.enableTlsHostnameVerification(tlsHostnameVerificationEnable);

            String tlsTrustCertsFilePath =
                    pulsarClientConf.getClientConfValueRaw("tlsTrustCertsFilePath");
            if (!StringUtils.isBlank(tlsTrustCertsFilePath)) {
                clientBuilder.tlsTrustCertsFilePath(tlsTrustCertsFilePath);
            }

            boolean tlsAllowInsecureConnection = BooleanUtils.toBoolean(
                    pulsarClientConf.getClientConfValueRaw("tlsAllowInsecureConnection"));
            clientBuilder.allowTlsInsecureConnection(tlsAllowInsecureConnection);
        }

        return clientBuilder.build();
    }

    public Producer createPulsarProducer(PulsarClient pulsarClient,
                                         PulsarClientConf pulsarClientConf)
    throws PulsarClientException
    {
        ProducerBuilder producerBuilder = pulsarClient.newProducer();

        Map producerConfMap = new HashMap();
        producerConfMap.putAll(pulsarClientConf.getProducerConfMapTgt());

        // Remove the following producer conf parameters since they'll be
        // handled explicitly outside "loadConf()"
        producerConfMap.remove("topicName");
        producerConfMap.remove("producerName");


        producerBuilder.loadConf(producerConfMap);

        producerBuilder.topic(getProducerTopicName());
        String clientName = getPulsarClientName();
        if (StringUtils.isNotBlank(clientName)) {
            producerBuilder.producerName(clientName);
        }

        return producerBuilder.create();
    }

    public Consumer<?> createPulsarConsumer(PulsarClient pulsarClient,
                                            PulsarClientConf pulsarClientConf)
    throws PulsarClientException
    {
        ConsumerBuilder<?> consumerBuilder = pulsarClient.newConsumer();

        Map consumerConfMap = new HashMap();
        consumerConfMap.putAll(pulsarClientConf.getConsumerConfMapTgt());

        // Remove the following consumer conf parameters since they'll be
        // handled explicitly outside "loadConf()"
        consumerConfMap.remove("topicNames");
        consumerConfMap.remove("topicsPattern");
        consumerConfMap.remove("subscriptionName");
        consumerConfMap.remove("subscriptionType");
        consumerConfMap.remove("consumerName");

        // TODO: It looks like loadConf() method can't handle the following settings properly.
        //       Do these settings manually for now
        //       - deadLetterPolicy
        //       - negativeAckRedeliveryBackoff
        //       - ackTimeoutRedeliveryBackoff
        consumerConfMap.remove("deadLetterPolicy");
        consumerConfMap.remove("negativeAckRedeliveryBackoff");
        consumerConfMap.remove("ackTimeoutRedeliveryBackoff");


        consumerBuilder.loadConf(consumerConfMap);

        String clientName = getPulsarClientName();
        if (StringUtils.isNotBlank(clientName)) {
            consumerBuilder.consumerName(clientName);
        }

        List<String> topicNames = getConsumerTopicList();
        if (!topicNames.isEmpty()) {
            consumerBuilder.topics(topicNames);
        }
        else {
            Pattern topicNamePattern = getConsumerTopicsPattern();
            consumerBuilder.topicsPattern(topicNamePattern);
        }

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

        consumerBuilder.subscriptionName(getConsumerSubscriptionName());
        consumerBuilder.subscriptionType(getConsumerSubscriptionType());

        return consumerBuilder.subscribe();
    }
}
