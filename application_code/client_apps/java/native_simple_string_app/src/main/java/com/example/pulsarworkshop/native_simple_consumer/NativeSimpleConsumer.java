package com.example.pulsarworkshop.native_simple_consumer;

import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.InvalidParamException;

import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;
import org.apache.pulsar.client.api.*;

public class NativeSimpleConsumer extends PulsarWorkshopCmdApp {

    private PulsarClient pulsarClient;
    private Consumer<?> pulsarConsumer;

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
                Message<?> message = pulsarConsumer.receive();
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
        // (Required) Pulsar subscription name
        subscriptionName = processStringInputParam("subName");

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