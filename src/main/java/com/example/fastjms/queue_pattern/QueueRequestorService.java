package com.example.fastjms.queue_pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.jms.*;

public class QueueRequestorService implements Runnable, MessageListener, ExceptionListener {

    static final Logger LOGGER = LogManager.getLogger(QueueRequestorService.class);

    private final QueueSession _session;
    private final QueueReceiver _queueReceiver;
    private QueueSender _queueSender;

    public QueueRequestorService(QueueConnection queueConnection, Queue queue) throws JMSException {
        _session = queueConnection.createQueueSession(false, Session.AUTO_ACKNOWLEDGE);
        _queueReceiver = _session.createReceiver(queue);
        _queueReceiver.setMessageListener(this);
        queueConnection.setExceptionListener(this);
        queueConnection.start();
    }

    public void close() throws  JMSException {
        if (_queueReceiver != null) _queueReceiver.close();
        if (_session != null) _session.close();
    }

    @Override
    public void onException(JMSException e) {
        LOGGER.trace("[QueueRequestorService] Unexpected error: " + e.getMessage() );
        e.printStackTrace();
    }

    @Override
    public void onMessage(Message message) {
        try {
            int msgPaylodInt = Integer.parseInt(message.getBody(String.class));
            TextMessage responseMsg =
                    _session.createTextMessage(String.valueOf(msgPaylodInt * 100));
            Queue replyQueue = (Queue) message.getJMSReplyTo();
            QueueSender queueSender = _session.createSender(replyQueue);
            queueSender.send(responseMsg);
        }
        catch (JMSException jmsException) {
            onException(jmsException);
        }
    }

    @Override
    public void run() {

    }
}
