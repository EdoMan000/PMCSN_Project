package org.pmcsn.controller;


import org.pmcsn.centers.*;
import org.pmcsn.conf.CenterFactory;
import org.pmcsn.conf.Config;
import org.pmcsn.model.*;
import org.pmcsn.model.MeanStatistics;
import org.pmcsn.libraries.Rngs;
import org.pmcsn.utils.AnalyticalComputation;
import org.pmcsn.utils.Comparison;
import org.pmcsn.utils.Verification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import static org.pmcsn.utils.PrintUtils.printFinalResults;
import static org.pmcsn.utils.PrintUtils.printJobsServedByNodes;
import static org.pmcsn.utils.AnalyticalComputation.computeAnalyticalResults;
import static org.pmcsn.utils.Comparison.compareResults;
import static org.pmcsn.utils.Verification.verifyConfidenceIntervals;

public class BatchSimulationRunner {
    private static final Logger logger = Logger.getLogger(BatchSimulationRunner.class.getName());
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

    // We need to compute autocorrelation on the series
    // Number of jobs in single batch (B)
    private final int batchSize;
    // Number of batches (K >= 40)
    private final int batchesNumber;
    private final int warmupThreshold;
    private boolean isWarmingUp = true;
    private final int[] eventsCounter = new int[6];

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

    public List<BatchStatistics> runBatchSimulation(boolean approximateServiceAsExponential) throws Exception {
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

        while(checkEndOfBatchSimulation(((long) batchesNumber * batchSize))) {
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
                saveAllBatchStats(msqTime); // saving the statistics of the batch only after the warm-up time period
            }
        }
        System.out.println(simulationType + " HAS JUST FINISHED.");

        // Writing statistics csv with data from all batches
        writeAllStats(simulationType);

        if (approximateServiceAsExponential) {
            modelVerification(simulationType); // Computing and writing verifications stats csv
        }

        // controllo di consistenza sul numero di jobs processati
         printJobsServedByNodes(luggageChecks, checkInDesksTarget, checkInDesksOthers, boardingPassScanners, securityChecks, passportChecks, stampsCheck, boardingTarget, boardingOthers, false);

        return luggageChecks.getBatchStatistics();
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
                if (!isWarmingUp) {
                    ++eventsCounter[event.nodeId-1];
                }
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

    private void saveAllBatchStats(MsqTime time) {
        luggageChecks.saveBatchStats(time);
        checkInDesksTarget.saveBatchStats(time);
        checkInDesksOthers.saveBatchStats(time);
        boardingPassScanners.saveBatchStats(time);
        securityChecks.saveBatchStats(time);
        passportChecks.saveBatchStats(time);
        stampsCheck.saveBatchStats(time);
        boardingTarget.saveBatchStats(time);
        boardingOthers.saveBatchStats(time);
    }

    private void modelVerification(String simulationType) {
        List<AnalyticalComputation.AnalyticalResult> analyticalResultList = computeAnalyticalResults(simulationType);

        // Compare results and verifications and save comparison result
        List<MeanStatistics> batchMeanStatisticsList = aggregateBatchMeanStatistics();

        List<Comparison.ComparisonResult> comparisonResultList = compareResults(simulationType, analyticalResultList, batchMeanStatisticsList);

        List<ConfidenceIntervals> confidenceIntervalsList = aggregateConfidenceIntervals();

        List<Verification.VerificationResult> verificationResultList = verifyConfidenceIntervals(simulationType, comparisonResultList, confidenceIntervalsList);

        printFinalResults(comparisonResultList, verificationResultList);
    }

    private List<MeanStatistics> aggregateBatchMeanStatistics() {
        List<MeanStatistics> batchMeanStatisticsList = new ArrayList<>();

        batchMeanStatisticsList.addAll(luggageChecks.getBatchMeanStatistics());
        batchMeanStatisticsList.add(checkInDesksTarget.getBatchMeanStatistics());
        batchMeanStatisticsList.addAll(checkInDesksOthers.getBatchMeanStatistics());
        batchMeanStatisticsList.add(boardingPassScanners.getBatchMeanStatistics());
        batchMeanStatisticsList.add(securityChecks.getBatchMeanStatistics());
        batchMeanStatisticsList.add(passportChecks.getBatchMeanStatistics());
        batchMeanStatisticsList.add(stampsCheck.getBatchMeanStatistics());
        batchMeanStatisticsList.add(boardingTarget.getBatchMeanStatistics());
        batchMeanStatisticsList.addAll(boardingOthers.getBatchMeanStatistics());
        return batchMeanStatisticsList;
    }

    private List<ConfidenceIntervals> aggregateConfidenceIntervals() {
        List<ConfidenceIntervals> confidenceIntervalsList = new ArrayList<>();

        confidenceIntervalsList.addAll(createConfidenceIntervalsList(luggageChecks.getBatchStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(checkInDesksTarget.getBatchStatistics()));
        confidenceIntervalsList.addAll(createConfidenceIntervalsList(checkInDesksOthers.getBatchStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(boardingPassScanners.getBatchStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(securityChecks.getBatchStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(passportChecks.getBatchStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(stampsCheck.getBatchStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(boardingTarget.getBatchStatistics()));
        confidenceIntervalsList.addAll(createConfidenceIntervalsList(boardingOthers.getBatchStatistics()));

        return confidenceIntervalsList;
    }

    private ConfidenceIntervals createConfidenceIntervals(BatchStatistics stats) {
        return new ConfidenceIntervals(
                stats.meanResponseTimeList, stats.meanQueueTimeList, stats.meanServiceTimeList,
                stats.meanSystemPopulationList, stats.meanQueuePopulationList, stats.meanUtilizationList, stats.lambdaList
        );
    }

    private List<ConfidenceIntervals> createConfidenceIntervalsList(List<BatchStatistics> statisticsList) {
        List<ConfidenceIntervals> confidenceIntervalsList = new ArrayList<>();
        for (BatchStatistics stats : statisticsList) {
            confidenceIntervalsList.add(createConfidenceIntervals(stats));
        }
        return confidenceIntervalsList;
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

    private long getMinimumNumberOfJobsServedByCenters() { //TODO
        long minimumLuggageChecks = Arrays.stream(luggageChecks.getNumberOfJobsPerCenter()).min().orElseThrow();
        // System.out.printf("Luggage checks %d%n", minimumLuggageChecks);
        long minimumCheckInDeskOthers = Arrays.stream(checkInDesksOthers.getNumberOfJobsPerCenter()).min().orElseThrow();
        // System.out.printf("Check In Desk Others %d%n", minimumCheckInDeskOthers);
        long minimumBoardingOthers = Arrays.stream(boardingOthers.getNumberOfJobsPerCenter()).min().orElseThrow();
        // System.out.printf("Boarding Others %d%n", minimumBoardingOthers);
//        return Stream.of(
//                minimumLuggageChecks,
//                checkInDesksTarget.getJobsServed(),
//                minimumCheckInDeskOthers,
//                boardingPassScanners.getJobsServed(),
//                securityChecks.getJobsServed(),
//                passportChecks.getJobsServed(),
//                stampsCheck.getJobsServed(),
//                boardingTarget.getJobsServed(),
//                minimumBoardingOthers
//        ).min(Long::compare).orElseThrow();
        return Arrays.stream(eventsCounter).min().orElseThrow();
    }

    private boolean checkEndOfBatchSimulation(long n) {
        return getMinimumNumberOfJobsServedByCenters() <= n;
    }
}
