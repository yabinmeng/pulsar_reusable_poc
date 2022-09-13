package com.example.s4j.queue_pattern;

import com.example.util.CommonUtil;
import com.example.util.JmsDemoUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jms.*;

public class QueueSenderExample extends QueuePatternExample {

    static final Logger LOGGER = LogManager.getLogger(QueueSenderExample.class);

    public QueueSenderExample(QueueConnection queueConnection, Queue queue) throws  JMSException {
        super(queueConnection, queue);
    }

    @Override
    void demo() throws JMSException {
        if (queueSession != null) {
            QueueSender queueSender = queueSession.createSender(queue);

            LOGGER.info("QueueSender:: sending 10 messages from queue: {}", queue.getQueueName());

            for (int i = 0; i < 10; i++) {
                Message message = queueSession.createTextMessage(CommonUtil.randomString(20));
                message.setIntProperty(JmsDemoUtil.JMS_MSG_PROPERTY.SEQUENCE_ID.label, i);
                long curTime = System.currentTimeMillis();
                message.setJMSTimestamp(curTime);
                queueSender.send(message);

                LOGGER.info("  > sent message: properties { " +
                        JmsDemoUtil.JMS_MSG_PROPERTY.SEQUENCE_ID.label + ":" + i + ", " +
                        JmsDemoUtil.JMS_MSG_PROPERTY.JMS_TIME.label + ":" + curTime +
                        " }; payload { " +  message.getBody(String.class) + " }");

                // Pause for 1 second
                try {
                    Thread.sleep(200);
                }
                catch (InterruptedException ite) {
                    ite.printStackTrace();
                }
            }

            queueSender.close();
        }
    }
}
