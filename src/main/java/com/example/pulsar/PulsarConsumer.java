package com.example.pulsar;

import com.example.util.CommonUtil;
import org.apache.pulsar.client.api.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class PulsarConsumer {

    private String topicName;
    private Consumer<byte[]> consumer;
    private String clientIdentifier;
    private String subscription;
    private SubscriptionType subscriptionType;

    private Map<String, List<String>> msgListByKey = new HashMap<>();

    public PulsarConsumer(PulsarClient pulsarClient,
                          String topicName,
                          String subNameStr,
                          String subTypeStr) throws PulsarClientException {
        assert pulsarClient != null;

        this.topicName = topicName;
        this.clientIdentifier = "[C]" + CommonUtil.randomClientIdentifier();
        this.subscription = subNameStr;
        subscriptionType = SubscriptionType.valueOf(subTypeStr);

        consumer = pulsarClient.newConsumer()
                .topic(this.topicName)
                .subscriptionName(this.subscription)
                .subscriptionType(this.subscriptionType)
                .subscribe();
    }

    public void recvMessageSync(int totalMsgNum) throws PulsarClientException {
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");

        Date now = Calendar.getInstance().getTime();
        System.out.println(
                dateFormat.format(now) + "\n" +
                        "---------------------------------------------------------\n" +
                        "Consumer    : " + this.clientIdentifier + "\n" +
                        "Topic       : " + this.topicName + "\n" +
                        "Subscription: " + this.subscription + "\n" +
                        "Subscription Type: " + this.subscriptionType.name() + "\n" +
                        "---------------------------------------------------------");

        int numMessagesConsumed = 0;

        while (totalMsgNum == 0 || numMessagesConsumed < totalMsgNum) {
            Message<?> message = consumer.receive();

            if (message != null) {
                numMessagesConsumed += 1;
                consumer.acknowledge(message);

                String msgKey = message.getKey();
                if (msgKey == null) msgKey = "N/A";
                String msgPayload = new String(message.getData());
                Date msgPubTime = new Date(message.getPublishTime());

                System.out.println(
                        "  message received: msg-key=" + msgKey +
                                ", msg-properties=" + message.getProperties() +
                                ", msg-payload=" + msgPayload +
                                ", publish-time: " + dateFormat.format(msgPubTime));

                List<String> msgPayLoadArrForKey = msgListByKey.getOrDefault(msgKey, new ArrayList<>());
                msgPayLoadArrForKey.add(msgPayload);
                msgListByKey.put(msgKey, msgPayLoadArrForKey);

                CommonUtil.pause(100);
            }
        }
    }

    void close() throws PulsarClientException {
        if (consumer != null) consumer.close();
    }

    int getTotalReceivedMsgKeyNum() {
        return msgListByKey.keySet().size();
    }

    int getTotalReceivedMsgNum() {
        int msgCnt = 0;

        for (String key : msgListByKey.keySet()) {
            msgCnt += msgListByKey.get(key).size();
        }

        return msgCnt;
    }

    void printReceivedMsgByKey() {
        for (String key : msgListByKey.keySet()) {
            List<String> msgPayloadArrForKey = msgListByKey.get(key);

            System.out.println("  [key: " + key + " (" + msgPayloadArrForKey.size() +  "messages)]");

            for (String payload : msgPayloadArrForKey) {
                System.out.println("        " + payload);
            }
        }
    }
}
