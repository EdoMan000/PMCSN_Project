package org.pmcsn.controller;


import org.pmcsn.conf.CenterFactory;
import org.pmcsn.conf.Config;
import org.pmcsn.utils.AnalyticalComputation;
import org.pmcsn.utils.Comparison;
import org.pmcsn.utils.Verification;
import org.pmcsn.utils.WelchPlot;
import org.pmcsn.centers.*;
import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.ArrayList;
import java.util.List;

import static org.pmcsn.utils.Comparison.compareResults;
import static org.pmcsn.utils.PrintUtils.printFinalResults;
import static org.pmcsn.utils.PrintUtils.printJobsServedByNodes;
import static org.pmcsn.utils.AnalyticalComputation.computeAnalyticalResults;
import static org.pmcsn.utils.Verification.verifyConfidenceIntervals;

public class BasicSimulationRunner {
    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Population
     */

    // Constants
    private static final int START = 0;
    private static final long SEED = 123456789L;

    private final Config config = new Config();
    private LuggageChecks luggageChecks;
    private CheckInDesksTarget checkInDesksTarget;
    private CheckInDesksOthers checkInDesksOthers;
    private BoardingPassScanners boardingPassScanners;
    private SecurityChecks securityChecks;
    private PassportChecks passportChecks;
    private StampsCheck stampsCheck;
    private BoardingTarget boardingTarget;
    private BoardingOthers boardingOthers;
    private final List<Observations> luggageObservations = new ArrayList<>();
    private final List<Observations> checkInDeskTargetObservations = new ArrayList<>();
    private final List<List<Observations>> checkinDeskOthersObservations = new ArrayList<>();
    private final List<Observations> boardingPassScannerObservations = new ArrayList<>();
    private final List<Observations> securityCheckObservations = new ArrayList<>();
    private final List<Observations> passportCheckObservations = new ArrayList<>();
    private final List<Observations> boardingTargetObservations = new ArrayList<>();
    private Observations stampsCheckObservations;
    private final List<List<Observations>> boardingOthersObservations = new ArrayList<>();

    public void runBasicSimulation(boolean approximateServiceAsExponential) throws Exception {
        runBasicSimulation(approximateServiceAsExponential, true);
    }

