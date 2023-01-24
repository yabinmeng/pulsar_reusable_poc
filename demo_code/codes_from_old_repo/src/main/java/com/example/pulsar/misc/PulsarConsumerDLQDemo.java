package com.example.pulsar.misc;

import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.client.impl.MultiplierRedeliveryBackoff;

import java.util.concurrent.TimeUnit;

public class PulsarConsumerDLQDemo {

    private static final String topicName = "persistent://public/default/mys4jtest_t";
    private static final String brokerServiceUrl = "pulsar://localhost:6650";
    private static final String jwtTokenFile = "/path/to/jwt/token/file";
    private static final boolean jwtTokenAuth = true;
    private static final boolean useNegAckSimu = false;

    public static void main(String[] args)  {
        try {
            ClientBuilder clientBuilder = PulsarClient.builder();
            clientBuilder.serviceUrl(brokerServiceUrl);

            // Only for JWT token auth or No auth at all
            if (jwtTokenAuth) {
                clientBuilder.authentication(
                        "org.apache.pulsar.client.impl.auth.AuthenticationToken",
                        StringUtils.join("file://", jwtTokenFile));
            }

            PulsarClient pulsarClient = clientBuilder.build();

            RedeliveryBackoff ackTimeoutRedeliveryBackoff = MultiplierRedeliveryBackoff.builder()
                    .minDelayMs(10)
                    .maxDelayMs(20)
                    .multiplier(1.2)
                    .build();
            RedeliveryBackoff negAckRedeliveryBackoff = MultiplierRedeliveryBackoff.builder()
                    .minDelayMs(10)
                    .maxDelayMs(20)
                    .multiplier(1.2)
                    .build();
            DeadLetterPolicy dlqPolicy = DeadLetterPolicy.builder()
                    .maxRedeliverCount(5)
                    .build();

            // Enable DLQ policy with
            // - ackTimeout : 10 seconds
            // - AckTimeoutRedeliveryBackoff: minDelayMs(10), maxDelayMs(20), multiplier(1.2)
            // - NegAckRedeliveryBackoff: minDelayMs(10), maxDelayMs(20), multiplier(1.2)
            // - DLQ policy :  maxRedeliverCount(5)
            Consumer<byte[]> consumer = pulsarClient.newConsumer()
                    .topic(topicName)
                    .receiverQueueSize(0)
                    .subscriptionName("myDlqSub")
                    .subscriptionInitialPosition(SubscriptionInitialPosition.Earliest)
                    .subscriptionType(SubscriptionType.Shared)
                    .ackTimeout(1, TimeUnit.SECONDS)
                    .ackTimeoutRedeliveryBackoff(ackTimeoutRedeliveryBackoff)
                    .negativeAckRedeliveryBackoff(negAckRedeliveryBackoff)
                    .deadLetterPolicy(dlqPolicy)
                    .subscribe();

            // Receive 10 messages without acknowledging it
            for (int i = 0; i < 10; i++) {
                Message<?> message = consumer.receive();
                System.out.println("===> received message " + message.getMessageId());

                if (useNegAckSimu) {
                    consumer.negativeAcknowledge(message);
                    System.out.println("     negAck this message. ");
                }
                else {
                    System.out.println("     not acknowledging this message, relying on ack timeout. ");
                }

                // This is needed in order to make sure that the internal caching time period
                Thread.sleep(5000);
            }

            // Keep the consumer connected for 2 minute before closing it
            Thread.sleep(120000);

            consumer.close();
            pulsarClient.close();
        }
        catch (PulsarClientException pce) {
            pce.printStackTrace();
        }
        catch (InterruptedException ie) {
            ie.printStackTrace();
        }

        System.exit(0);
    }
}
