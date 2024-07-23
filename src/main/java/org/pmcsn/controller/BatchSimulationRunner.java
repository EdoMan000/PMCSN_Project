package org.pmcsn.controller;


import org.pmcsn.centers.*;
import org.pmcsn.controller.Verification.Result;
import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;
import org.pmcsn.model.Statistics.MeanStatistics;

import java.util.ArrayList;
import java.util.List;

import static org.pmcsn.controller.Verification.modelVerification;
import static org.pmcsn.model.EventType.*;
import static org.pmcsn.utils.Comparison.compareResults;
import static org.pmcsn.utils.EventUtils.getNextEvent;

public class BatchSimulationRunner {
    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Population
     */

    // Constants
    private static final int START = 0;
    private static final long SEED = 123456789L;
    private static final String SIMULATION_TYPE = "BATCH_SIMULATION";
    private static int STOP = 1440;

    private static final int BATCH_SIZE = STOP*100; // Number of jobs in single batch (B)
    private static final int NUM_BATCHES = 150; // Number of batches (K)


    public void runBatchSimulation() {
        System.out.println("\nRunning Batch Simulation...");

        // Rng setting the seed
        Rngs rngs = new Rngs();
        rngs.plantSeeds(SEED);

        // Declare variables for centers
        LuggageChecks luggageChecks = new LuggageChecks();
        CheckInDesksTarget checkInDesksTarget = new CheckInDesksTarget();
        CheckInDesksOthers checkInDesksOthers = new CheckInDesksOthers();
        BoardingPassScanners boardingPassScanners = new BoardingPassScanners();
        SecurityChecks securityChecks = new SecurityChecks();
        PassportChecks passportChecks = new PassportChecks();
        StampsCheck stampsCheck = new StampsCheck();
        Boarding boarding = new Boarding();

        // Initialize MsqTime
        MsqTime msqTime = new MsqTime();
        msqTime.current = START;
        List<MsqEvent> events = new ArrayList<>();

        // Initialize LuggageChecks
        luggageChecks.reset(rngs, START);
        luggageChecks.setSTOP(Integer.MAX_VALUE); // so that it will not stop generating arrivals

        // Generate the first arrival
        double time = luggageChecks.getArrival();
        events.add(new MsqEvent(time, true, EventType.ARRIVAL_LUGGAGE_CHECK));

        // Initialize other centers
        checkInDesksTarget.reset(rngs);
        checkInDesksOthers.reset(rngs);
        boardingPassScanners.reset(rngs);
        securityChecks.reset(rngs);
        passportChecks.reset(rngs);
        stampsCheck.reset(rngs);
        boarding.reset(rngs);

        long number = 1;
        MsqEvent event;

        // need to use OR because both the conditions should be false
        while (!luggageChecks.isEndOfArrivals() || number != 0) {

            event = getNextEvent(events);
            msqTime.next = event.time;

            // Updating the areas
            luggageChecks.setArea(msqTime);
            checkInDesksTarget.setArea(msqTime);
            checkInDesksOthers.setArea(msqTime);
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
                    luggageChecks.processArrival(event, msqTime, events);
                    break;
                case LUGGAGE_CHECK_DONE:
                    luggageChecks.processCompletion(event, msqTime, events);
                    break;
                case ARRIVAL_CHECK_IN_TARGET:
                    checkInDesksTarget.processArrival(event, msqTime, events);
                    break;
                case CHECK_IN_TARGET_DONE:
                    checkInDesksTarget.processCompletion(event, msqTime, events);
                    break;
                case ARRIVAL_CHECK_IN_OTHERS:
                    checkInDesksOthers.processArrival(event, msqTime, events);
                    break;
                case CHECK_IN_OTHERS_DONE:
                    checkInDesksOthers.processCompletion(event, msqTime, events);
                    break;
                case ARRIVAL_BOARDING_PASS_SCANNERS:
                    boardingPassScanners.processArrival(event, msqTime, events);
                    break;
                case BOARDING_PASS_SCANNERS_DONE:
                    boardingPassScanners.processCompletion(event, msqTime, events);
                    break;
                case ARRIVAL_SECURITY_CHECK:
                    securityChecks.processArrival(event, msqTime, events);
                    break;
                case SECURITY_CHECK_DONE:
                    securityChecks.processCompletion(event, msqTime, events);
                    break;
                case ARRIVAL_PASSPORT_CHECK:
                    passportChecks.processArrival(event, msqTime, events);
                    break;
                case PASSPORT_CHECK_DONE:
                    passportChecks.processCompletion(event, msqTime, events);
                    break;
                case ARRIVAL_STAMP_CHECK:
                    stampsCheck.processArrival(event, msqTime, events);
                    break;
                case STAMP_CHECK_DONE:
                    stampsCheck.processCompletion(event, msqTime, events);
                    break;
                case ARRIVAL_BOARDING:
                    boarding.processArrival(event, msqTime, events);
                    break;
                case BOARDING_DONE:
                    boarding.processCompletion(event, msqTime, events);
                    break;
            }

            // TODO implementare saveBatchStats()
            // Saving statistics for current batch
            if(luggageChecks.getJobsServed() % BATCH_SIZE == 0)   luggageChecks.saveBatchStats();
            if(checkInDesksTarget.getJobsServed() % BATCH_SIZE == 0)   checkInDesksTarget.saveBatchStats();
            if(checkInDesksOthers.getJobsServed() % BATCH_SIZE == 0)   checkInDesksOthers.saveBatchStats();
            if(boardingPassScanners.getJobsServed() % BATCH_SIZE == 0)   boardingPassScanners.saveBatchStats();
            if(securityChecks.getJobsServed() % BATCH_SIZE == 0)   securityChecks.saveBatchStats();
            if(passportChecks.getJobsServed() % BATCH_SIZE == 0)   passportChecks.saveBatchStats();
            if(stampsCheck.getJobsServed() % BATCH_SIZE == 0)   stampsCheck.saveBatchStats();
            if(boarding.getJobsServed() % BATCH_SIZE == 0)   boarding.saveBatchStats();

            number = luggageChecks.getNumberOfJobsInNode() + checkInDesksTarget.getNumberOfJobsInNode() + checkInDesksOthers.getNumberOfJobsInNode() + boarding.getNumberOfJobsInNode()
                    + boardingPassScanners.getNumberOfJobsInNode() + securityChecks.getNumberOfJobsInNode() + passportChecks.getNumberOfJobsInNode() + stampsCheck.getNumberOfJobsInNode() + boarding.getNumberOfJobsInNode();
            //System.out.println("JOBS REMAINING IN SYSTEM FOR BATCH N°"+(batch+1)+": "+number);
        }

        // Writing statistics csv with data from all batches
        luggageChecks.writeStats(SIMULATION_TYPE);
        checkInDesksTarget.writeStats(SIMULATION_TYPE);
        checkInDesksOthers.writeStats(SIMULATION_TYPE);
        boardingPassScanners.writeStats(SIMULATION_TYPE);
        securityChecks.writeStats(SIMULATION_TYPE);
        passportChecks.writeStats(SIMULATION_TYPE);
        stampsCheck.writeStats(SIMULATION_TYPE);
        boarding.writeStats(SIMULATION_TYPE);

        // Computing and writing verifications stats csv
        List<Result> verificationResults = modelVerification(SIMULATION_TYPE);

        // Compare results and verifications and save comparison result
        List<MeanStatistics> meanStatisticsList = new ArrayList<MeanStatistics>();
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

}
