package com.example.s4j.queue_pattern;

import javax.jms.*;
import java.util.Objects;

public abstract class QueuePatternExample {

    final QueueConnection queueConnection;
    final QueueSession queueSession;
    final Queue queue;
    String msgSelectorStr;

    public QueuePatternExample(QueueConnection queueConnection, Queue queue) throws JMSException {
        assert(queueConnection != null);
        this.queueConnection = queueConnection;
        this.queueSession = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        this.queue = queue;
    }

    public QueuePatternExample(QueueConnection queueConnection, Queue queue, String msgSelectorStr) throws JMSException {
        this(queueConnection, queue);
        this.msgSelectorStr = Objects.requireNonNullElse(msgSelectorStr, "");
    }

    abstract void demo() throws JMSException;
}
