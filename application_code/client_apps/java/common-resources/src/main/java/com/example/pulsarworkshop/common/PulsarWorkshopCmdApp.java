package com.example.pulsarworkshop.common;

import org.apache.commons.cli.*;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.pulsar.client.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.example.pulsarworkshop.common.exception.HelpExitException;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.exception.WorkshopRuntimException;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Map;

abstract public class PulsarWorkshopCmdApp {

    private final static Logger logger = LoggerFactory.getLogger(PulsarWorkshopCmdApp.class);

    protected String[] rawCmdInputParams;
    protected String pulsarTopicName;
    protected File clientConnfFile;
    protected File clientConfigFile;
    protected boolean useAstraStreaming;

    protected DefaultParser cmdParser;
    protected Options basicCliOptions = new Options();
    protected Options extraCliOptions = new Options();
    protected Options cliOptions = new Options();
    
    public abstract void processInputParams() throws InvalidParamException;
    public abstract void runApp();
    public abstract void termApp();


    public PulsarWorkshopCmdApp(String[] inputParams) {
        this.rawCmdInputParams = inputParams;
        this.cmdParser = new DefaultParser();

        addCommandLineOption(new Option("h", "help", false, "Displays the usage method."));
        addCommandLineOption(new Option("top", "topic", true, "Pulsar topic name."));
        addCommandLineOption(new Option("num","numMsg", true, "Number of message."));
        addCommandLineOption(new Option("con","connFile", true, "\"client.conf\" file path."));
        addCommandLineOption(new Option("cfg", "cfgFile", true, "Extra config properties file path."));
        addCommandLineOption(new Option("as", "astra", false, "Whether to use Astra streaming."));

    }

    protected void addCommandLineOption(Option option) {
    	cliOptions.addOption(option);
    }
    
    public int run(String appName) {
        int exitCode = 0;
        try {
            this.processInputParams();
            this.runApp();
        }
        catch (HelpExitException hee) {
            this.usage(appName);
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
            this.termApp();
        }
        
        return exitCode;
    }
    
    public String getPulsarTopicName() { return this.pulsarTopicName; }
    public File getClientConnfFile() { return this.clientConnfFile; }
    public File getClientConfigFile() { return this.clientConfigFile; }

    public void processBasicInputParams(CommandLine cmdLine) throws HelpExitException, InvalidParamException {
        // CLI option for help messages
        if (cmdLine.hasOption("help")) {
            throw new HelpExitException();
        }

        // (Required) CLI option for Pulsar topic
        pulsarTopicName = processStringInputParam(cmdLine, "top");

        // (Optional) CLI option for client.conf file
        clientConnfFile = processFileInputParam(cmdLine, "con");

        // (Optional) CLI option for extra config properties file
        clientConfigFile = processFileInputParam(cmdLine, "cfg");

        		// (Optional) Whether to use Astra Streaming
        if (cmdLine.hasOption("as")) {
            useAstraStreaming = true;
        }
    }

    public Integer processIntegerInputParam(CommandLine cmdLine, String optionName) {
        Option option = cliOptions.getOption(optionName);
        int intVal = 0;

        if (option.isRequired() &&
        	cmdLine.getOptionValue(option) == null) {
                throw new InvalidParamException("Empty value for argument '" + optionName +"'");
        }
        else if (cmdLine.getOptionValue(option) != null) {    	

        	String msgNumParam = cmdLine.getOptionValue(option.getOpt());        	
        	intVal = NumberUtils.toInt(msgNumParam);
        }
        
        return Integer.valueOf(intVal);
    }
    
    public String processStringInputParam(CommandLine cmdLine, String optionName) {

    	Option option = cliOptions.getOption(optionName);
        String value = cmdLine.getOptionValue(option);

        if (option.isRequired() &&
        	cmdLine.getOptionValue(option) == null) {
            throw new InvalidParamException("Empty value for argument '" + optionName +"'");
        }

        return value;
    }
    
    public File processFileInputParam(CommandLine cmdLine, String optionName) {
        File file = null;
        Option option = cliOptions.getOption(optionName);
        if (cmdLine.getOptionValue(option) != null) {

        	String path = cmdLine.getOptionValue(option.getOpt());    	
	        try {
	            file = new File(path);
	            file.getCanonicalPath();
	        } catch (IOException ex) {
	        	throw new InvalidParamException("Invalid file path for param '" + optionName + "': " + path);
	        }
    	}

        return file;
    }    
    
    public void usage(String appNme) {
        PrintWriter printWriter = new PrintWriter(System.out, true);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(printWriter, 150, "appNme",
                "Command Line Options:",
                cliOptions, 2, 1, "", true);

        System.out.println();
    }

    private PulsarConnCfgConf getPulsarConnCfgConf() {
        PulsarConnCfgConf connCfgConf = null;
        if (clientConnfFile != null) {
            connCfgConf = new PulsarConnCfgConf(clientConnfFile);
        }
        if (connCfgConf == null) {
            throw new WorkshopRuntimException(
                    "Can't properly read the Pulsar connection information from the \"client.conf\" file!");
        }
        return connCfgConf;
    }

