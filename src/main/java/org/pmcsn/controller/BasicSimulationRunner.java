package org.pmcsn.controller;


import org.pmcsn.WelchPlot;
import org.pmcsn.centers.*;
import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;
import org.pmcsn.model.Statistics.MeanStatistics;
import org.pmcsn.utils.Verification.Result;

import java.util.ArrayList;
import java.util.List;

import static org.pmcsn.utils.Comparison.compareResults;
import static org.pmcsn.utils.Verification.modelVerification;

public class BasicSimulationRunner {
    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Population
     */

    // Constants
    private static final int START = 0;
    private static final long SEED = 123456789L;


    public void runBasicSimulation(boolean approximateServiceAsExponential) throws Exception {
        if (approximateServiceAsExponential) {
            System.out.println("\nRunning Basic Simulation with Exponential Service...");
        }else{
            System.out.println("\nRunning Basic Simulation...");
        }

        //Rng setting the seed
        long[] seeds = new long[1024];
        seeds[0] = SEED;
        Rngs rngs = new Rngs();

        // Declare variables for centers
        LuggageChecks luggageChecks = new LuggageChecks(6, (24 * 60) / 6300.0, 1, approximateServiceAsExponential);
        CheckInDesksTarget checkInDesksTarget = new CheckInDesksTarget("CHECK_IN_TARGET", 10, 3, 19, approximateServiceAsExponential);
        CheckInDesksOthers checkInDesksOthers = new CheckInDesksOthers(19, 3, 10, 21, approximateServiceAsExponential);
        BoardingPassScanners boardingPassScanners = new BoardingPassScanners("BOARDING_PASS_SCANNERS", 0.3, 3, 59, approximateServiceAsExponential);
        SecurityChecks securityChecks = new SecurityChecks("SECURITY_CHECKS", 0.9, 8, 61, approximateServiceAsExponential);
        PassportChecks passportChecks = new PassportChecks("PASSPORT_CHECK", 5, 24, 66, approximateServiceAsExponential);
        StampsCheck stampsCheck = new StampsCheck("STAMP_CHECK", 0.1,68, approximateServiceAsExponential);
        Boarding boarding = new Boarding("BOARDING", 4, 2, 72, approximateServiceAsExponential);

        final List<Observations> observationsList = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            observationsList.add(new Observations("LUGGAGE_CHECKS_%d".formatted(i+1), 150, List.of("E[Ts]")));
        }
        for (int i = 0; i < 150; i++) {

            double sarrival = START;
            long number = 1;

            rngs.plantSeeds(seeds[i]);

            //Msq initialization
            MsqTime msqTime = new MsqTime();
            msqTime.current = START;
            EventQueue queue = new EventQueue();

            // Initialize LuggageChecks
            luggageChecks.reset(rngs, sarrival);

            //generating first arrival
            double time = luggageChecks.getArrival();
            queue.add(new MsqEvent(EventType.ARRIVAL_LUGGAGE_CHECK, time));

            // Initialize other centers
            checkInDesksTarget.reset(rngs);
            checkInDesksOthers.reset(rngs);
            boardingPassScanners.reset(rngs);
            securityChecks.reset(rngs);
            passportChecks.reset(rngs);
            stampsCheck.reset(rngs);
            boarding.reset(rngs);

            MsqEvent event;

            int eventCount = 0;

            // need to use OR because both the conditions should be false
            while (!luggageChecks.isEndOfArrivals() || number != 0) {

                event = queue.pop();
                msqTime.next = event.time;

                // Updating the areas
                luggageChecks.setArea(msqTime);
                checkInDesksTarget.setArea(msqTime);
                checkInDesksOthers.updateArea(msqTime);
                boardingPassScanners.setArea(msqTime);
                securityChecks.setArea(msqTime);
                passportChecks.setArea(msqTime);
                stampsCheck.setArea(msqTime);
                boarding.setArea(msqTime);

                // Advancing the clock
                msqTime.current = msqTime.next;

                // Processing the event based on its type
                switch (event.type) {
                    case ARRIVAL_LUGGAGE_CHECK:
                        luggageChecks.processArrival(event, msqTime, queue);
                        break;
                    case LUGGAGE_CHECK_DONE:
                        luggageChecks.processCompletion(event, msqTime, queue);
                        break;
                    case ARRIVAL_CHECK_IN_TARGET:
                        checkInDesksTarget.processArrival(event, msqTime, queue);
                        break;
                    case CHECK_IN_TARGET_DONE:
                        checkInDesksTarget.processCompletion(event, msqTime, queue);
                        break;
                    case ARRIVAL_CHECK_IN_OTHERS:
                        checkInDesksOthers.processArrival(event, msqTime, queue);
                        break;
                    case CHECK_IN_OTHERS_DONE:
                        checkInDesksOthers.processCompletion(event, msqTime, queue);
                        break;
                    case ARRIVAL_BOARDING_PASS_SCANNERS:
                        boardingPassScanners.processArrival(event, msqTime, queue);
                        break;
                    case BOARDING_PASS_SCANNERS_DONE:
                        boardingPassScanners.processCompletion(event, msqTime, queue);
                        break;
                    case ARRIVAL_SECURITY_CHECK:
                        securityChecks.processArrival(event, msqTime, queue);
                        break;
                    case SECURITY_CHECK_DONE:
                        securityChecks.processCompletion(event, msqTime, queue);
                        break;
                    case ARRIVAL_PASSPORT_CHECK:
                        passportChecks.processArrival(event, msqTime, queue);
                        break;
                    case PASSPORT_CHECK_DONE:
                        passportChecks.processCompletion(event, msqTime, queue);
                        break;
                    case ARRIVAL_STAMP_CHECK:
                        stampsCheck.processArrival(event, msqTime, queue);
                        break;
                    case STAMP_CHECK_DONE:
                        stampsCheck.processCompletion(event, msqTime, queue);
                        break;
                    case ARRIVAL_BOARDING:
                        boarding.processArrival(event, msqTime, queue);
                        break;
                    case BOARDING_DONE:
                        boarding.processCompletion(event, msqTime, queue);
                        break;
                }

                // Saving observations to compute warm up period boundaries
                luggageChecks.updateObservations(observationsList, i);

                eventCount++;

                number = luggageChecks.getNumberOfJobsInNode() + checkInDesksTarget.getNumberOfJobsInNode() + checkInDesksOthers.getNumberOfJobsInNode() + boarding.getNumberOfJobsInNode()
                        + boardingPassScanners.getNumberOfJobsInNode() + securityChecks.getNumberOfJobsInNode() + passportChecks.getNumberOfJobsInNode() + stampsCheck.getNumberOfJobsInNode() + boarding.getNumberOfJobsInNode();

            }

            //System.out.println("EVENT COUNT FOR RUN N°"+i+": " + eventCount);

            // Saving statistics for current run
            luggageChecks.saveStats();
            checkInDesksTarget.saveStats();
            checkInDesksOthers.saveStats();
            boardingPassScanners.saveStats();
            securityChecks.saveStats();
            passportChecks.saveStats();
            stampsCheck.saveStats();
            boarding.saveStats();

            // Generating next seed
            rngs.selectStream(255);
            seeds[i + 1] = rngs.getSeed();
        }

