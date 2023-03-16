package com.example.pulsarworkshop;
import org.apache.commons.cli.Option;
import org.apache.pulsar.shade.org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.pulsarworkshop.common.PulsarConnCfgConf;
import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Connection;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class S4RQueueProducer extends PulsarWorkshopCmdApp {
    private final static Logger logger = LoggerFactory.getLogger(S4RQueueProducer.class);
    int S4RPort = 5672;
    String S4RQueueName = "s4r-default-queue";
    String S4RMessage = "This is a RabbitMQ message ******** ";
    String S4RRabbitMQHost = "localhost";
    String S4RPassword = "";
    String S4RUser = "";
    String S4RVirtualHost = "";
    ConnectionFactory S4RFactory;
    Connection connection;
    Channel channel;
    File rabbitmqConnfFile;
    Boolean AstraInUse;

    public S4RQueueProducer(String[] inputParams) {
        super(inputParams);
        addCommandLineOption(new Option("q", "s4rqueue", true, "S4R Pulsar RabbitMQ queue name."));
        addCommandLineOption(new Option("m", "s4rmessage", true, "S4R Pulsar RabbitMQ message to send, otherwise a default is used."));
        addCommandLineOption(new Option("c", "rabbitmqconf", true, "S4R Pulsar RabbitMQ conf filename. Default is rabbitmq.conf."));
        addCommandLineOption(new Option("a", "useAstra", true, "Use Astra Streaming for RabbitMQ server."));
    }
    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new S4RQueueProducer(args);

        int exitCode = workshopApp.run("S4RQueueProducer");

        System.exit(exitCode);
    }

    @Override
    public void processInputParams() throws InvalidParamException {
       String queueName = processStringInputParam("s4rqueue");
        if (!StringUtils.isBlank(queueName)) {
            S4RQueueName = queueName;
        }
        String msgToSend = processStringInputParam("s4rmessage");
        if (!StringUtils.isBlank(msgToSend)) {
            S4RMessage = msgToSend;
        }
        rabbitmqConnfFile = processFileInputParam("rabbitmqconf");
        if(rabbitmqConnfFile == null) {
            throw new InvalidParamException("rabbitmq.conf file must be provided.");
        }
        processRabbitMQConfFile();
        String useAstra= processStringInputParam("useAstra");
        if(useAstra != null) {
            AstraInUse = true;
        } else {
            AstraInUse = false;
        }

    }

    @Override
    public void runApp() {
        try {
            S4RFactory= new ConnectionFactory();
            S4RFactory.setHost(S4RRabbitMQHost);
            S4RFactory.setPort(S4RPort);
            S4RFactory.setUsername(S4RUser);
            S4RFactory.setPassword(S4RPassword);
            S4RFactory.setVirtualHost(S4RVirtualHost);
            if(AstraInUse) {
                S4RFactory.useSslProtocol();    
            }
            connection = S4RFactory.newConnection();
            channel = connection.createChannel();
            channel.confirmSelect();
            channel.queueDeclare(S4RQueueName, true, false, false, null);
            int msgSent = 0;
            while (numMsg > msgSent) {
                String message = S4RMessage; 
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
    private void processRabbitMQConfFile() {
        PulsarConnCfgConf connCfgConf = null;
        connCfgConf = new PulsarConnCfgConf(rabbitmqConnfFile);
        connCfgConf.getClientConfMap();
        Map<String, String> clientConnMap = connCfgConf.getClientConfMap();

        String port = clientConnMap.get("port");
        S4RPort = Integer.parseInt(port);  
        
        S4RRabbitMQHost = clientConnMap.get("host");
        S4RPassword = clientConnMap.get("password");
        S4RUser = clientConnMap.get("username");
        if(S4RUser == null) {
            S4RUser = ""; // null will cause connection errors, set to blank ""
        }
        S4RVirtualHost = clientConnMap.get("virtual_host");
    }

}
