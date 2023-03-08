package com.example.pulsarworkshop;

import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;

import org.apache.commons.cli.Option;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.shade.org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PulsarConsumerFullCfg extends PulsarWorkshopCmdApp {

    private final static Logger logger = LoggerFactory.getLogger(PulsarConsumerFullCfg.class);

    private String subsriptionName;
    private SubscriptionType subscriptionType = SubscriptionType.Exclusive;

    private PulsarClient pulsarClient;
    private Consumer<?> pulsarConsumer;

    public PulsarConsumerFullCfg(String[] inputParams) {
        super(inputParams);

        addCommandLineOption(new Option("sbt","subType", true, "Pulsar subscription type."));
        addCommandLineOption(new Option("sbn", "subName", true, "Pulsar subscription name."));
    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new PulsarConsumerFullCfg(args);

        int exitCode = workshopApp.run("PulsarConsumerFullCfg");

        System.exit(exitCode);
    }

    @Override
    public void processInputParams() throws InvalidParamException {
        // (Required) Pulsar subscription name
        subsriptionName = processStringInputParam("subName");

        // (Optional) Pulsar subscription type
        String subType = processStringInputParam("subType");
        if (!StringUtils.isBlank(subType)) {
        try {
	            subscriptionType = SubscriptionType.valueOf(subType);
	        }
	        catch (IllegalArgumentException iae) {
	            subscriptionType = SubscriptionType.Exclusive;
	        }
        }
    }

    @Override
    public void runApp() {
        try {
            pulsarClient = createNativePulsarClient();
            pulsarConsumer = createPulsarConsumer(
                    pulsarTopicName,
                    pulsarClient,
                    subsriptionName,
                    subscriptionType);

            int msgRecvd = 0;
            if (numMsg == -1) {
                numMsg = Integer.MAX_VALUE;
            }

            while (msgRecvd < numMsg) {
                Message<?> message = pulsarConsumer.receive();
                if (logger.isDebugEnabled()) {
                    logger.debug("({}) Message received and acknowledged: " +
                                    "msg-key={}; msg-properties={}; msg-payload={}",
                            pulsarConsumer.getConsumerName(),
                            message.getKey(),
                            message.getProperties(),
                            new String(message.getData()));
                }
                pulsarConsumer.acknowledge(message);

                msgRecvd++;
            }

        }
        catch (PulsarClientException pce) {
            throw new WorkshopRuntimException("Unexpected error when consuming Pulsar messages: " + pce.getMessage());
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
