package com.example.pulsar;

import com.example.util.CommonUtil;
import org.apache.commons.lang3.RandomUtils;
import org.apache.pulsar.client.api.Producer;
import org.apache.pulsar.client.api.PulsarClient;
import org.apache.pulsar.client.api.PulsarClientException;
import org.apache.pulsar.client.api.TypedMessageBuilder;

import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class PulsarProducer {

    private String topicName;
    private Producer<byte[]> producer;
    private String clientIdentifier;

    public PulsarProducer(PulsarClient pulsarClient, String topicName) throws PulsarClientException {
        this.topicName = topicName;
        this.clientIdentifier = "[P]" + CommonUtil.randomClientIdentifier();

        producer = pulsarClient.newProducer()
                .topic(this.topicName)
                .create();
    }

    public void sendMessageSync(int msgNum) throws PulsarClientException {

        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
        DateFormat dateFormat2 = new SimpleDateFormat("yyyyMMdd-hhmmss-SSS");

        Date now = Calendar.getInstance().getTime();
        System.out.println(
                dateFormat.format(now) + "\n" +
                        "---------------------------------------------------------\n" +
                        "Producer    : " + clientIdentifier + "\n" +
                        "Topic       : " + producer.getTopic() + "\n" +
                        "Number of messages to publish: " + msgNum + "\n" +
                        "---------------------------------------------------------");

        for (int i=0; i<msgNum; i++) {
            CommonUtil.pause(50);

            now = Calendar.getInstance().getTime();
            String msgKey = "[" + i + "] " + dateFormat2.format(now);

            int payloadStrLen = RandomUtils.nextInt(
                    CommonUtil.MIN_RNDM_STRING_LENGTH, CommonUtil.MAX_RNDM_STRING_LENGTH);
            String msgPayload = CommonUtil.randomString(payloadStrLen);

            TypedMessageBuilder<byte[]> messageBuilder = producer.newMessage();
            System.out.println(
                    "  message published: msg-key=" + msgKey + ", msg-payload=" + msgPayload);

            messageBuilder
                    .key(msgKey)
                    .value(msgPayload.getBytes(StandardCharsets.UTF_8))
                    .send();
        }
    }

    public void close() throws PulsarClientException {
        if (producer != null) producer.close();
    }
}
