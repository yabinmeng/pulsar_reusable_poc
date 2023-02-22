package com.example.pulsarworkshop.nat_cons;

import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.HelpExitException;
import com.example.pulsarworkshop.common.exception.InvalidParamException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.pulsar.client.api.SubscriptionType;
import java.io.PrintWriter;

public class NatConsCmdApp extends PulsarWorkshopCmdApp {

    // Default to consume 20 messages
    // -1 means to consume all available messages (indefinitely)
    private int numMsg = 20;
    private String subsriptionName;
    private SubscriptionType subscriptionType = SubscriptionType.Exclusive;

    public NatConsCmdApp(String[] inputParams) {
        super(inputParams);

        extraCliOptions.addOption(new Option("numMsg", true, "Number of message to produce."));
        extraCliOptions.addOption(new Option("subType", false, "Pulsar subscription type."));
        extraCliOptions.addOption(new Option("subName", false, "Pulsar subscription name."));
    }

    public static void main(String[] args) {
        PulsarWorkshopCmdApp workshopApp = new NatConsCmdApp(args);

        try {
            workshopApp.processInputParams();
            workshopApp.runApp();

            System.exit(0);
        }
        catch (HelpExitException hee) {
            workshopApp.usage("NatConsCmdApp");
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

        // CLI option for number of messages
        String msgNumParam = extraCmdLine.getOptionValue("numMsg");
        int intVal = NumberUtils.toInt(msgNumParam);
        if ( (intVal > 0) || (intVal == -1) ) {
            numMsg = intVal;
        }
        else {
            throw new InvalidParamException("Message number must be a positive integer or -1 (all available raw input)!");
        }

        // Pulsar subscription name
        subsriptionName = extraCmdLine.getOptionValue("subName");
        if (StringUtils.isBlank(subsriptionName)) {
            throw new InvalidParamException("Empty subscription name!");
        }

        // Pulsar subscription type, default Exclusive
        String subTypeParam = extraCmdLine.getOptionValue("subType");
        if ( ! StringUtils.isBlank(subTypeParam) ) {
            try {
                subscriptionType = SubscriptionType.valueOf(subTypeParam);
            }
            catch (IllegalArgumentException iae) {
                subscriptionType = SubscriptionType.Exclusive;
            }
        }
    }

    @Override
    public void runApp() {

    }

    @Override
    public void termApp() {

    }
}
