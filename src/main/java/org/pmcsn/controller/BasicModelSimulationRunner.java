package org.pmcsn.controller;


import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqT;

public class BasicModelSimulationRunner {

    // Constants
    private static final int START = 0;
    private static final int STOP = 86400;
    private static final long SEED = 123456789L;


    public void runBasicModelSimulation() {
        System.out.println("Running Basic Model Simulation...");
        //Msq initialization
        MsqT msqT = new MsqT();
        //MsqEvent msqEvent = new MsqEvent();
    }

}
