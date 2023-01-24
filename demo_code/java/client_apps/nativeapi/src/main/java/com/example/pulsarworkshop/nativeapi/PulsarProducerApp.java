package com.example.pulsarworkshop.nativeapi;

import com.example.pulsarworkshop.common.CommonUtils;
import com.example.pulsarworkshop.common.exception.CliOptProcRuntimeException;
import com.example.pulsarworkshop.common.exception.HelpExitException;
import com.example.pulsarworkshop.common.exception.InvalidCfgParamException;
import com.example.pulsarworkshop.nativeapi.utils.PulsarClientCLIAppUtil;
import com.example.pulsarworkshop.common.CsvFileLineScanner;
import com.example.pulsarworkshop.common.PulsarClientConf;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PulsarProducerApp extends PulsarClientCLIApp {

    private static Logger logger = LoggerFactory.getLogger(PulsarProducerApp.class);

    public PulsarProducerApp() {
        super(true);
    }
    public static void main(String[] args) {

        PulsarProducerApp producerCmdApp = new PulsarProducerApp();

        try {
            producerCmdApp.processInputParameters(args);

            PulsarClientConf pulsarClientConf =
                    new PulsarClientConf(producerCmdApp.getConfigurartionFile());

            PulsarClient pulsarClient = producerCmdApp.createPulsarClient(pulsarClientConf);

            Producer producer = producerCmdApp.createPulsarProducer(pulsarClient, pulsarClientConf);

            // TODO: right now the message is sent as byte[].
            //       add support for more complex types likes 'avro' or 'json' in the future.
            boolean simpleMsgFormat =
                    BooleanUtils.toBoolean(pulsarClientConf.getSchemaConfValueRaw("simple_message"));
            if (!simpleMsgFormat) {
                throw new RuntimeException("Complex schema type is not supported yet!");
            }

            File workloadCsvFile = producerCmdApp.getRawWorkloadFile();
            if (workloadCsvFile != null) {
                CsvFileLineScanner csvFileLineScanner = new CsvFileLineScanner(workloadCsvFile);

                TypedMessageBuilder messageBuilder = producer.newMessage();

                boolean isTitleLine = true;
                String titleLine = "";
                int msgSent = 0;
                int totalMsgToSend = producerCmdApp.getNumMessage();
                if (totalMsgToSend == -1) {
                    totalMsgToSend = Integer.MAX_VALUE;
                }

                List<CompletableFuture<MessageId>> msgSendFutureList = new ArrayList<>();

                while (csvFileLineScanner.hasNextLine()) {
                    String csvLine = csvFileLineScanner.getNextLine();
                    // Skip the first line which is a title line
                    if (!isTitleLine) {
                        String msgPayload = CommonUtils.getJsonStrForCsv(titleLine, csvLine);

                        if (msgSent < totalMsgToSend) {
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
            }

        } catch (HelpExitException helpExitException) {
            usageAndExit(0);
        } catch (CliOptProcRuntimeException cliOptProcRuntimeException) {
            System.out.println("\n[ERROR] Invalid CLI parameter value(s) detected ...");
            System.out.println("-----------------------------------");
            cliOptProcRuntimeException.printStackTrace();
            System.exit(cliOptProcRuntimeException.getErrorExitCode());
        } catch (InvalidCfgParamException invalidCfgParamException) {
            System.out.println("\n[ERROR] Invalid workshop properties values detected ...");
            System.out.println("-----------------------------------");
            invalidCfgParamException.printStackTrace();
            System.exit(300);
        } catch (Exception e) {
            System.out.println("\n[ERROR] Unexpected error detected ...");
            System.out.println("-----------------------------------");
            e.printStackTrace();
            System.exit(900);
        }

        System.exit(0);
    }
}
