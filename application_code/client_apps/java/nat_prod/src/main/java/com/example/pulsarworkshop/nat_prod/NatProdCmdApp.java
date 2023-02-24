package com.example.pulsarworkshop.nat_prod;

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

public class NatProdCmdApp extends PulsarWorkshopCmdApp {

    private final static Logger logger = LoggerFactory.getLogger(NatProdCmdApp.class);

    private File srcWrkldFile;

    // Default to publish 20 messages
    // -1 means to read all data from the source workload file and publish as messages
    private int numMsg = 20;

    private PulsarClient pulsarClient;
    private Producer pulsarProducer;

    public NatProdCmdApp(String[] inputParams) {
        super(inputParams);

        extraCliOptions.addOption(new Option("num","numMsg", true, "Number of message to produce."));
        extraCliOptions.addOption(new Option("wrk","srcWrkldFile", true, "Data source workload file."));
    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new NatProdCmdApp(args);

        int exitCode = 0;
        try {
            workshopApp.processInputParams();
            workshopApp.runApp();
        }
        catch (HelpExitException hee) {
            workshopApp.usage("NatProdCmdApp");
            exitCode = 1;
        }
        catch (InvalidParamException ipe) {
            System.out.println("\n[ERROR] Invalid input value(s) detected!");
            ipe.printStackTrace();
            exitCode = 2;
        }
        catch (WorkshopRuntimException wre) {
            System.out.println("\n[ERROR] Unexpected runtime error detected!");
            wre.printStackTrace();
            exitCode = 3;
        }
        finally {
            workshopApp.termApp();
        }

        System.exit(exitCode);
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

        // (Required) CLI option for number of messages
        String msgNumParam = commandLine.getOptionValue("num");
        int intVal = NumberUtils.toInt(msgNumParam);
        if ( (intVal > 0) || (intVal == -1) ) {
            numMsg = intVal;
        }
        else {
            throw new InvalidParamException("Message number must be a positive integer or -1 (all available raw input)!");
        }

        // (Required) CLI option for data source workload file
        String srcWrkldFileParam = commandLine.getOptionValue("wrk");
        if (StringUtils.isBlank(srcWrkldFileParam)) {
            throw new InvalidParamException("Must specify the source workload file!");
        }
        else {
            try {
                srcWrkldFile = new File(srcWrkldFileParam);
                srcWrkldFile.getCanonicalPath();
            } catch (IOException ex) {
                throw new InvalidParamException("Invalid file path for the source workload file!");
            }
        }
    }

    @Override
    public void runApp() throws WorkshopRuntimException {

        PulsarExtraCfgConf extraCfgConf = null;
        if (extraCfgConf != null) {
            extraCfgConf = new PulsarExtraCfgConf(clientConfigFile);
        }

        try {
            pulsarClient = createNativePulsarClient();
            pulsarProducer = createPulsarProducer(pulsarTopicName, pulsarClient, extraCfgConf);

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

            List<CompletableFuture<MessageId>> msgSendFutureList = new ArrayList<>();

            while (csvFileLineScanner.hasNextLine()) {
                String csvLine = csvFileLineScanner.getNextLine();
                // Skip the first line which is a title line
                if (!isTitleLine) {
                    String msgPayload = CommonUtils.getJsonStrForCsv(titleLine, csvLine);

                    if (msgSent < numMsg) {
                        CompletableFuture<MessageId> msgSendFuture = messageBuilder
                                .value(msgPayload.getBytes(StandardCharsets.UTF_8))
                                .sendAsync()
                                .thenAccept( messageId -> {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Published a message: {}", messageId);
                                    }
                                })
                                .exceptionally( e -> {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Failed to publish a message: {}", e);
                                    }
                                    return null;
                                });


                        msgSendFutureList.add(msgSendFuture);
                        msgSent++;
                    } else {
                        break;
                    }
                } else {
                    isTitleLine = false;
                    titleLine = csvLine;
                }
            }

            msgSendFutureList.forEach(CompletableFuture::join);

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
