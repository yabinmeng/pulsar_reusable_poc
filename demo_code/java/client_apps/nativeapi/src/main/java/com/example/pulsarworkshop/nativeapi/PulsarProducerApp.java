package com.example.pulsarworkshop.nativeapi;

import com.example.pulsarworkshop.utilities.WorkshopConfUtil;
import com.example.pulsarworkshop.utilities.exception.CliOptProcException;

import java.util.Map;

public class PulsarProducer extends PulsarClientCLIApp {

    public PulsarProducer() {
        super(true);
    }
    public static void main(String[] args) {

        PulsarProducer producerCmdApp = new PulsarProducer();

        try {
            producerCmdApp.processInputParameters(args);

            WorkshopConfUtil workshopConfUtil =
                    new WorkshopConfUtil(producerCmdApp.getWorkShopCfgFile());

            Map<String, String> clientConfMap = workshopConfUtil.getClientConfMapRaw();



        }
        catch (CliOptProcException cope) {
            System.exit(cope.getSystemErrExitCode());
        }
    }
}
