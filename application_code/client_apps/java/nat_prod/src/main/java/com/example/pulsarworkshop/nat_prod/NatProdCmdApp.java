package com.example.pulsarworkshop.nat_prod;

import com.example.pulsarworkshop.common.PulsarWorkshopCmdApp;
import com.example.pulsarworkshop.common.exception.HelpExitException;
import com.example.pulsarworkshop.common.exception.InvalidParamException;

public class NatProdCliCmdApp extends PulsarWorkshopCmdApp {
    PulsarWorkshopCmdApp workshopApp = new NatProdCliCmdApp();

    public void main(String[] args) {
        try {
            workshopApp.processInputParams(args);



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
    public void processInputParams(String[] inputParams) throws
            HelpExitException, InvalidParamException {

    }

    @Override
    public void runApp() {

    }

    @Override
    public void termApp() {

    }

    @Override
    public void usage() {

    }
}
