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

import static org.pmcsn.utils.PrintUtils.*;
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
    private CheckInDesks checkInDesks;
    private BoardingPassScanners boardingPassScanners;
    private SecurityChecks securityChecks;
    private PassportChecks passportChecks;
    private StampsCheck stampsCheck;
    private Boarding boarding;

    // We need to compute autocorrelation on the series
    // Number of jobs in single batch (B)
    private final int batchSize;
    // Number of batches (K >= 40)
    private final int batchesNumber;
    private final int warmupThreshold;
    private boolean isWarmingUp = true;


    public BatchSimulationRunner() {
        Config config = new Config();
        batchSize = config.getInt("general", "batchSize");
        batchesNumber = config.getInt("general", "numBatches");
        warmupThreshold = (int) ((batchSize*batchesNumber)*0.2);
    }

    public BatchSimulationRunner(int batchesNumber, int batchSize) {
        this.batchSize = batchSize;
        this.batchesNumber = batchesNumber;
        warmupThreshold = (int) ((batchSize*batchesNumber)*0.2);
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

        resetCenters(rngs); // Reset other centers

        MsqEvent event;

        while(!isDone()) {
            event = events.pop(); // Retrieving next event to be processed
            msqTime.next = event.time;
            updateAreas(msqTime); // Updating areas
            msqTime.current = msqTime.next; // Advancing the clock

            processCurrentEvent(event, msqTime, events); // Processing the event based on its type

            if ( isWarmingUp && getMinimumNumberOfJobsServedByCenters() >= warmupThreshold ) { // Checking if still in warmup period
                System.out.println("WARMUP COMPLETED... Starting to collect statistics for centers from now on.");
                isWarmingUp = false;
                stopWarmup(msqTime);
            }
        }
        System.out.println(simulationType + " HAS JUST FINISHED.");
        System.out.printf("Events queue size %d%n", events.size());

        // Writing statistics csv with data from all batches
        writeAllStats(simulationType);

        if (approximateServiceAsExponential) {
            modelVerification(simulationType); // Computing and writing verifications stats csv
        }

        // controllo di consistenza sul numero di jobs processati
         printJobsServedByNodes(luggageChecks, checkInDesks, boardingPassScanners, securityChecks, passportChecks, stampsCheck, boarding, false);

        return getBatchStatistics();
    }

    private void stopWarmup(MsqTime time) {
        luggageChecks.stopWarmup(time);
        checkInDesks.stopWarmup(time);
        boardingPassScanners.stopWarmup(time);
        securityChecks.stopWarmup(time);
        passportChecks.stopWarmup(time);
        stampsCheck.stopWarmup(time);
        boarding.stopWarmup(time);
    }

    private List<BatchStatistics> getBatchStatistics() {
        List<BatchStatistics> batchStatistics = new ArrayList<>(luggageChecks.getBatchStatistics());
        batchStatistics.addAll(checkInDesks.getBatchStatistics());
        batchStatistics.add(securityChecks.getBatchStatistics());
        batchStatistics.add(passportChecks.getBatchStatistics());
        batchStatistics.add(stampsCheck.getBatchStatistics());
        batchStatistics.addAll(boarding.getBatchStatistics());
        return batchStatistics;
    }


    private void initCenters(boolean approximateServiceAsExponential) {
        CenterFactory factory = new CenterFactory();
        luggageChecks = factory.createLuggageChecks(approximateServiceAsExponential);
        checkInDesks = factory.createCheckinDeskOthers(approximateServiceAsExponential);
        boardingPassScanners = factory.createBoardingPassScanners(approximateServiceAsExponential);
        securityChecks = factory.createSecurityChecks(approximateServiceAsExponential);
        passportChecks = factory.createPassportChecks(approximateServiceAsExponential);
        stampsCheck = factory.createStampsCheck(approximateServiceAsExponential);
        boarding = factory.createBoardingOthers(approximateServiceAsExponential);
    }

    private void resetCenters(Rngs rngs) {
        checkInDesks.reset(rngs);
        boardingPassScanners.reset(rngs);
        securityChecks.reset(rngs);
        passportChecks.reset(rngs);
        stampsCheck.reset(rngs);
        boarding.reset(rngs);
    }

    private void processCurrentEvent(MsqEvent event, MsqTime msqTime, EventQueue events) {
        switch (event.type) {
            case ARRIVAL_LUGGAGE_CHECK:
                luggageChecks.processArrival(event, msqTime, events);
                break;
            case LUGGAGE_CHECK_DONE:
                luggageChecks.processCompletion(event, msqTime, events);
                break;
            case ARRIVAL_CHECK_IN:
                checkInDesks.processArrival(event, msqTime, events);
                break;
            case CHECK_IN_DONE:
                checkInDesks.processCompletion(event, msqTime, events);
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
    }

    private void modelVerification(String simulationType) {
        List<AnalyticalComputation.AnalyticalResult> analyticalResultList = computeAnalyticalResults(simulationType);
        System.out.println("an res size: "+ analyticalResultList.size());

        // Compare results and verifications and save comparison result
        List<MeanStatistics> batchMeanStatisticsList = aggregateBatchMeanStatistics();
        System.out.println("batch mean statistics: "+ batchMeanStatisticsList.size());

        List<Comparison.ComparisonResult> comparisonResultList = compareResults(simulationType, analyticalResultList, batchMeanStatisticsList);

        List<ConfidenceIntervals> confidenceIntervalsList = aggregateConfidenceIntervals();

        List<Verification.VerificationResult> verificationResultList = verifyConfidenceIntervals(simulationType, batchMeanStatisticsList, comparisonResultList, confidenceIntervalsList);

        printFinalResults(verificationResultList);
    }

    private List<MeanStatistics> aggregateBatchMeanStatistics() {
        List<MeanStatistics> batchMeanStatisticsList = new ArrayList<>();

        batchMeanStatisticsList.addAll(luggageChecks.getBatchMeanStatistics());
        batchMeanStatisticsList.addAll(checkInDesks.getBatchMeanStatistics());
        batchMeanStatisticsList.add(boardingPassScanners.getBatchMeanStatistics());
        batchMeanStatisticsList.add(securityChecks.getBatchMeanStatistics());
        batchMeanStatisticsList.add(passportChecks.getBatchMeanStatistics());
        batchMeanStatisticsList.add(stampsCheck.getBatchMeanStatistics());
        batchMeanStatisticsList.addAll(boarding.getBatchMeanStatistics());
        return batchMeanStatisticsList;
    }

    private List<ConfidenceIntervals> aggregateConfidenceIntervals() {
        List<ConfidenceIntervals> confidenceIntervalsList = new ArrayList<>();

        confidenceIntervalsList.addAll(createConfidenceIntervalsList(luggageChecks.getBatchStatistics()));
        confidenceIntervalsList.addAll(createConfidenceIntervalsList(checkInDesks.getBatchStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(boardingPassScanners.getBatchStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(securityChecks.getBatchStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(passportChecks.getBatchStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(stampsCheck.getBatchStatistics()));
        confidenceIntervalsList.addAll(createConfidenceIntervalsList(boarding.getBatchStatistics()));

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

        luggageChecks.writeBatchStats(simulationType);
        checkInDesks.writeBatchStats(simulationType);
        boardingPassScanners.writeBatchStats(simulationType);
        securityChecks.writeBatchStats(simulationType);
        passportChecks.writeBatchStats(simulationType);
        stampsCheck.writeBatchStats(simulationType);
        boarding.writeBatchStats(simulationType);
    }

    private void updateAreas(MsqTime msqTime) {
        luggageChecks.setAreaForAll(msqTime);
        checkInDesks.setAreaForAll(msqTime);
        boardingPassScanners.setArea(msqTime);
        securityChecks.setArea(msqTime);
        passportChecks.setArea(msqTime);
        stampsCheck.setArea(msqTime);
        boarding.setAreaForAll(msqTime);
    }

    private long getMinimumNumberOfJobsServedByCenters() {
        return Arrays.stream(luggageChecks.getTotalNumberOfJobsServed()).min().orElseThrow();
    }

    private boolean isDone() {
       return luggageChecks.isDone()
               && checkInDesks.isDone()
               && securityChecks.isDone()
               && passportChecks.isDone()
               && stampsCheck.isDone()
               && boardingPassScanners.isDone()
               && boarding.isDone();
    }
}
