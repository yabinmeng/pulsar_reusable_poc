package com.example.pulsarworkshop.native_simple_producer;

import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;
import org.apache.pulsar.client.api.*;

public class NativeSimpleProducer extends PulsarWorkshopCmdApp {

    private PulsarClient pulsarClient;
    private Producer pulsarProducer;
    public NativeSimpleProducer(String[] inputParams) {
        super(inputParams);
    }

    public static void main(String[] args) throws PulsarClientException {
        PulsarWorkshopCmdApp workshopApp = new NativeSimpleProducer(args);
        workshopApp.processInputParams();
        workshopApp.runApp();
    }

    @Override
    public void runApp() throws WorkshopRuntimException {
        try {
            setupProducer();
            // Publish a message to the topic
            String message = "Hello, Pulsar!";
            pulsarProducer.send(message);

            // Close the producer and the Pulsar client when done
            pulsarProducer.close();
            pulsarClient.close();

        } catch (PulsarClientException pce) {
            throw new WorkshopRuntimException("Unexpected error when producing Pulsar messages!");
        }
    }
    @Override
    public void processInputParams() throws InvalidParamException {
    	// No additional params needed for this client
    }
    public void setupProducer() {
        try {
            try {
                pulsarClient = createNativePulsarClient();
            } catch (PulsarClientException e) {
                throw new RuntimeException(e);
            }
            pulsarProducer = createPulsarProducer(pulsarTopicName, pulsarClient);
        } catch (PulsarClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void termApp() {
        try {
            if (pulsarProducer != null) {
                pulsarProducer.close();
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
