package com.example.pulsarworkshop.common;

import com.example.pulsarworkshop.common.exception.HelpExitException;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import org.apache.commons.cli.*;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

abstract public class PulsarWorkshopCmdApp {
    protected String[] rawCmdInputParams;
    protected String pulsarTopicName;
    protected File clientConnfFile;
    protected File extraCfgPropFile;

    protected DefaultParser cmdParser;
    protected Options basicCliOptions = new Options();
    protected Options extraCliOptions = new Options();

    public PulsarWorkshopCmdApp(String[] inputParams) {
        this.rawCmdInputParams = inputParams;

        // Basic Command line options
        basicCliOptions.addOption(new Option("help", false, "Displays the usage method."));
        basicCliOptions.addOption(new Option("topic", true, "Pulsar topic name."));
        basicCliOptions.addOption(new Option("connFile", true, "\"client.conf\" file path."));
        basicCliOptions.addOption(new Option("cfgFile", true, "Extra config properties file path."));
    }

    public String getPulsarTopicName() { return this.pulsarTopicName; }
    public File getClientConnfFile() { return this.clientConnfFile; }
    public File getExtraCfgPropFile() { return this.extraCfgPropFile; }

    public void processInputParams() throws
            HelpExitException, InvalidParamException {
        cmdParser = new DefaultParser();
        processBasicInputParams();
        processExtraInputParams();
    }
    public void processBasicInputParams() throws HelpExitException, InvalidParamException {
        CommandLine basicCmdLine = null;

        try {
            basicCmdLine = cmdParser.parse(basicCliOptions, rawCmdInputParams);
        } catch (ParseException e) {
            throw new InvalidParamException("Failed to parse basic CLI input parameters!");
        }

        // CLI option for help messages
        if (basicCmdLine.hasOption("help")) {
            throw new HelpExitException();
        }

        // (Required) CLI option for Pulsar topic
        pulsarTopicName = basicCmdLine.getOptionValue("topic");
        if (StringUtils.isBlank(pulsarTopicName)) {
            throw new InvalidParamException("Empty Pulsar topic name!");
        }

        // (Required) CLI option for client.conf file
        try {
            String clntConnFileParam = basicCmdLine.getOptionValue("connFile");
            clientConnfFile = new File(clntConnFileParam);
            clientConnfFile.getCanonicalPath();
        }
        catch (IOException ex) {
            throw new InvalidParamException("Invalid \"client.conf\" file path!");
        }

        // (Optional) CLI option for extra config properties file
        String cfgFileParam = basicCmdLine.getOptionValue("cfgFile");
        if (StringUtils.isNotBlank(cfgFileParam)) {
            try {
                clientConnfFile = new File(cfgFileParam);
                clientConnfFile.getCanonicalPath();
            } catch (IOException ex) {
                throw new InvalidParamException("Invalid configuration properties file path!");
            }
        }
    }

    public Options getCliOptions() {
        Options options = new Options();
        for (Option opt : basicCliOptions.getOptions()) {
            options.addOption(opt);
        }
        for (Option opt : extraCliOptions.getOptions()) {
            options.addOption(opt);
        }
        return options;
    }

    public void usage(String appNme) {

        PrintWriter printWriter = new PrintWriter(System.out, true);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(printWriter, 150, "appNme",
                "Command Line Options:",
                getCliOptions(), 2, 1, "", true);

        System.out.println();
    }

    public abstract void processExtraInputParams() throws InvalidParamException;
    public abstract void runApp();
    public abstract void termApp();
}