    private PulsarExtraCfgConf getPulsarExtraCfgConf() {
        PulsarExtraCfgConf extraCfgConf = new PulsarExtraCfgConf(clientConfigFile);
        return extraCfgConf;
    }


    protected PulsarClient createNativePulsarClient()
    throws PulsarClientException {
        ClientBuilder clientBuilder = PulsarClient.builder();

        PulsarConnCfgConf connCfgConf = getPulsarConnCfgConf();
        Map<String, String> clientConnMap = connCfgConf.getClientConfMap();

        String pulsarSvcUrl = clientConnMap.get("brokerServiceUrl");
        clientBuilder.serviceUrl(pulsarSvcUrl);

        String authPluginClassName = clientConnMap.get("authPlugin");
        String authParams = clientConnMap.get("authParams");
        if ( !StringUtils.isAnyBlank(authPluginClassName, authParams) ) {
            clientBuilder.authentication(authPluginClassName, authParams);
        }

        // For Astra streaming, there is no need for this section.
        // But for Luna streaming, they're required if TLS is expected.
        if ( !useAstraStreaming && StringUtils.contains(pulsarSvcUrl, "pulsar+ssl") ) {
            boolean tlsHostnameVerificationEnable = BooleanUtils.toBoolean(
                    clientConnMap.get("tlsEnableHostnameVerification"));
            clientBuilder.enableTlsHostnameVerification(tlsHostnameVerificationEnable);

            String tlsTrustCertsFilePath =
                    clientConnMap.get("tlsTrustCertsFilePath");
            if (!StringUtils.isBlank(tlsTrustCertsFilePath)) {
                clientBuilder.tlsTrustCertsFilePath(tlsTrustCertsFilePath);
            }

            boolean tlsAllowInsecureConnection = BooleanUtils.toBoolean(
                    clientConnMap.get("tlsAllowInsecureConnection"));
            clientBuilder.allowTlsInsecureConnection(tlsAllowInsecureConnection);
        }

        return clientBuilder.build();
    }

    protected Producer createPulsarProducer(String topicName,
                                            PulsarClient pulsarClient)
    throws PulsarClientException {
        ProducerBuilder producerBuilder = pulsarClient.newProducer();
        PulsarExtraCfgConf pulsarExtraCfgConf = getPulsarExtraCfgConf();

        if (pulsarExtraCfgConf != null) {
            Map producerConfMap = new HashMap();
            producerConfMap.putAll(pulsarExtraCfgConf.getProducerConfMapTgt());

            // Remove the following producer conf parameters since they'll be
            // handled explicitly outside "loadConf()"
            producerConfMap.remove("topicName");

            producerBuilder.loadConf(producerConfMap);
        }

        producerBuilder.topic(topicName);

        return producerBuilder.create();
    }

    public Consumer<?> createPulsarConsumer(String topicName,
                                            PulsarClient pulsarClient,
                                            String consumerSubscriptionName,
                                            SubscriptionType consumerSubscriptionType)
            throws PulsarClientException
    {
        ConsumerBuilder<?> consumerBuilder = pulsarClient.newConsumer();
        PulsarExtraCfgConf pulsarExtraCfgConf = getPulsarExtraCfgConf();

        Map consumerConfMap = new HashMap();
        if (pulsarExtraCfgConf != null) {
            consumerConfMap.putAll(pulsarExtraCfgConf.getConsumerConfMapTgt());

            // Remove the following consumer conf parameters since they'll be
            // handled explicitly outside "loadConf()"
            consumerConfMap.remove("topicNames");
            consumerConfMap.remove("topicsPattern");
            consumerConfMap.remove("subscriptionName");
            consumerConfMap.remove("subscriptionType");

            // TODO: It looks like loadConf() method can't handle the following settings properly.
            //       Do these settings manually for now
            //       - deadLetterPolicy
            //       - negativeAckRedeliveryBackoff
            //       - ackTimeoutRedeliveryBackoff
            consumerConfMap.remove("deadLetterPolicy");
            consumerConfMap.remove("negativeAckRedeliveryBackoff");
            consumerConfMap.remove("ackTimeoutRedeliveryBackoff");

            consumerBuilder.loadConf(consumerConfMap);
        }

        consumerBuilder.topic(topicName);
        consumerBuilder.subscriptionName(consumerSubscriptionName);
        consumerBuilder.subscriptionType(consumerSubscriptionType);

        if (consumerConfMap.containsKey("deadLetterPolicy")) {
            consumerBuilder.deadLetterPolicy(
                    (DeadLetterPolicy) consumerConfMap.get("deadLetterPolicy"));
        }
        if (consumerConfMap.containsKey("negativeAckRedeliveryBackoff")) {
            consumerBuilder.negativeAckRedeliveryBackoff(
                    (RedeliveryBackoff) consumerConfMap.get("negativeAckRedeliveryBackoff"));
        }
        if (consumerConfMap.containsKey("ackTimeoutRedeliveryBackoff")) {
            consumerBuilder.ackTimeoutRedeliveryBackoff(
                    (RedeliveryBackoff) consumerConfMap.get("ackTimeoutRedeliveryBackoff"));
        }

        return consumerBuilder.subscribe();
    }
}
