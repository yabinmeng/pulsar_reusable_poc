package com.example.pulsarworkshop.native_simple_consumer;
import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.SubscriptionType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.ParseException;
import org.apache.pulsar.client.api.*;

public class NativeSimpleConsumer extends PulsarWorkshopCmdApp {
    private final static Logger logger = LoggerFactory.getLogger(NativeSimpleConsumer.class);

    private PulsarClient pulsarClient;
    private Consumer pulsarConsumer;

    private String subscriptionName;
    public NativeSimpleConsumer(String[] inputParams) {
        super(inputParams);
    }

    public static void main(String[] args) throws PulsarClientException {
        PulsarWorkshopCmdApp workshopApp = new NativeSimpleConsumer(args);
        workshopApp.processInputParams();
        workshopApp.runApp();
    }

    @Override
    public void runApp() throws WorkshopRuntimException {
        try {
            setupConsumer();
            // Consume messages from the topic
            while (true) {
                Message<String> message = pulsarConsumer.receive();
                System.out.println("Received message: " + message.getValue());
                pulsarConsumer.acknowledge(message);
            }

        } catch (PulsarClientException pce) {
            // Close the consumer and the client when finished
            try {
                pulsarConsumer.close();
                pulsarClient.close();
            } catch (PulsarClientException e) {
                throw new RuntimeException(e);
            }
            throw new WorkshopRuntimException("Unexpected error when producing Pulsar messages!");
        }
    }
    @Override
    public void processInputParams() throws InvalidParamException {
        CommandLine commandLine = null;

        try {
            commandLine = cmdParser.parse(cliOptions, rawCmdInputParams);
        } catch (ParseException e) {
            throw new InvalidParamException("Failed to parse application CLI input parameters!");
        }

        super.processBasicInputParams(commandLine);
        // (Required) Pulsar subscription name
        subscriptionName = commandLine.getOptionValue("subName");
        if (StringUtils.isBlank(subscriptionName)) {
            throw new InvalidParamException("Empty subscription name!");
        }
    }
    public void setupConsumer() {
        try {
            try {
                pulsarClient = createNativePulsarClient();
            } catch (PulsarClientException e) {
                throw new RuntimeException(e);
            }
            pulsarConsumer = createPulsarConsumer(
                    pulsarTopicName,
                    pulsarClient,
                    subscriptionName,
                    SubscriptionType.Shared);
        } catch (PulsarClientException e) {
            throw new RuntimeException(e);
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