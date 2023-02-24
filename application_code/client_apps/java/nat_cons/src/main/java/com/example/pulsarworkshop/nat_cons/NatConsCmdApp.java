package com.example.pulsarworkshop.nat_cons;

import com.example.pulsarworkshop.common.PulsarExtraCfgConf;
import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class NatConsCmdApp extends PulsarWorkshopCmdApp {

    private final static Logger logger = LoggerFactory.getLogger(NatConsCmdApp.class);

    // Default to consume 20 messages
    // -1 means to consume all available messages (indefinitely)
    private int numMsg = 20;
    private String subsriptionName;
    private SubscriptionType subscriptionType = SubscriptionType.Exclusive;

    private PulsarClient pulsarClient;
    private Consumer<?> pulsarConsumer;

    public NatConsCmdApp(String[] inputParams) {
        super(inputParams);

        extraCliOptions.addOption(new Option("num","numMsg", true, "Number of message to produce."));
        extraCliOptions.addOption(new Option("sbt","subType", true, "Pulsar subscription type."));
        extraCliOptions.addOption(new Option("sbn", "subName", true, "Pulsar subscription name."));
    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new NatConsCmdApp(args);

        int exitCode = workshopApp.run("NatConsCmdApp");

        System.exit(exitCode);
    }

    @Override
    public void processInputParams() throws InvalidParamException {
        CommandLine commandLine = null;

        try {
            commandLine = cmdParser.parse(getCliOptions(), rawCmdInputParams);
        } catch (ParseException e) {
            throw new InvalidParamException("Failed to parse application CLI input parameters!");
        }

        super.processBasicInputParams(commandLine);

        // (Required) CLI option for number of messages
        String msgNumParam = commandLine.getOptionValue("num");
        int intVal = NumberUtils.toInt(msgNumParam);
        if ( (intVal > 0) || (intVal == -1) ) {
            numMsg = intVal;
        }
        else {
            throw new InvalidParamException("Message number must be a positive integer or -1 (all available raw input)!");
        }

        // (Required) Pulsar subscription name
        subsriptionName = commandLine.getOptionValue("subName");
        if (StringUtils.isBlank(subsriptionName)) {
            throw new InvalidParamException("Empty subscription name!");
        }

        // (Optional) Pulsar subscription type. Default to Exclusive
        String subTypeParam = commandLine.getOptionValue("subType");
        if ( ! StringUtils.isBlank(subTypeParam) ) {
            try {
                subscriptionType = SubscriptionType.valueOf(subTypeParam);
            }
            catch (IllegalArgumentException iae) {
                subscriptionType = SubscriptionType.Exclusive;
            }
        }
    }

    @Override
    public void runApp() {


        PulsarExtraCfgConf extraCfgConf = null;
        if (extraCfgConf != null) {
            extraCfgConf = new PulsarExtraCfgConf(clientConfigFile);
        }

        try {
            pulsarClient = createNativePulsarClient();
            pulsarConsumer = createPulsarConsumer(
                    pulsarTopicName,
                    pulsarClient,
                    extraCfgConf,
                    subsriptionName,
                    subscriptionType);

            int msgRecvd = 0;
            if (numMsg == -1) {
                numMsg = Integer.MAX_VALUE;
            }

            List<CompletableFuture<? extends Message<?>>> msgRecvFutureList = new ArrayList<>();

            while (msgRecvd < numMsg) {
                CompletableFuture<? extends Message<?>> msgRecvFuture = pulsarConsumer.receiveAsync();
                msgRecvFuture.thenAccept( message -> {
                            try {
                                pulsarConsumer.acknowledge(message);

                                if (logger.isDebugEnabled()) {
                                    logger.debug("({}) Message received and acknowledged: " +
                                                    "msg-key={}; msg-properties={}; msg-payload={}",
                                            pulsarConsumer.getConsumerName(),
                                            message.getKey(),
                                            message.getProperties(),
                                            new String(message.getData()));
                                }
                            }
                            catch (PulsarClientException pulsarClientException) {
                                throw new WorkshopRuntimException("Unexpected error when consuming Pulsar messages!");
                            }
                        })
                        .exceptionally( e -> {
                            if (logger.isDebugEnabled()) {
                                logger.debug("Failed to receive a message: {}", e);
                            }
                            return null;
                        });;

                msgRecvFutureList.add(msgRecvFuture);
                msgRecvd++;
            }

            msgRecvFutureList.forEach(CompletableFuture::join);
        }
        catch (PulsarClientException pce) {
            throw new WorkshopRuntimException("Unexpected error when consuming Pulsar messages!");
        }
    }

    @Override
    public void termApp() {
        try {
            if (pulsarConsumer != null) {
                pulsarConsumer.close();
            }

            if (pulsarClient != null) {
                pulsarClient.close();
            }
        }
        catch (PulsarClientException pce) {
            throw new WorkshopRuntimException("Failed to terminate Pulsar producer or client!");
        }
    }
}
