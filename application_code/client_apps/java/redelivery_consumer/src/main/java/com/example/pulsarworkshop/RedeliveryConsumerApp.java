package com.example.pulsarworkshop;

import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;

import java.util.concurrent.TimeUnit;

import org.apache.commons.cli.Option;
import org.apache.pulsar.client.api.*;

public class RedeliveryConsumerApp extends PulsarWorkshopCmdApp {

    private String subsriptionName;
    private PulsarClient pulsarClient;
    private Consumer<?> pulsarConsumer;

    private String deadLetterTopicName;

    public RedeliveryConsumerApp(String[] inputParams) {
        super(inputParams);

        addCommandLineOption(new Option("sbn", "subName", true, "Pulsar subscription name."));
        addCommandLineOption(new Option("dlt", "deadLetterTopic", true, 
        			"Pulsar dead letter topic where message go if redelivery fails."));
    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new RedeliveryConsumerApp(args);

        int exitCode = workshopApp.run("RedeliveryConsumerApp");
        
        System.exit(exitCode);
    }

    @Override
    public void processInputParams() throws InvalidParamException {
        // (Required) Pulsar subscription name
        subsriptionName = processStringInputParam("subName");

        // (Required) Pulsar dead letter topic
        deadLetterTopicName = processStringInputParam("dlt");
    }

    @Override
    public void runApp() {

        try {
        	
            pulsarClient = createNativePulsarClient();

            Consumer<byte[]> pulsarConsumer = pulsarClient.newConsumer()
                    .ackTimeout(1, TimeUnit.SECONDS)
                    .topic(pulsarTopicName)
                    .subscriptionName(subsriptionName)
                    .subscriptionType(SubscriptionType.Shared)
                    .enableRetry(true)
                    .deadLetterPolicy(DeadLetterPolicy.builder()
                            .maxRedeliverCount(5)
                            .deadLetterTopic(deadLetterTopicName)
                            .build())
                    .subscribe();

        	// Negative Acknowledge message until re-delivery attempts are exceeded
        	while (true) {
                Message<byte[]> message = pulsarConsumer.receive();
            	System.out.println("########### Received message: " + new String(message.getData()));
            	pulsarConsumer.negativeAcknowledge(message);
        	}
        }
        catch (Exception e) {
        	e.printStackTrace();
        	throw new WorkshopRuntimException("Unexpected error when consuming Pulsar messages: " + e.getMessage());
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