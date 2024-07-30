package org.pmcsn.controller;


import org.pmcsn.centers.*;
import org.pmcsn.conf.CenterFactory;
import org.pmcsn.conf.Config;
import org.pmcsn.model.*;
import org.pmcsn.utils.Verification.Result;
import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.Statistics.MeanStatistics;

import java.util.ArrayList;
import java.util.List;

import static org.pmcsn.utils.Verification.modelVerification;
import static org.pmcsn.utils.Comparison.compareResults;

public class BatchSimulationRunner {
    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Population
     */

    // Constants
    private static final int START = 0;
    private static final long SEED = 123456789L;

    // let's assume K=400 (after discarding warmup) -> we have 400 mean values registered.
    // We need to compute autocorrelation on the series


    private long BATCH_SIZE_B; // Number of jobs in single batch (B)
    private long NUM_BATCHES_K; // Number of batches (K)
    private int WARMUP_THRESHOLD = 6000; // L'ho visto empiricamente perchè fino al batch 45 il boarding aveva infinity tra le statistiche

    public BatchSimulationRunner() {
        Config config = new Config();
        BATCH_SIZE_B = config.getInt("general", "batchSize");
        NUM_BATCHES_K = config.getInt("general", "numBatches");
        WARMUP_THRESHOLD = config.getInt("general", "warmup");
    }

    public BatchSimulationRunner(int numBatch_k, int batchSize_b, int warmup) {
        this.BATCH_SIZE_B = batchSize_b;
        this.NUM_BATCHES_K = numBatch_k;
        WARMUP_THRESHOLD = warmup;
    }

    public List<Statistics> runBatchSimulation(boolean approximateServiceAsExponential) throws Exception {
        CenterFactory factory = new CenterFactory();

        LuggageChecks luggageChecks = factory.createLuggageChecks(approximateServiceAsExponential);
        CheckInDesksTarget checkInDesksTarget = factory.createCheckinDeskTarget(approximateServiceAsExponential);
        CheckInDesksOthers checkInDesksOthers = factory.createCheckinDeskOthers(approximateServiceAsExponential);
        BoardingPassScanners boardingPassScanners = factory.createBoardingPassScanners(approximateServiceAsExponential);
        SecurityChecks securityChecks = factory.createSecurityChecks(approximateServiceAsExponential);
        PassportChecks passportChecks = factory.createPassportChecks(approximateServiceAsExponential);
        StampsCheck stampsCheck = factory.createStampsCheck(approximateServiceAsExponential);
        Boarding boarding = factory.createBoarding(approximateServiceAsExponential);

        if (approximateServiceAsExponential) {
            System.out.println("\nRunning Batch Simulation with Exponential Service...");
        }else{
            System.out.println("\nRunning Batch Simulation...");
        }

        // Rng setting the seed
        Rngs rngs = new Rngs();
        rngs.plantSeeds(SEED);

        // Initialize MsqTime
        MsqTime msqTime = new MsqTime();
        msqTime.current = START;
        EventQueue events = new EventQueue();

        // Initialize LuggageChecks
        luggageChecks.reset(rngs, START);
        luggageChecks.setSTOP(Integer.MAX_VALUE);

        // Generate the first arrival
        double time = luggageChecks.getArrival();
        events.add(new MsqEvent(EventType.ARRIVAL_LUGGAGE_CHECK, time));

        // Initialize other centers
        checkInDesksTarget.reset(rngs);
        checkInDesksOthers.reset(rngs);
        boardingPassScanners.reset(rngs);
        securityChecks.reset(rngs);
        passportChecks.reset(rngs);
        stampsCheck.reset(rngs);
        boarding.reset(rngs);

        MsqEvent event;
        long alreadySaved = 0;
        int numberOfCurrentBatch = 0;


        while (luggageChecks.getTotalNumberOfJobsServed() <= BATCH_SIZE_B * NUM_BATCHES_K + WARMUP_THRESHOLD) {

            event = events.pop();
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

            long n = luggageChecks.getTotalNumberOfJobsServed();
            if (n <= WARMUP_THRESHOLD) {
                continue;
            }
            // still in the middle of a batch, need to save statistics
            if((n - WARMUP_THRESHOLD - alreadySaved) == BATCH_SIZE_B){
                // keeping track of the fact that a batch has already been processed
                alreadySaved += BATCH_SIZE_B;
                numberOfCurrentBatch++;

                // saving the statistics of the batch only after the warm-up time period
                luggageChecks.saveStats();
                checkInDesksTarget.saveStats();
                checkInDesksOthers.saveStats();
                boardingPassScanners.saveStats();
                securityChecks.saveStats();
                passportChecks.saveStats();
                stampsCheck.saveStats();
                boarding.saveStats();
            }
        }

        System.out.println("SIMULATION RUN DID " + numberOfCurrentBatch + " BATCHES");

        String SIMULATION_TYPE;
        if(approximateServiceAsExponential){
            SIMULATION_TYPE = "BATCH_SIMULATION_EXPONENTIAL";
        }else{
            SIMULATION_TYPE = "BATCH_SIMULATION";
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

        List<Statistics> statisticsList = new ArrayList<>();
        statisticsList.addAll(luggageChecks.getStatistics());
        statisticsList.add(checkInDesksTarget.getStatistics());
        statisticsList.addAll(checkInDesksOthers.getStatistics());
        statisticsList.add(boardingPassScanners.getStatistics());
        statisticsList.add(securityChecks.getStatistics());
        statisticsList.add(passportChecks.getStatistics());
        statisticsList.add(stampsCheck.getStatistics());
        statisticsList.add(boarding.getStatistics());

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
        }

        return statisticsList;

        //printJobsServedByNodes(luggageChecks, checkInDesksTarget, checkInDesksOthers, boardingPassScanners, securityChecks, passportChecks, stampsCheck, boarding);
    }
}
