package com.example.pulsarworkshop.jaeger;

import com.example.pulsarworkshop.common.PulsarClientCLIApp;
import com.example.pulsarworkshop.common.PulsarClientConf;
import com.example.pulsarworkshop.common.exception.CliOptProcRuntimeException;
import com.example.pulsarworkshop.common.exception.HelpExitException;
import com.example.pulsarworkshop.common.exception.InvalidCfgParamException;
import io.jaegertracing.Configuration;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.util.GlobalTracer;
import io.streamnative.pulsar.tracing.TracingPulsarUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.pulsar.client.api.*;

/////
// OpenTracing Tutorial
// - https://github.com/yurishkuro/opentracing-tutorial
// - OpenTelemtry is replacing Opentracing
//   (there is no ready-to-use library yet)

public class MsgProcWithTracingApp extends PulsarClientCLIApp {
    private Tracer tracer;

    private static final String tracingSvcName = "pulsarWorkshopTracing";

    public MsgProcWithTracingApp() {
        super(true);
    }

    public void initTracer(String service) {
        Configuration.SamplerConfiguration samplerConfig =
                Configuration.SamplerConfiguration.fromEnv().withType("const").withParam(1);
        Configuration.ReporterConfiguration reporterConfig =
                Configuration.ReporterConfiguration.fromEnv().withLogSpans(true);
        Configuration config = new Configuration(service).withSampler(samplerConfig).withReporter(reporterConfig);

        this.tracer = config.getTracer();
        GlobalTracer.registerIfAbsent(this.tracer);
    }

    public void closeTracer() {
        this.tracer.close();
    }

    public Tracer getTracer() {
        return this.tracer;
    }

    public static void main(String[] args) {

        // Create a producer APP
        MsgProcWithTracingApp msgTracingApp = new MsgProcWithTracingApp();
        msgTracingApp.initTracer(tracingSvcName);

        try {
            msgTracingApp.processInputParameters(args);

            PulsarClientConf pulsarClientConf =
                    new PulsarClientConf(msgTracingApp.getConfigurartionFile());
            PulsarClient pulsarClient = msgTracingApp.createPulsarClient(pulsarClientConf);

            Producer producer = msgTracingApp.createPulsarProducer(pulsarClient, pulsarClientConf, true);

            int msgSent = 0;
            int totalMsgToSend = msgTracingApp.getNumMessage();

            System.out.println("[Producer] Send " + totalMsgToSend + " simple messages with tracing enabled!");
            System.out.println("-----------------------------------");
            TypedMessageBuilder messageBuilder = producer.newMessage();
            while (msgSent < totalMsgToSend) {
                String msgPayload = RandomStringUtils.randomAlphanumeric(10);

                messageBuilder
                        .property("msg-seq", String.valueOf(msgSent))
                        .value(msgPayload.getBytes())
                        .send();

                msgSent++;
            }


            // Pause 1 second before receiving messages
            Thread.sleep(1000);


            Consumer<?> consumer = msgTracingApp.createPulsarConsumer(
                    pulsarClient,
                    pulsarClientConf,
                    producer.getTopic(),
                    "sub-" + tracingSvcName,
                    true);

            int msgRecvd = 0;
            int totalMsgToRecv = msgTracingApp.getNumMessage();

            System.out.println("\n\n[Consumer] Receive " + totalMsgToRecv + " simple messages with tracing enabled!");
            System.out.println("-----------------------------------");

            while (msgRecvd < totalMsgToRecv) {
                Message<?> message = consumer.receive();
                SpanContext spanContext = TracingPulsarUtils.extractSpanContext(message, msgTracingApp.getTracer());
                System.out.println("Message received and acknowledged: " +
                                "msg-key=" + message.getKey() +  "; " +
                                "msg-properties=" + message.getProperties() + "; " +
                                "msg-payload=" + new String(message.getData()) + "; " +
                                "spanContext_traceId=" + spanContext.toTraceId() );
                consumer.acknowledge(message);
                msgRecvd++;
            }


            // Pause 2 second before finishing up
            Thread.sleep(2000);

            consumer.close();
            producer.close();
            msgTracingApp.closeTracer();
            pulsarClient.close();

            System.exit(0);

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

    }
}
