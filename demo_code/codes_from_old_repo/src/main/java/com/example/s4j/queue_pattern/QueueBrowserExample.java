package com.example.s4j.queue_pattern;

import com.example.util.JmsDemoUtil;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jms.*;
import java.util.Enumeration;

public class QueueBrowserExample extends QueuePatternExample {

    static final Logger LOGGER = LogManager.getLogger(QueueBrowserExample.class);

    public QueueBrowserExample(QueueConnection queueConnection, Queue queue, String msgSelector) throws JMSException {
        super(queueConnection, queue, msgSelector);
    }

    @Override
    void demo() throws JMSException {
        if (queueSession != null) {
            queueConnection.start();

            QueueBrowser queueBrowser;
            if (StringUtils.isBlank(msgSelectorStr))
                queueBrowser = queueSession.createBrowser(queue);
            else
                queueBrowser = queueSession.createBrowser(queue, msgSelectorStr);

            int totalMsg = 0;
            Enumeration enumeration= queueBrowser.getEnumeration();

            LOGGER.info("QueueReceiver:: " +
                    "browsing messages from queue: " + queue.getQueueName() + " " +
                    "[message selector: \"" + msgSelectorStr + "\"]");

            if (enumeration.hasMoreElements()) {
                while (enumeration.hasMoreElements()) {
                    Message message = (TextMessage) enumeration.nextElement();

                    if (message != null) {
                        totalMsg++;
                        int msg_seqid = message.getIntProperty(JmsDemoUtil.JMS_MSG_PROPERTY.SEQUENCE_ID.label);
                        long curTime = message.getJMSTimestamp();
                        LOGGER.info("  - browsed message: properties { " +
                                JmsDemoUtil.JMS_MSG_PROPERTY.SEQUENCE_ID.label + ":" + msg_seqid + ", " +
                                JmsDemoUtil.JMS_MSG_PROPERTY.JMS_TIME.label + ":" + curTime +
                                "}; payload { " + message.getBody(String.class) + " }");
                    }
                }
            }
            LOGGER.info("QueueBrowser:: total "  + totalMsg + " messages browsed!");

            queueBrowser.close();
            queueConnection.stop();
        }
    }
}