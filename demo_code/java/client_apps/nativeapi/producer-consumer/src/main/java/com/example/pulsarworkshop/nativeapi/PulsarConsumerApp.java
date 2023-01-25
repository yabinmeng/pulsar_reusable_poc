package com.example.pulsarworkshop.nativeapi;

import com.example.pulsarworkshop.common.PulsarClientConf;
import com.example.pulsarworkshop.common.exception.CliOptProcRuntimeException;
import com.example.pulsarworkshop.common.exception.HelpExitException;
import com.example.pulsarworkshop.common.exception.InvalidCfgParamException;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class PulsarConsumerApp extends  PulsarClientCLIApp {

    private static Logger logger = LoggerFactory.getLogger(PulsarConsumerApp.class);

    public PulsarConsumerApp() {
        super(false);
    }

    public static void main(String[] args) {

        PulsarConsumerApp consumerCmdApp = new PulsarConsumerApp();

        try {
            consumerCmdApp.processInputParameters(args);

            PulsarClientConf pulsarClientConf =
                    new PulsarClientConf(consumerCmdApp.getConfigurartionFile());

            PulsarClient pulsarClient = consumerCmdApp.createPulsarClient(pulsarClientConf);

            Consumer<?> consumer = consumerCmdApp.createPulsarConsumer(pulsarClient, pulsarClientConf);

            // TODO: right now the message is sent as byte[].
            //       add support for more complex types likes 'avro' or 'json' in the future.
            boolean simpleMsgFormat =
                    BooleanUtils.toBoolean(pulsarClientConf.getSchemaConfValueRaw("simple_message"));
            if (!simpleMsgFormat) {
                throw new RuntimeException("Complex schema type is not supported yet!");
            }

            int msgRecvd = 0;
            int totalMsgToRecv = consumerCmdApp.getNumMessage();
            if (totalMsgToRecv == -1) {
                totalMsgToRecv = Integer.MAX_VALUE;
            }

            List<CompletableFuture<? extends Message<?>>> msgRecvFutureList = new ArrayList<>();

            while (msgRecvd < totalMsgToRecv) {
                CompletableFuture<? extends Message<?>> msgRecvFuture = consumer.receiveAsync();
                msgRecvFuture.thenAccept( message -> {
                    try {
                        consumer.acknowledge(message);

                        if (logger.isDebugEnabled()) {
                            logger.debug("({}) Message received and acknowledged: " +
                                            "msg-key={}; msg-properties={}; msg-payload={}",
                                    consumer.getConsumerName(),
                                    message.getKey(),
                                    message.getProperties(),
                                    new String(message.getData()));
                        }
                    }
                    catch (PulsarClientException pulsarClientException) {
                        throw new RuntimeException(pulsarClientException);
                    }
                })
                .exceptionally( e -> {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Failed to receive a message: {}", e);
                    }
                    return null;
                });;

                msgRecvFutureList.add(msgRecvFuture);
                msgRecvd++;
            }

            msgRecvFutureList.forEach(CompletableFuture::join);

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
