package com.example.s4j.misc;

import com.datastax.oss.pulsar.jms.PulsarConnectionFactory;
import org.apache.commons.lang3.StringUtils;

import javax.jms.Destination;
import javax.jms.JMSConsumer;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import java.util.HashMap;
import java.util.Map;

public class JmsConsumerDLQDemo {

    private static final String topicName = "persistent://public/default/mys4jtest_t";
    private static final String brokerServiceUrl = "pulsar://localhost:6650";
    private static final String webServiceUrl = "http://localhost:8080";

    private static final String jwtTokenFile = "/path/to/jwt/token/file";
    private static final boolean jwtTokenAuth = true;

    public static void main(String[] args)  {
        try {
            Map<String, Object> dlqPolicy = new HashMap<>();
            dlqPolicy.put("maxRedeliverCount", 2);
            dlqPolicy.put("deadLetterTopic", StringUtils.join(topicName, "-myDlqTopic"));

            Map<String, Object> redeliveryBackoffPolicy = new HashMap<>();
            redeliveryBackoffPolicy.put("minDelayMs", 10);
            redeliveryBackoffPolicy.put("maxDelayMs", 20);
            redeliveryBackoffPolicy.put("multiplier", 1.5);

            Map<String, Object> pulsarConsumerConfMap = new HashMap<>();
            pulsarConsumerConfMap.put("ackTimeoutMillis", 1000);
            // Can't set the receiver queue size to be 0. Otherwise, it fails with the following error message:
            // "Can't use receive with timeout, if the queue size is 0"
            pulsarConsumerConfMap.put("receiverQueueSize", 1);
            //pulsarConsumerConfMap.put("subscriptionInitialPosition", SubscriptionInitialPosition.Earliest);
            pulsarConsumerConfMap.put("deadLetterPolicy", dlqPolicy);
            pulsarConsumerConfMap.put("ackTimeoutRedeliveryBackoff", redeliveryBackoffPolicy);
            pulsarConsumerConfMap.put("negativeAckRedeliveryBackoff", redeliveryBackoffPolicy);

            Map<String, Object> jmsConfMap = new HashMap<>();
            jmsConfMap.put("brokerServiceUrl", webServiceUrl);
            jmsConfMap.put("webServiceUrl", webServiceUrl);
            if (jwtTokenAuth) {
                jmsConfMap.put("authPlugin", "org.apache.pulsar.client.impl.auth.AuthenticationToken");
                jmsConfMap.put("authParams", StringUtils.join("file://", jwtTokenFile));
            }
            jmsConfMap.put("consumerConfig", pulsarConsumerConfMap);

            PulsarConnectionFactory factory = new PulsarConnectionFactory(jmsConfMap);

            JMSContext jmsContext = factory.createContext(JMSContext.CLIENT_ACKNOWLEDGE);
            jmsContext.start();

            Destination destination = jmsContext.createQueue(topicName);
            JMSConsumer jmsConsumer = jmsContext.createConsumer(destination);

            // Receive 10 messages without acknowledging it
            for (int i = 0; i < 10; i++) {
                javax.jms.Message message = jmsConsumer.receive();

                System.out.println("===> received message " + message.getJMSMessageID());
                System.out.println("     not acknowledging this message, relying on ack timeout. ");

                // This is needed in order to make sure that the internal caching time period
                Thread.sleep(5000);
            }

            // Keep the consumer connected for 2 minute before closing it
            Thread.sleep(120000);

            jmsContext.stop();
            jmsContext.close();
            factory.close();
        }
        catch (InterruptedException ie) {
            ie.printStackTrace();
        } catch (JMSException e) {
            throw new RuntimeException(e);
        }

        System.exit(0);
    }
}
