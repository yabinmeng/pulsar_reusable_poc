package com.example.pulsarworkshop;
import org.apache.commons.cli.Option;
import org.apache.pulsar.shade.org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class S4RQueueProducer extends PulsarWorkshopCmdApp {
    private final static Logger logger = LoggerFactory.getLogger(S4RQueueProducer.class);
    int S4RPort = 5672;
    String S4RQueueName = "s4r-default-queue";
    ConnectionFactory S4RFactory;
    Connection connection;
    Channel channel;

    public S4RQueueProducer(String[] inputParams) {
        super(inputParams);
        addCommandLineOption(new Option("s4rport", "s4rport", true, "S4R Pulsar RabbitMQ port number."));
        addCommandLineOption(new Option("q", "s4rqueue", true, "S4R Pulsar RabbitMQ queue name."));
    }
    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new S4RQueueProducer(args);

        int exitCode = workshopApp.run("S4RQueueProducer");

        System.exit(exitCode);
    }

    @Override
    public void processInputParams() throws InvalidParamException {
        S4RPort = processIntegerInputParam("s4rport");
    	if ( S4RPort <= 0  ) {
    		throw new InvalidParamException("S4RPort number must be a positive integer.  Default is 5672");
    	}
        S4RQueueName = processStringInputParam("s4rqueue");
        if (StringUtils.isBlank(S4RQueueName)) {
            S4RQueueName = "s4r-default-queue";
        }
    }

    @Override
    public void runApp() {
        try {
            S4RFactory= new ConnectionFactory();
            S4RFactory.setHost("localhost");
            S4RFactory.setPort(S4RPort);
            connection = S4RFactory.newConnection();
            channel = connection.createChannel();
            channel.confirmSelect();
            channel.queueDeclare(S4RQueueName, true, false, false, null);
            int msgSent = 0;
            while (numMsg > msgSent) {
                String message = "This is the RabbitMQ message ******** Msg num: " + msgSent; 
                channel.basicPublish("", S4RQueueName, null, message.getBytes());
                if (logger.isDebugEnabled()) {
                    logger.debug("Published a message: {}", msgSent);
                }
                msgSent++;
                channel.waitForConfirmsOrDie(5000);  //basically flush after each message published
            }
        } catch (Exception e) {
            throw new WorkshopRuntimException("Unexpected error when producing S4R messages: " + e.getMessage());  
        }
    }

    @Override
    public void termApp() {
        try {
            channel.close();
            connection.close();
        } catch (IOException ioe) {
            throw new WorkshopRuntimException("Unexpected error when shutting down S4R Queue Producer IO Exception: " + ioe.getMessage());  
        } catch (TimeoutException te) {
            throw new WorkshopRuntimException("Unexpected error when shutting down S4R Queue Producer Timeout Exception: " + te.getMessage());  
        }
    }
}
