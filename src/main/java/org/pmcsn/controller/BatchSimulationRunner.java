package org.pmcsn.controller;


import org.pmcsn.centers.*;
import org.pmcsn.utils.Verification.Result;
import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;
import org.pmcsn.model.Statistics.MeanStatistics;

import java.util.ArrayList;
import java.util.List;

import static org.pmcsn.utils.Verification.modelVerification;
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

    // total jobs processed are 24M so the following k and b work
//    private static final int BATCH_SIZE = 24765; // Number of jobs in single batch (B)
//    private static final int NUM_BATCHES = 1024; // Number of batches (K)


    private static final int BATCH_SIZE = 10000; // Number of jobs in single batch (B)
    private static final int NUM_BATCHES = 100; // Number of batches (K)

    public void runBatchSimulation() {
        System.out.println("\nRunning Batch Simulation...");

        // Rng setting the seed
        Rngs rngs = new Rngs();
        rngs.plantSeeds(SEED);

        // Declare variables for centers
        LuggageChecks luggageChecks = new LuggageChecks(6, (24 * 60) / 3600.0, 1.4);
        CheckInDesksTarget checkInDesksTarget = new CheckInDesksTarget("CHECK_IN_TARGET", 10, 3, 10);
        CheckInDesksOthers checkInDesksOthers = new CheckInDesksOthers();
        BoardingPassScanners boardingPassScanners = new BoardingPassScanners();
        SecurityChecks securityChecks = new SecurityChecks("SECURITY_CHECKS", 1.8, 8, 52);
        PassportChecks passportChecks = new PassportChecks("PASSPORT_CHECKS", 5, 24, 57);
        StampsCheck stampsCheck = new StampsCheck(0.1);
        Boarding boarding = new Boarding("BOARDING", 4, 2, 63);

        // Initialize MsqTime
        MsqTime msqTime = new MsqTime();
        msqTime.current = START;
        List<MsqEvent> events = new ArrayList<>();

        // Initialize LuggageChecks
        luggageChecks.reset(rngs, START);
        luggageChecks.setSTOP(Integer.MAX_VALUE);

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
        long alreadySaved = 0;
        //while (!luggageChecks.isEndOfArrivals()) {
        while (!(luggageChecks.getTotalNumberOfJobsServed() > BATCH_SIZE*NUM_BATCHES)) {

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

            number = luggageChecks.getNumberOfJobsInNode() + checkInDesksTarget.getNumberOfJobsInNode() + checkInDesksOthers.getNumberOfJobsInNode() + boarding.getNumberOfJobsInNode()
                    + boardingPassScanners.getNumberOfJobsInNode() + securityChecks.getNumberOfJobsInNode() + passportChecks.getNumberOfJobsInNode() + stampsCheck.getNumberOfJobsInNode() + boarding.getNumberOfJobsInNode();

            // Saving statistics for current batch
            //TODO condizione di scarto (forse?) (es: non contare primi 20/30 batch)
            long totJobs = luggageChecks.getTotalNumberOfJobsServed();
            if((totJobs - alreadySaved) == BATCH_SIZE) {
                alreadySaved += totJobs;
                luggageChecks.saveStats();
                checkInDesksTarget.saveStats();
                checkInDesksOthers.saveStats();
                boardingPassScanners.saveStats();
                securityChecks.saveStats();
                passportChecks.saveStats();
                stampsCheck.saveStats();
                boarding.saveStats();
            }


//            for (int i = 0; i < luggageChecks.numberOfCenters; i++) {
//                long jobsServed = luggageChecks.getJobsServed(i);
//                //System.out.println("Luggage Checks Center " + i + ": Jobs Served = " + jobsServed);
//                if (jobsServed == BATCH_SIZE && luggageChecks.getBatchIndex(i) <= NUM_BATCHES) {
//                    luggageChecks.saveStats(i);
//                    luggageChecks.resetBatch(i);
//                    System.out.println("Luggage Checks Center " + i + ": Jobs Served = " + jobsServed + ": BATCH:INDEX = " + luggageChecks.getBatchIndex(i));
//                }
//            }
//
//            long checkInDesksTargetJobsServed = checkInDesksTarget.getJobsServed();
//            //System.out.println("Check-In Desks Target: Jobs Served = " + checkInDesksTargetJobsServed);
//            if (checkInDesksTargetJobsServed == BATCH_SIZE && checkInDesksTarget.batchIndex <= NUM_BATCHES) {
//                checkInDesksTarget.saveStats();
//                checkInDesksTarget.resetBatch();
//                System.out.println("Check-In Desks Target: Jobs Served = " + checkInDesksTargetJobsServed + ": BATCH:INDEX = " + checkInDesksTarget.batchIndex);
//            }
//
//            for (int i = 0; i < checkInDesksOthers.numberOfCenters; i++) {
//                long jobsServed = checkInDesksOthers.getJobsServed(i);
//                //System.out.println("Check-In Desks Others Center " + i + ": Jobs Served = " + jobsServed);
//                if (jobsServed == BATCH_SIZE && checkInDesksOthers.getBatchIndex(i) <= NUM_BATCHES) {
//                    checkInDesksOthers.saveStats(i);
//                    checkInDesksOthers.resetBatch(i);
//                    System.out.println("Check-In Desks Others Center " + i + ": Jobs Served = " + jobsServed + ": BATCH:INDEX = " + checkInDesksOthers.getBatchIndex(i));
//                }
//            }
//
//            long boardingPassScannersJobsServed = boardingPassScanners.getJobsServed();
//            //System.out.println("Boarding Pass Scanners: Jobs Served = " + boardingPassScannersJobsServed);
//            if (boardingPassScannersJobsServed == BATCH_SIZE && boardingPassScanners.batchIndex <= NUM_BATCHES){
//                boardingPassScanners.saveStats();
//                boardingPassScanners.resetBatch();
//                System.out.println("Boarding Pass Scanners: Jobs Served = " + checkInDesksTargetJobsServed + ": BATCH:INDEX = " + boardingPassScanners.batchIndex);
//            }
//
//            long securityChecksJobsServed = securityChecks.getJobsServed();
//            //System.out.println("Security Checks: Jobs Served = " + securityChecksJobsServed);
//            if (securityChecksJobsServed == BATCH_SIZE && securityChecks.batchIndex <= NUM_BATCHES) {
//                securityChecks.saveStats();
//                securityChecks.resetBatch();
//                System.out.println("Security Checks: Jobs Served = " + checkInDesksTargetJobsServed + ": BATCH:INDEX = " + securityChecks.batchIndex);
//            }
//
//            long passportChecksJobsServed = passportChecks.getJobsServed();
//            //System.out.println("Passport Checks: Jobs Served = " + passportChecksJobsServed);
//            if (passportChecksJobsServed == BATCH_SIZE && passportChecks.batchIndex <= NUM_BATCHES) {
//                passportChecks.saveStats();
//                passportChecks.resetBatch();
//                System.out.println("Passport Checks: Jobs Served = " + checkInDesksTargetJobsServed + ": BATCH:INDEX = " + passportChecks.batchIndex);
//            }
//
//            long stampsCheckJobsServed = stampsCheck.getJobsServed();
//            //System.out.println("Stamps Check: Jobs Served = " + stampsCheckJobsServed);
//            if (stampsCheckJobsServed == BATCH_SIZE && stampsCheck.batchIndex <= NUM_BATCHES) {
//                stampsCheck.saveStats();
//                stampsCheck.resetBatch();
//                System.out.println("Stamps Check: Jobs Served = " + checkInDesksTargetJobsServed + ": BATCH:INDEX = " + stampsCheck.batchIndex);
//            }
//
//            long boardingJobsServed = boarding.getJobsServed();
//            //System.out.println("Boarding: Jobs Served = " + boardingJobsServed);
//            if (boardingJobsServed == BATCH_SIZE && boarding.batchIndex <= NUM_BATCHES) {
//                boarding.saveStats();
//                boarding.resetBatch();
//                System.out.println("Boarding: Jobs Served = " + checkInDesksTargetJobsServed + ": BATCH:INDEX = " + boarding.batchIndex);
//            }
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

        // controllo di consistenza sul numero di jobs processati
        long jobServedEntrances = luggageChecks.getTotalNumberOfJobsServed();
        System.out.println("TOT Luggage Checks Jobs Served = " + jobServedEntrances);

        long checkInDesksTargetJobsServed = checkInDesksTarget.getJobsServed();
        //System.out.println("Check-In Desks Target: Jobs Served = " + checkInDesksTargetJobsServed);

        long jobServedCheckIns = checkInDesksTargetJobsServed;
        for (int i = 0; i < checkInDesksOthers.numberOfCenters; i++) {
            long jobsServed = checkInDesksOthers.getJobsServed(i);
            jobServedCheckIns += jobsServed;
            //System.out.println("Check-In Desks Others Center " + i + ": Jobs Served = " + jobsServed);
        }
        System.out.println("TOT Check-In Desks Jobs Served = " + jobServedCheckIns);

        long boardingPassScannersJobsServed = boardingPassScanners.getJobsServed();
        System.out.println("Boarding Pass Scanners: Jobs Served = " + boardingPassScannersJobsServed);

        long securityChecksJobsServed = securityChecks.getJobsServed();
        System.out.println("Security Checks: Jobs Served = " + securityChecksJobsServed);

        long passportChecksJobsServed = passportChecks.getJobsServed();
        System.out.println("Passport Checks: Jobs Served = " + passportChecksJobsServed);

        long stampsCheckJobsServed = stampsCheck.getCompletions();
        System.out.println("Stamps Check: Jobs Served = " + stampsCheckJobsServed);

        long boardingJobsServed = boarding.getJobsServed();
        System.out.println("Boarding: Jobs Served = " + boardingJobsServed);


    }
}
