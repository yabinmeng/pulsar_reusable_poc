package com.example.pulsarworkshop;

import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;

import org.apache.commons.cli.Option;
import org.apache.pulsar.client.api.*;
import org.apache.pulsar.shade.org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JmsQueueSender extends PulsarWorkshopCmdApp {

    private final static Logger logger = LoggerFactory.getLogger(JmsQueueSender.class);

    public JmsQueueSender(String[] inputParams) {
        super(inputParams);

    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new JmsQueueSender(args);

        int exitCode = workshopApp.run("JmsQueueSender");

        System.exit(exitCode);
    }

    @Override
    public void processInputParams() throws InvalidParamException {

    }

    @Override
    public void runApp() {

    }

    @Override
    public void termApp() {

    }
}
