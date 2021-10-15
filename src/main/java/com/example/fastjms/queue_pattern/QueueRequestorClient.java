package com.example.fastjms.queue_pattern;

import org.apache.commons.lang3.RandomUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jms.*;

public class QueueRequestorClient extends QueuePatternExample {

    static final Logger LOGGER = LogManager.getLogger(QueueRequestorClient.class);

    final Queue replyQueue;

    public QueueRequestorClient(QueueConnection queueConnection, Queue queue) throws JMSException {
        super(queueConnection, queue);
        replyQueue = queueSession.createQueue("persistent://public/default/qpatn_requestor_reply");
    }

    @Override
    void demo() throws JMSException {
        if (queueSession != null) {
            queueConnection.start();
            QueueRequestor queueRequestor = new QueueRequestor(queueSession, queue);

            LOGGER.info("QueueRequestor:: sending 10 messages from queue: {}", queue.getQueueName());
            for (int i = 0; i < 10; i++) {
                Message message = queueSession.createTextMessage(String.valueOf(RandomUtils.nextInt(0, 10)));

                Message response = queueRequestor.request(message);
                LOGGER.info ("  > sent message: {{}}, response message: {{}}",
                        message.getBody(String.class),
                        response.getBody(String.class));
            }

            queueRequestor.close();
            queueConnection.stop();
        }
    }
}