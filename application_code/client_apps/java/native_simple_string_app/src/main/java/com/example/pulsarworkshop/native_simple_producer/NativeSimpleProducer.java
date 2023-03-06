package com.example.pulsarworkshop.native_simple_producer;

import com.example.pulsarworkshop.common.PulsarConnCfgConf;
import com.example.pulsarworkshop.common.PulsarExtraCfgConf;
import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.HelpExitException;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;
import com.example.pulsarworkshop.common.utils.CommonUtils;
import com.example.pulsarworkshop.common.utils.CsvFileLineScanner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.apache.pulsar.client.api.*;

public class NativeSimpleProducer extends PulsarWorkshopCmdApp {
    private final static Logger logger = LoggerFactory.getLogger(NativeSimpleProducer.class);

    private PulsarClient pulsarClient;
    private Producer pulsarProducer;
    public NativeSimpleProducer(String[] inputParams) {
        super(inputParams);
    }

    public static void main(String[] args) throws PulsarClientException {
        PulsarWorkshopCmdApp workshopApp = new NativeSimpleProducer(args);
        workshopApp.processInputParams();
        workshopApp.runApp();
    }

    @Override
    public void runApp() throws WorkshopRuntimException {
        try {
            setupProducer();
            // Publish a message to the topic
            String message = "Hello, Pulsar!";
            pulsarProducer.send(message);

            // Close the producer and the Pulsar client when done
            pulsarProducer.close();
            pulsarClient.close();

        } catch (PulsarClientException pce) {
            throw new WorkshopRuntimException("Unexpected error when producing Pulsar messages!");
        }
    }
    @Override
    public void processInputParams() throws InvalidParamException {
        CommandLine commandLine = null;

        try {
            commandLine = cmdParser.parse(getCliOptions(), rawCmdInputParams);
        } catch (ParseException e) {
            throw new InvalidParamException("Failed to parse application CLI input parameters!");
        }

        super.processBasicInputParams(commandLine);
    }
    public void setupProducer() {
        try {
            try {
                pulsarClient = createNativePulsarClient();
            } catch (PulsarClientException e) {
                throw new RuntimeException(e);
            }
            pulsarProducer = createPulsarProducer(pulsarTopicName, pulsarClient);
        } catch (PulsarClientException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void termApp() {
        try {
            if (pulsarProducer != null) {
                pulsarProducer.close();
            }

            if (pulsarClient != null) {
                pulsarClient.close();
            }
        }
        catch (PulsarClientException pce) {
            throw new WorkshopRuntimException("Failed to terminate Pulsar producer or client!");
        }
    }
}
