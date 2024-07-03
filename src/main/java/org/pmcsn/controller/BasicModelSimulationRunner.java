package org.pmcsn.controller;


import org.pmcsn.centers.*;
import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;

import java.util.ArrayList;
import java.util.List;

public class BasicModelSimulationRunner {
    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Population
     */

    // Constants
    private static final int START = 0;
    private static final int STOP = 86400;
    private static final long SEED = 123456789L;
    private static double sarrival;


    public void runBasicModelSimulation() {
        System.out.println("Running Basic Model Simulation...");

        //Rng setting the seed
        long[] seeds = new long[1024];
        seeds[0] = SEED;
        Rngs rngs = new Rngs();


        for (int i = 0; i < 150; i++) {

            boolean stopArrivals = false;
            sarrival = START;
            long number = 1;

            rngs.plantSeeds(seeds[i]);

            //Msq initialization
            MsqTime msqTime = new MsqTime();
            msqTime.current = START;
            List<MsqEvent> events = new ArrayList<>();

            //generating first arrival
            double time = getArrival(rngs);
            events.add(new MsqEvent(time, true, EventType.ARRIVAL_LUGGAGE_CHECK));

            //creation of centers
            LuggageChecks luggageChecks = new LuggageChecks();
            CheckInDesksTarget checkInDesksTarget = new CheckInDesksTarget();
            CheckInDesksOthers checkInDesksOthers = new CheckInDesksOthers();
            BoardingPassScanners boardingPassScanners = new BoardingPassScanners(rngs);
            SecurityChecks securityChecks = new SecurityChecks();
            PassportChecks passportChecks = new PassportChecks();
            StampsCheck stampsCheck = new StampsCheck();
            Boarding boardingTarget = new Boarding();


            while(!stopArrivals && number != 0) {

                //TODO: aggiungere i vari processamenti

                //TODO: aggiornare la variabile number come la somma dei number dei vari centri
            }

        }

    }


    double exponential(double m, Rngs r) {
        /* ---------------------------------------------------
         * generate an Exponential random variate, use m > 0.0
         * ---------------------------------------------------
         */
        return (-m * Math.log(1.0 - r.random()));
    }

    double getArrival(Rngs r) {
        /* --------------------------------------------------------------
         * generate the next arrival time, with rate 1/2
         * --------------------------------------------------------------
         */
        r.selectStream(0);
        sarrival += exponential(2.0, r);
        return (sarrival);
    }

}
