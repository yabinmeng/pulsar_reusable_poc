package com.example.pulsarworkshop.common;

import com.example.pulsarworkshop.common.exception.CliOptProcRuntimeException;
import com.example.pulsarworkshop.common.exception.HelpExitException;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.utils.PulsarClientCLIAppUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;

abstract public class PulsarWorkshopApp {
    protected DefaultParser cmdParser = new DefaultParser();
    protected CommandLine cmdLine = null;

    protected String pulsarTopic;

    protected File clientConfFile;

    public PulsarWorkshopApp(String[] inputParams) {
        try {
            cmdLine = cmdParser.parse(PulsarClientCLIAppUtil.cliOptions, inputParams);
        } catch (ParseException e) {
            throw new InvalidParamException("Failed to parse the command line input parameters!");
        }
    }

    public void setPulsarTopic(String topic) { this.pulsarTopic = topic; }
    public String getPulsarTopic() { return this.pulsarTopic; }

    public void setClientConfFile(String clientConfFilePath) throws InvalidParamException {
        try {
            this.clientConfFile = new File(clientConfFilePath);
            this.clientConfFile.getCanonicalPath();
        }
        catch(IOException ioe) {
            throw new InvalidParamException("Can't read the specified client.conf file!");
        }
    }
    public File getClientConfFile() { return this.clientConfFile; }

    public abstract void processInputParams(String[] inputParams) throws
            HelpExitException, InvalidParamException;
    public abstract void runApp();
    public abstract void termApp();
    public abstract void usage();
}
