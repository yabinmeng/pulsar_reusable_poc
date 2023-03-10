package com.example.pulsarworkshop;

import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;

import org.apache.pulsar.client.api.*;

public class ReaderApp extends PulsarWorkshopCmdApp {

    private PulsarClient pulsarClient;
    private Consumer<?> pulsarConsumer;

    public ReaderApp(String[] inputParams) {
        super(inputParams);
    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new ReaderApp(args);

        int exitCode = workshopApp.run("RedeliveryConsumerApp");
        
        System.exit(exitCode);
    }

    @Override
    public void processInputParams() throws InvalidParamException {
        if (numMsg < 3) {
        	throw new InvalidParamException("The number of messages to process must be 3 or greater.");
        }
    }
    
    @Override
    public void runApp() {

        try {
            PulsarClient pulsarClient = createNativePulsarClient();
        	MessageId messageId = produceMessages(pulsarClient);
        	consumeMessages(pulsarClient, messageId);
        	
        }
        catch (Exception e) {
        	e.printStackTrace();
        	throw new WorkshopRuntimException("Unexpected error when consuming Pulsar messages: " + e.getMessage());
        }
    }
    
    private MessageId produceMessages(PulsarClient pulsarClient)
    					throws Exception {
    

    	MessageId messageId = null;
    	
		MessageId tempId = null;
		for (int i=0; i < 10; i++) {
			Producer pulsarProducer = createPulsarProducer(pulsarTopicName, pulsarClient);
			tempId = pulsarProducer.send(("message " + i).getBytes());
			
			// Grab message id half way through the list
			if (i == numMsg/2) {
				messageId = tempId;
			}
    	}
		
    	return messageId;
    }
    
    private void consumeMessages(PulsarClient pulsarClient, MessageId startMessageId)
    					throws Exception {
        
        Reader<byte[]> pulsarReader = (Reader<byte[]>) pulsarClient.newReader()
        		.topic(pulsarTopicName)
        		.startMessageId(startMessageId)
        		.create();
        		
        		
        Message<byte[]> message = pulsarReader.readNext();
    	System.out.println("########### Reading message " + startMessageId + ": " + new String(message.getData()));
    	
        message = pulsarReader.readNext();
    	System.out.println("########### Reading message " + startMessageId + ": " + new String(message.getData()));
    	
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