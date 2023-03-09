package com.example.pulsarworkshop;
import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;
import org.apache.commons.cli.Option;
import org.apache.pulsar.shade.org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import com.rabbitmq.client.Connection;
import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class S4RQueueConsumer extends PulsarWorkshopCmdApp {
    private final static Logger logger = LoggerFactory.getLogger(S4RQueueConsumer.class);
    int S4RPort = 5672;
    String S4RQueueName = "s4r-default-queue";
    ConnectionFactory S4RFactory;
    Connection connection;
    Channel channel;
    DefaultConsumer consumer;
    int MsgReceived = 0;
    public S4RQueueConsumer(String[] inputParams) {
        super(inputParams);
        addCommandLineOption(new Option("p", "s4rport", true, "S4R Pulsar RabbitMQ port number."));
        addCommandLineOption(new Option("q", "s4rqueue", true, "S4R Pulsar RabbitMQ queue name."));
    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new S4RQueueConsumer(args);

        int exitCode = workshopApp.run("S4RQueueConsumer");

        System.exit(exitCode);
    }

    @Override
    public void processInputParams() throws InvalidParamException {
        S4RPort = processIntegerInputParam("s4rport");
    	if ( S4RPort <= 0  ) {
    		throw new InvalidParamException("S4RPort number must be a positive integer.  Default is 5672");
    	}
        String queueName = processStringInputParam("s4rqueue");
        if (!StringUtils.isBlank(queueName)) {
            S4RQueueName = queueName;
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
            channel.queueDeclare(S4RQueueName, true, false, false, null);
            consumer = new DefaultConsumer(channel) {
                @Override
                 public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                        String message = new String(body, "UTF-8");
                        // process the message
                        logger.info("SR4 Consumer received message count: " + MsgReceived + " Message: " + message);
                        MsgReceived++;
                 }
            };
            channel.basicConsume(S4RQueueName, true, consumer);
            logger.info("SR4 Consumer created for queue " + S4RQueueName + " running until " + numMsg + " messages are received.");
            while (numMsg > MsgReceived) {
                Thread.sleep(2000);    
            }
        } catch (Exception e) {
            throw new WorkshopRuntimException("Unexpected error when consuming S4R messages: " + e.getMessage());   
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
