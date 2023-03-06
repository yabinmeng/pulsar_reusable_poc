package com.example.pulsarworkshop;

import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;
import com.example.pulsarworkshop.common.utils.CommonUtils;
import com.example.pulsarworkshop.common.utils.CsvFileLineScanner;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PulsarProducerFullCfg extends PulsarWorkshopCmdApp {

    private final static Logger logger = LoggerFactory.getLogger(PulsarProducerFullCfg.class);

    private File srcWrkldFile;

    // Default to publish 20 messages
    // -1 means to read all data from the source workload file and publish as messages
    private Integer numMsg = 20;

    private PulsarClient pulsarClient;
    private Producer pulsarProducer;

    public PulsarProducerFullCfg(String[] inputParams) {
        super(inputParams);

        addCommandLineOption(new Option("num","numMsg", true, "Number of message to produce."));
        addCommandLineOption(new Option("wrk","srcWrkldFile", true, "Data source workload file."));
    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new PulsarProducerFullCfg(args);

        int exitCode = workshopApp.run("PulsarProducerFullCfg");

        System.exit(exitCode);
    }

    @Override
    public void processInputParams() throws InvalidParamException {
        CommandLine commandLine = null;

        try {
            commandLine = cmdParser.parse(cliOptions, rawCmdInputParams);
        } catch (ParseException e) {
            throw new InvalidParamException("Failed to parse application CLI input parameters: " + e.getMessage());
        }

        super.processBasicInputParams(commandLine);

        // (Required) CLI option for number of messages
        numMsg = processIntegerInputParam(commandLine, "num");

        // (Required) CLI option for data source workload file
        srcWrkldFile = processFileInputParam(commandLine, "wrk");
        
    }

    @Override
    public void runApp() throws WorkshopRuntimException {
        try {
            pulsarClient = createNativePulsarClient();
            pulsarProducer = createPulsarProducer(pulsarTopicName, pulsarClient);

            // TODO: right now the message is sent as byte[].
            //       add support for more complex types likes 'avro' or 'json' in the future.
            assert (srcWrkldFile != null);

            CsvFileLineScanner csvFileLineScanner = new CsvFileLineScanner(srcWrkldFile);
            TypedMessageBuilder messageBuilder = pulsarProducer.newMessage();

            boolean isTitleLine = true;
            String titleLine = "";
            int msgSent = 0;
            if (numMsg == -1) {
                numMsg = Integer.MAX_VALUE;
            }

            while (csvFileLineScanner.hasNextLine()) {
                String csvLine = csvFileLineScanner.getNextLine();
                // Skip the first line which is a title line
                if (!isTitleLine) {
                    String msgPayload = CommonUtils.getJsonStrForCsv(titleLine, csvLine);

                    if (msgSent < numMsg) {
                        MessageId messageId = messageBuilder
                                .value(msgPayload.getBytes(StandardCharsets.UTF_8))
                                .send();
                        if (logger.isDebugEnabled()) {
                            logger.debug("Published a message: {}", messageId);
                        }

                        msgSent++;
                    } else {
                        break;
                    }
                } else {
                    isTitleLine = false;
                    titleLine = csvLine;
                }
            }

        } catch (PulsarClientException pce) {
            throw new WorkshopRuntimException("Unexpected error when producing Pulsar messages!");
        } catch (IOException ioException) {
            throw new WorkshopRuntimException("Failed to read from the workload data source file!");
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