    public void runBasicSimulation(boolean approximateServiceAsExponential, boolean shouldTrackObservations) throws Exception {
        initCenters(approximateServiceAsExponential);

        String simulationType;
        if(approximateServiceAsExponential){
            simulationType = "BASIC_SIMULATION_EXPONENTIAL";
        }else{
            simulationType = "BASIC_SIMULATION";
        }
        System.out.println("\nRUNNING " + simulationType + "...");

        //Rng setting the seed
        long[] seeds = new long[1024];
        seeds[0] = SEED;
        Rngs rngs = new Rngs();

        if (shouldTrackObservations) {
            initObservations();
        }

        int runsNumber = config.getInt("general", "runsNumber");
        for (int i = 0; i < runsNumber; i++) {
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

            resetCenters(rngs); // Initialize other centers

            MsqEvent event;
            int skip = 3;
            int eventCount = 0;

            // need to use OR because all the conditions should be false
            while (!luggageChecks.isEndOfArrivals() || !queue.isEmpty() || number != 0) {
                event = queue.pop(); // Retrieving next event to be processed
                msqTime.next = event.time;
                updateAreas(msqTime); // Updating areas
                msqTime.current = msqTime.next; // Advancing the clock

                processCurrentEvent(shouldTrackObservations, event, msqTime, queue, eventCount, skip, i); // Processing the event based on its type

                eventCount++;

                number = getTotalNumberOfJobsInSystem();
            }

            // Writing observations for current run
            if (shouldTrackObservations) {
                writeObservations(simulationType);
                resetObservations();
            }

            //System.out.println("EVENT COUNT FOR RUN N°"+i+": " + eventCount);

            saveAllStats(); // Saving statistics for current run

            // Generating next seed
            rngs.selectStream(config.getInt("general", "seedStreamIndex"));
            seeds[i + 1] = rngs.getSeed();
        }

        System.out.println(simulationType + " HAS JUST FINISHED.");

        if (shouldTrackObservations) {
            WelchPlot.welchPlot("csvFiles/BASIC_SIMULATION_EXPONENTIAL/observations");
        }

        // Writing statistics csv with data from all runs
        writeAllStats(simulationType);

        if(approximateServiceAsExponential) {
            modelVerification(simulationType); // Computing and writing verifications stats csv
        }

        printJobsServedByNodes(luggageChecks, checkInDesksTarget, checkInDesksOthers, boardingPassScanners, securityChecks, passportChecks, stampsCheck, boardingTarget, boardingOthers, false);
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

    private void resetCenters(Rngs rngs) {
        checkInDesksTarget.reset(rngs);
        checkInDesksOthers.reset(rngs);
        boardingPassScanners.reset(rngs);
        securityChecks.reset(rngs);
        passportChecks.reset(rngs);
        stampsCheck.reset(rngs);
        boardingTarget.reset(rngs);
        boardingOthers.reset(rngs);
    }

    private void processCurrentEvent(boolean shouldTrackObservations, MsqEvent event, MsqTime msqTime, EventQueue queue, int eventCount, int skip, int i) {
        switch (event.type) {
            case ARRIVAL_LUGGAGE_CHECK:
                luggageChecks.processArrival(event, msqTime, queue);
                break;
            case LUGGAGE_CHECK_DONE:
                luggageChecks.processCompletion(event, msqTime, queue);
                if (shouldTrackObservations && eventCount % skip == 0)
                    luggageChecks.updateObservations(luggageObservations, i);
                break;
            case ARRIVAL_CHECK_IN_TARGET:
                checkInDesksTarget.processArrival(event, msqTime, queue);
                break;
            case CHECK_IN_TARGET_DONE:
                checkInDesksTarget.processCompletion(event, msqTime, queue);
                if (shouldTrackObservations && eventCount % skip == 0)
                    checkInDesksTarget.updateObservations(checkInDeskTargetObservations, i);
                break;
            case ARRIVAL_CHECK_IN_OTHERS:
                checkInDesksOthers.processArrival(event, msqTime, queue);
                break;
            case CHECK_IN_OTHERS_DONE:
                checkInDesksOthers.processCompletion(event, msqTime, queue);
                if (shouldTrackObservations && eventCount % skip == 0)
                    checkInDesksOthers.updateObservations(checkinDeskOthersObservations, i);
                break;
            case ARRIVAL_BOARDING_PASS_SCANNERS:
                boardingPassScanners.processArrival(event, msqTime, queue);
                break;
            case BOARDING_PASS_SCANNERS_DONE:
                boardingPassScanners.processCompletion(event, msqTime, queue);
                if (shouldTrackObservations && eventCount % skip == 0)
                    boardingPassScanners.updateObservations(boardingPassScannerObservations, i);
                break;
            case ARRIVAL_SECURITY_CHECK:
                securityChecks.processArrival(event, msqTime, queue);
                break;
            case SECURITY_CHECK_DONE:
                securityChecks.processCompletion(event, msqTime, queue);
                if (shouldTrackObservations && eventCount % skip == 0)
                    securityChecks.updateObservations(securityCheckObservations, i);
                break;
            case ARRIVAL_PASSPORT_CHECK:
                passportChecks.processArrival(event, msqTime, queue);
                break;
            case PASSPORT_CHECK_DONE:
                passportChecks.processCompletion(event, msqTime, queue);
                if (shouldTrackObservations && eventCount % skip == 0)
                    passportChecks.updateObservations(passportCheckObservations, i);
                break;
            case ARRIVAL_STAMP_CHECK:
                stampsCheck.processArrival(event, msqTime, queue);
                break;
            case STAMP_CHECK_DONE:
                stampsCheck.processCompletion(event, msqTime, queue);
                if (shouldTrackObservations && eventCount % skip == 0)
                    stampsCheck.updateObservations(stampsCheckObservations, i);
                break;
            case ARRIVAL_BOARDING_TARGET:
                boardingTarget.processArrival(event, msqTime, queue);
                break;
            case BOARDING_TARGET_DONE:
                boardingTarget.processCompletion(event, msqTime, queue);
                if (shouldTrackObservations && eventCount % skip == 0)
                    boardingTarget.updateObservations(boardingTargetObservations, i);
                break;
            case ARRIVAL_BOARDING_OTHERS:
                boardingOthers.processArrival(event, msqTime, queue);
                break;
            case BOARDING_OTHERS_DONE:
                boardingOthers.processCompletion(event, msqTime, queue);
                if (shouldTrackObservations && eventCount % skip == 0)
                    boardingOthers.updateObservations(boardingOthersObservations, i);
                break;
        }
    }

    private void saveAllStats() {
        luggageChecks.saveStats();
        checkInDesksTarget.saveStats();
        checkInDesksOthers.saveStats();
        boardingPassScanners.saveStats();
        securityChecks.saveStats();
        passportChecks.saveStats();
        stampsCheck.saveStats();
        boardingTarget.saveStats();
        boardingOthers.saveStats();
    }

    private void modelVerification(String simulationType) {
        List<AnalyticalComputation.AnalyticalResult> analyticalResultList = computeAnalyticalResults(simulationType);

        // Compare results and verifications and save comparison result
        List<MeanStatistics> meanStatisticsList = aggregateMeanStatistics();

        List<Comparison.ComparisonResult> comparisonResultList = compareResults(simulationType, analyticalResultList, meanStatisticsList);

        List<ConfidenceIntervals> confidenceIntervalsList = aggregateConfidenceIntervals();

        List<Verification.VerificationResult> verificationResultList = verifyConfidenceIntervals(simulationType, comparisonResultList, confidenceIntervalsList);

        printFinalResults(comparisonResultList, verificationResultList);
    }

    private List<MeanStatistics> aggregateMeanStatistics() {
        List<MeanStatistics> meanStatisticsList = new ArrayList<>();

        meanStatisticsList.addAll(luggageChecks.getMeanStatistics());
        meanStatisticsList.add(checkInDesksTarget.getMeanStatistics());
        meanStatisticsList.addAll(checkInDesksOthers.getMeanStatistics());
        meanStatisticsList.add(boardingPassScanners.getMeanStatistics());
        meanStatisticsList.add(securityChecks.getMeanStatistics());
        meanStatisticsList.add(passportChecks.getMeanStatistics());
        meanStatisticsList.add(stampsCheck.getMeanStatistics());
        meanStatisticsList.add(boardingTarget.getMeanStatistics());
        meanStatisticsList.addAll(boardingOthers.getMeanStatistics());
        return meanStatisticsList;
    }

    private List<ConfidenceIntervals> aggregateConfidenceIntervals() {
        List<ConfidenceIntervals> confidenceIntervalsList = new ArrayList<>();

        confidenceIntervalsList.addAll(createConfidenceIntervalsList(luggageChecks.getStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(checkInDesksTarget.getStatistics()));
        confidenceIntervalsList.addAll(createConfidenceIntervalsList(checkInDesksOthers.getStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(boardingPassScanners.getStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(securityChecks.getStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(passportChecks.getStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(stampsCheck.getStatistics()));
        confidenceIntervalsList.add(createConfidenceIntervals(boardingTarget.getStatistics()));
        confidenceIntervalsList.addAll(createConfidenceIntervalsList(boardingOthers.getStatistics()));

        return confidenceIntervalsList;
    }

    private ConfidenceIntervals createConfidenceIntervals(BasicStatistics stats) {
        return new ConfidenceIntervals(
                stats.meanResponseTimeList, stats.meanQueueTimeList, stats.meanServiceTimeList,
                stats.meanSystemPopulationList, stats.meanQueuePopulationList, stats.meanUtilizationList, stats.lambdaList
        );
    }

    private List<ConfidenceIntervals> createConfidenceIntervalsList(List<BasicStatistics> statisticsList) {
        List<ConfidenceIntervals> confidenceIntervalsList = new ArrayList<>();
        for (BasicStatistics stats : statisticsList) {
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

    private long getTotalNumberOfJobsInSystem() {
        return luggageChecks.getTotalNumberOfJobsInNode() +
                checkInDesksTarget.getNumberOfJobsInNode() +
                checkInDesksOthers.getTotalNumberOfJobsInNode() +
                boardingPassScanners.getNumberOfJobsInNode() +
                securityChecks.getNumberOfJobsInNode() +
                passportChecks.getNumberOfJobsInNode() +
                stampsCheck.getNumberOfJobsInNode() +
                boardingTarget.getNumberOfJobsInNode() +
                boardingOthers.getTotalNumberOfJobsInNode();
    }

    private void updateAreas(MsqTime msqTime) {
        // Updating the areas
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

    private void initObservations() {

        int runsNumber = config.getInt("general", "runsNumber");
        for (int i = 0; i < config.getInt("luggageChecks", "numberOfCenters"); i++) {
            luggageObservations.add(new Observations("%s_%d".formatted(config.getString("luggageChecks", "centerName"), i+1), runsNumber));
        }
        for (int i = 0; i < config.getInt("checkInDeskTarget", "serversNumber"); i++) {
            checkInDeskTargetObservations.add(new Observations("%s_%d".formatted(config.getString("checkInDeskTarget", "centerName"), i+1), runsNumber));
        }
        for (int i = 0; i < config.getInt("checkInDeskOthers", "numberOfCenters"); i++) {
            checkinDeskOthersObservations.add(new ArrayList<>());
            for (int j = 0; j < config.getInt("checkInDeskOthers", "serversNumber"); j++) {
                checkinDeskOthersObservations.get(i).add(new Observations("%s_%d_%d".formatted(config.getString("checkInDeskOthers", "centerName"), i+1, j+1), runsNumber));
            }
        }
        for (int i = 0; i < config.getInt("boardingPassScanners", "serversNumber"); i++) {
            boardingPassScannerObservations.add(new Observations("%s_%d".formatted(config.getString("boardingPassScanners", "centerName"), i+1), runsNumber));
        }
        for (int i = 0; i < config.getInt("securityChecks", "serversNumber"); i++) {
            securityCheckObservations.add(new Observations("%s_%d".formatted(config.getString("securityChecks", "centerName"), i+1), runsNumber));
        }
        for (int i = 0; i < config.getInt("passportChecks", "serversNumber"); i++) {
            passportCheckObservations.add(new Observations("%s_%d".formatted(config.getString("passportChecks", "centerName"), i+1), runsNumber));
        }
        stampsCheckObservations = new Observations("STAMPS_CHECK", runsNumber);
        for (int i = 0; i < config.getInt("boardingTarget", "serversNumber"); i++) {
            boardingTargetObservations.add(new Observations("%s_%d".formatted(config.getString("boardingTarget", "centerName"), i+1), runsNumber));
        }
        for (int i = 0; i < config.getInt("boardingOthers", "numberOfCenters"); i++) {
            boardingOthersObservations.add(new ArrayList<>());
            for (int j = 0; j < config.getInt("boardingOthers", "serversNumber"); j++) {
                boardingOthersObservations.get(i).add(new Observations("%s_%d_%d".formatted(config.getString("boardingOthers", "centerName"), i+1, j+1), runsNumber));
            }
        }
    }

    private void resetObservations() {
        luggageObservations.forEach(Observations::reset);
        checkInDeskTargetObservations.forEach(Observations::reset);
        checkinDeskOthersObservations.forEach(x -> x.forEach(Observations::reset));
        boardingPassScannerObservations.forEach(Observations::reset);
        securityCheckObservations.forEach(Observations::reset);
        passportCheckObservations.forEach(Observations::reset);
        stampsCheckObservations.reset();
        boardingTargetObservations.forEach(Observations::reset);
        boardingOthersObservations.forEach(x -> x.forEach(Observations::reset));
    }

    private void writeObservations(String simulationType) {
        // Computing warm up period boundaries
        WelchPlot.writeObservations(simulationType, luggageObservations);
        WelchPlot.writeObservations(simulationType, checkInDeskTargetObservations);
        WelchPlot.writeObservations(checkinDeskOthersObservations, simulationType);
        WelchPlot.writeObservations(boardingOthersObservations, simulationType);
        WelchPlot.writeObservations(simulationType, boardingTargetObservations);
        WelchPlot.writeObservations(simulationType, boardingPassScannerObservations);
        WelchPlot.writeObservations(simulationType, securityCheckObservations);
        WelchPlot.writeObservations(simulationType, passportCheckObservations);
        WelchPlot.writeObservations(simulationType, List.of(stampsCheckObservations));
    }
}
