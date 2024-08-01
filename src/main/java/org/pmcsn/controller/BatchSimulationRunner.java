package org.pmcsn.controller;


import org.pmcsn.centers.*;
import org.pmcsn.conf.CenterFactory;
import org.pmcsn.conf.Config;
import org.pmcsn.model.*;
import org.pmcsn.utils.Verification.Result;
import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.Statistics.MeanStatistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.pmcsn.utils.PrintUtils.printJobsServedByNodes;
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
    private LuggageChecks luggageChecks;
    private CheckInDesksTarget checkInDesksTarget;
    private CheckInDesksOthers checkInDesksOthers;
    private BoardingPassScanners boardingPassScanners;
    private SecurityChecks securityChecks;
    private PassportChecks passportChecks;
    private StampsCheck stampsCheck;
    private BoardingTarget boardingTarget;
    private BoardingOthers boardingOthers;

    // let's assume K=400 (after discarding warmup) -> we have 400 mean values registered.
    // We need to compute autocorrelation on the series
    // Number of jobs in single batch (B)
    private final int batchSize;
    // Number of batches (K >= 40)
    private final int batchesNumber;
    private final int warmupThreshold;

    public BatchSimulationRunner() {
        Config config = new Config();
        batchSize = config.getInt("general", "batchSize");
        batchesNumber = config.getInt("general", "numBatches");
        warmupThreshold = config.getInt("general", "warmup");
    }

    public BatchSimulationRunner(int batchesNumber, int batchSize, int warmupThreshold) {
        this.batchSize = batchSize;
        this.batchesNumber = batchesNumber;
        this.warmupThreshold = warmupThreshold;
    }

    public List<Statistics> runBatchSimulation(boolean approximateServiceAsExponential) throws Exception {
        initCenters(approximateServiceAsExponential);

        String simulationType;
        if (approximateServiceAsExponential) {
            simulationType = "BATCH_SIMULATION_EXPONENTIAL";
        } else {
            simulationType = "BATCH_SIMULATION";
        }
        System.out.println("\nRUNNING " + simulationType + "...");

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

        resetOtherCenters(rngs); // Reset other centers

        MsqEvent event;

        boolean isWarmingUp = true;

        while(checkEndOfBatchSimulation((batchesNumber * batchSize) + warmupThreshold)){
            event = events.pop(); // Retrieving next event to be processed
            msqTime.next = event.time;
            updateAreas(msqTime); // Updating areas
            msqTime.current = msqTime.next; // Advancing the clock

            processCurrentEvent(event, msqTime, events); // Processing the event based on its type

            if (getMinimumNumberOfJobsServedByCenters() >= warmupThreshold && isWarmingUp) { // Checking if still in warmup period
                System.out.println("WARMUP COMPLETED... Starting to collect statistics for centers from now on.");
                isWarmingUp = false;
            }
            if (!isWarmingUp) {
                saveAllBatchStats(); // saving the statistics of the batch only after the warm-up time period
            }
        }
        System.out.println(simulationType + " HAS JUST FINISHED.");

        // Writing statistics csv with data from all batches
        writeAllStats(simulationType);

        if (approximateServiceAsExponential) {
            computeVerificationAndCompare(simulationType); // Computing and writing verifications stats csv
        }

        // controllo di consistenza sul numero di jobs processati
        printJobsServedByNodes(luggageChecks, checkInDesksTarget, checkInDesksOthers, boardingPassScanners, securityChecks, passportChecks, stampsCheck, boardingTarget, boardingOthers, false);

        return getAllStatsInSingleList();
    }


    private void initCenters(boolean approximateServiceAsExponential) {
        CenterFactory factory = new CenterFactory();
        luggageChecks = factory.createLuggageChecks(approximateServiceAsExponential);
        checkInDesksTarget = factory.createCheckinDeskTarget(approximateServiceAsExponential);
        checkInDesksOthers = factory.createCheckinDeskOthers(approximateServiceAsExponential);
        boardingPassScanners = factory.createBoardingPassScanners(approximateServiceAsExponential);
        securityChecks = factory.createSecurityChecks(approximateServiceAsExponential);
        passportChecks = factory.createPassportChecks(approximateServiceAsExponential);
        stampsCheck = factory.createStampsCheck(approximateServiceAsExponential);
        boardingTarget = factory.createBoardingTarget(approximateServiceAsExponential);
        boardingOthers = factory.createBoardingOthers(approximateServiceAsExponential);
    }

    private void resetOtherCenters(Rngs rngs) {
        checkInDesksTarget.reset(rngs);
        checkInDesksOthers.reset(rngs);
        boardingPassScanners.reset(rngs);
        securityChecks.reset(rngs);
        passportChecks.reset(rngs);
        stampsCheck.reset(rngs);
        boardingTarget.reset(rngs);
        boardingOthers.reset(rngs);
    }

    private void processCurrentEvent(MsqEvent event, MsqTime msqTime, EventQueue events) {
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
            case ARRIVAL_BOARDING_TARGET:
                boardingTarget.processArrival(event, msqTime, events);
                break;
            case BOARDING_TARGET_DONE:
                boardingTarget.processCompletion(event, msqTime, events);
                break;
            case ARRIVAL_BOARDING_OTHERS:
                boardingOthers.processArrival(event, msqTime, events);
                break;
            case BOARDING_OTHERS_DONE:
                boardingOthers.processCompletion(event, msqTime, events);
                break;
        }
    }

    private List<Statistics> getAllStatsInSingleList() {
        List<Statistics> statisticsList = new ArrayList<>();
        statisticsList.addAll(luggageChecks.getStatistics());
        statisticsList.add(checkInDesksTarget.getStatistics());
        statisticsList.addAll(checkInDesksOthers.getStatistics());
        statisticsList.add(boardingPassScanners.getStatistics());
        statisticsList.add(securityChecks.getStatistics());
        statisticsList.add(passportChecks.getStatistics());
        statisticsList.add(stampsCheck.getStatistics());
        statisticsList.add(boardingTarget.getStatistics());
        statisticsList.addAll(boardingOthers.getStatistics());
        return statisticsList;
    }

    private void resetAllBatchStats() {
        luggageChecks.resetBatch();
        checkInDesksTarget.resetBatch();
        checkInDesksOthers.resetBatch();
        boardingPassScanners.resetBatch();
        securityChecks.resetBatch();
        passportChecks.resetBatch();
        stampsCheck.resetBatch();
        boardingTarget.resetBatch();
        boardingOthers.resetBatch();
    }

    private void saveAllBatchStats() {
        luggageChecks.saveBatchStats(batchSize, batchesNumber);
        checkInDesksTarget.saveBatchStats(batchSize, batchesNumber);
        checkInDesksOthers.saveBatchStats(batchSize, batchesNumber);
        boardingPassScanners.saveBatchStats(batchSize, batchesNumber);
        securityChecks.saveBatchStats(batchSize, batchesNumber);
        passportChecks.saveBatchStats(batchSize, batchesNumber);
        stampsCheck.saveBatchStats(batchSize, batchesNumber);
        boardingTarget.saveBatchStats(batchSize, batchesNumber);
        boardingOthers.saveBatchStats(batchSize, batchesNumber);
    }

    private void computeVerificationAndCompare(String simulationType) {
        List<Result> verificationResults = modelVerification(simulationType);

        // Compare results and verifications and save comparison result
        List<MeanStatistics> meanStatisticsList = new ArrayList<>();
        meanStatisticsList.add(luggageChecks.getMeanStatistics());
        meanStatisticsList.add(checkInDesksTarget.getMeanStatistics());
        meanStatisticsList.add(checkInDesksOthers.getMeanStatistics());
        meanStatisticsList.add(boardingPassScanners.getMeanStatistics());
        meanStatisticsList.add(securityChecks.getMeanStatistics());
        meanStatisticsList.add(passportChecks.getMeanStatistics());
        meanStatisticsList.add(stampsCheck.getMeanStatistics());
        meanStatisticsList.add(boardingTarget.getMeanStatistics());
        meanStatisticsList.add(boardingOthers.getMeanStatistics());

        compareResults(simulationType, verificationResults, meanStatisticsList);
    }

    private void writeAllStats(String simulationType) {
        System.out.println("Writing csv files with stats for all the centers.");

        luggageChecks.writeStats(simulationType);
        checkInDesksTarget.writeStats(simulationType);
        checkInDesksOthers.writeStats(simulationType);
        boardingPassScanners.writeStats(simulationType);
        securityChecks.writeStats(simulationType);
        passportChecks.writeStats(simulationType);
        stampsCheck.writeStats(simulationType);
        boardingTarget.writeStats(simulationType);
        boardingOthers.writeStats(simulationType);
    }

    private void updateAreas(MsqTime msqTime) {
        luggageChecks.setAreaForAll(msqTime);
        checkInDesksTarget.setArea(msqTime);
        checkInDesksOthers.setAreaForAll(msqTime);
        boardingPassScanners.setArea(msqTime);
        securityChecks.setArea(msqTime);
        passportChecks.setArea(msqTime);
        stampsCheck.setArea(msqTime);
        boardingTarget.setArea(msqTime);
        boardingOthers.setAreaForAll(msqTime);
    }

    private long getMinimumNumberOfJobsServedByCenters() {
        long minimumLuggageChecks = Arrays.stream(luggageChecks.getNumberOfJobsPerCenter()).min().orElseThrow();
        // System.out.printf("Luggage checks %d%n", minimumLuggageChecks);
        long minimumCheckInDeskOthers = Arrays.stream(checkInDesksOthers.getNumberOfJobsPerCenter()).min().orElseThrow();
        // System.out.printf("Check In Desk Others %d%n", minimumCheckInDeskOthers);
        long minimumBoardingOthers = Arrays.stream(boardingOthers.getNumberOfJobsPerCenter()).min().orElseThrow();
        // System.out.printf("Boarding Others %d%n", minimumBoardingOthers);
        return Stream.of(
                minimumLuggageChecks,
                checkInDesksTarget.getJobsServed(),
                minimumCheckInDeskOthers,
                boardingPassScanners.getJobsServed(),
                securityChecks.getJobsServed(),
                passportChecks.getJobsServed(),
                stampsCheck.getJobsServed(),
                boardingTarget.getJobsServed(),
                minimumBoardingOthers
        ).min(Long::compare).orElseThrow();
    }

    private boolean checkEndOfBatchSimulation(long n) {
        return getMinimumNumberOfJobsServedByCenters() <= n;
    }
}