        String SIMULATION_TYPE;
        if(approximateServiceAsExponential){
            SIMULATION_TYPE = "BASIC_SIMULATION_EXPONENTIAL";
        }else{
            SIMULATION_TYPE = "BASIC_SIMULATION";
        }

        // Computing warm up period boundaries
        WelchPlot.writeObservations(SIMULATION_TYPE, observationsList);


        // Writing statistics csv with data from all runs
        luggageChecks.writeStats(SIMULATION_TYPE);
        checkInDesksTarget.writeStats(SIMULATION_TYPE);
        checkInDesksOthers.writeStats(SIMULATION_TYPE);
        boardingPassScanners.writeStats(SIMULATION_TYPE);
        securityChecks.writeStats(SIMULATION_TYPE);
        passportChecks.writeStats(SIMULATION_TYPE);
        stampsCheck.writeStats(SIMULATION_TYPE);
        boarding.writeStats(SIMULATION_TYPE);

        if(approximateServiceAsExponential) {
            // Computing and writing verifications stats csv
            List<Result> verificationResults = modelVerification(SIMULATION_TYPE);

            // Compare results and verifications and save comparison result
            List<MeanStatistics> meanStatisticsList = new ArrayList<>();
            meanStatisticsList.add(luggageChecks.getMeanStatistics());
            meanStatisticsList.add(checkInDesksTarget.getMeanStatistics());
            meanStatisticsList.add(checkInDesksOthers.getMeanStatistics());
            meanStatisticsList.add(boardingPassScanners.getMeanStatistics());
            meanStatisticsList.add(securityChecks.getMeanStatistics());
            meanStatisticsList.add(passportChecks.getMeanStatistics());
            meanStatisticsList.add(stampsCheck.getMeanStatistics());
            meanStatisticsList.add(boarding.getMeanStatistics());

            compareResults(SIMULATION_TYPE, verificationResults, meanStatisticsList);
        }

        //printJobsServedByNodes(luggageChecks, checkInDesksTarget, checkInDesksOthers, boardingPassScanners, securityChecks, passportChecks, stampsCheck, boarding);

    }

}
