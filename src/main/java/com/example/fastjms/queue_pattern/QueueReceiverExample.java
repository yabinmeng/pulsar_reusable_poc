package com.example.fastjms.queue_pattern;

import com.example.util.JmsDemoUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jms.*;

public class QueueReceiverExample extends QueuePatternExample {

    static final Logger LOGGER = LogManager.getLogger(QueueReceiverExample.class);

    public QueueReceiverExample(QueueConnection queueConnection, Queue queue, String msgSelector) throws JMSException  {
        super(queueConnection, queue, msgSelector);
    }

    @Override
    void demo() throws JMSException  {
        if (queueSession != null) {
            queueConnection.start();

            QueueReceiver queueReceiver;
            if (StringUtils.isBlank(msgSelectorStr))
                queueReceiver = queueSession.createReceiver(queue);
            else
                queueReceiver = queueSession.createReceiver(queue, msgSelectorStr);

            int totalMsg = 0;
            LOGGER.info("QueueReceiver:: " +
                    "receiving 10 messages from queue: " + queue.getQueueName() + " " +
                    "[message selector: \"" + msgSelectorStr + "\"]");

            for (int i=0; i<10; i++) {
                Message message = queueReceiver.receive(1000);
                if (message != null) {
                    totalMsg++;

                    int msg_seqid = message.getIntProperty(JmsDemoUtil.JMS_MSG_PROPERTY.SEQUENCE_ID.label);
                    long curTime = message.getJMSTimestamp();

                    LOGGER.info("  > received message: properties { " +
                            JmsDemoUtil.JMS_MSG_PROPERTY.SEQUENCE_ID.label + ":" + msg_seqid + ", " +
                            JmsDemoUtil.JMS_MSG_PROPERTY.JMS_TIME.label + ":" + curTime +
                            "}; payload { " + message.getBody(String.class) + " }");
                }
            }
            LOGGER.info("QueueReceiver:: total "  + totalMsg + " messages received!");

            queueReceiver.close();
            queueConnection.stop();
        }
    }
}
