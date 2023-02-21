package com.example.pulsarworkshop.nat_prod;

import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.CliOptProcRuntimeException;
import com.example.pulsarworkshop.common.exception.HelpExitException;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import com.example.pulsarworkshop.common.utils.PulsarClientCLIAppUtil;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.math.NumberUtils;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

public class NatProdCmdApp extends PulsarWorkshopCmdApp {

    private File srcWrkldFile;

    // Default to publish 20 messages
    // -1 means to read all data from the source workload file and publish as messages
    private int numMsg = 20;

    public NatProdCmdApp(String[] inputParams) {
        super(inputParams);

        extraCliOptions.addOption(new Option("srcWrkldFile", false, "Data source workload file."));
        extraCliOptions.addOption(new Option("numMsg", true, "Number of message to produce."));
    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new NatProdCmdApp(args);

        try {
            workshopApp.processInputParams();
            workshopApp.runApp();

            System.exit(0);
        }
        catch (HelpExitException hee) {
            workshopApp.usage();
            System.exit(1);
        }
        catch (InvalidParamException ipe) {
            System.out.println("\n[ERROR] Invalid input value(s) detected ...");
            System.out.println("\n        Error message: " + ipe.getErrorDescription());
            System.out.println("---------------------------------------------");
            ipe.printStackTrace();
        }
        finally {
            workshopApp.termApp();
        }
    }

    @Override
    public void processExtraInputParams() throws InvalidParamException {
        CommandLine extraCmdLine = null;

        try {
            extraCmdLine = cmdParser.parse(extraCliOptions, rawCmdInputParams);
        } catch (ParseException e) {
            throw new InvalidParamException("Failed to parse extra CLI input parameters!");
        }

        // CLI option for data source workload file
        try {
            String srcWrkldFileParam = extraCmdLine.getOptionValue("srcWrkldFile");
            srcWrkldFile = new File(srcWrkldFileParam);
            srcWrkldFile.getCanonicalPath();
        }
        catch (IOException ex) {
            throw new InvalidParamException("Invalid source workload file path!");
        }

        // CLI option for number of messages
        String msgNumParam = extraCmdLine.getOptionValue("numMsg");
        int intVal = NumberUtils.toInt(msgNumParam);
        if ( (intVal > 0) || (intVal == -1) ) {
            numMsg = intVal;
        }
        else {
            throw new InvalidParamException("Message number must be a positive integer or -1 (all available raw input)!");
        }
    }

    @Override
    public void runApp() {

    }

    @Override
    public void termApp() {

    }

    @Override
    public void usage() {

        PrintWriter printWriter = new PrintWriter(System.out, true);

        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp(printWriter, 150, "NatProdCmdApp",
                "Command Line Options:",
                getCliOptions(), 2, 1, "", true);

        System.out.println();
    }
}
