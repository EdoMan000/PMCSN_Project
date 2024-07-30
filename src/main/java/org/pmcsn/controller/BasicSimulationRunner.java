package org.pmcsn.controller;


import org.pmcsn.conf.CenterFactory;
import org.pmcsn.conf.Config;
import org.pmcsn.WelchPlot;
import org.pmcsn.centers.*;
import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;
import org.pmcsn.model.Statistics.MeanStatistics;
import org.pmcsn.utils.Verification.Result;

import java.io.IOException;
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
    private final Config config = new Config();


    public void runBasicSimulation(boolean approximateServiceAsExponential) throws Exception {
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
            System.out.println("\nRunning Basic Simulation with Exponential Service...");
        }else{
            System.out.println("\nRunning Basic Simulation...");
        }

        //Rng setting the seed
        long[] seeds = new long[1024];
        seeds[0] = SEED;
        Rngs rngs = new Rngs();

        int runsNumber = config.getInt("general", "runsNumber");

        final List<Observations> luggageObservations = new ArrayList<>();
        for (int i = 0; i < config.getInt("luggageChecks", "numberOfCenters"); i++) {
            luggageObservations.add(new Observations("%s_%d".formatted(config.getString("luggageChecks", "centerName"), i+1), runsNumber));
        }
        final List<Observations> checkInDeskTargetObservations = new ArrayList<>();
        for (int i = 0; i < config.getInt("checkInDeskTarget", "serversNumber"); i++) {
            checkInDeskTargetObservations.add(new Observations("%s_%d".formatted(config.getString("checkInDeskTarget", "centerName"), i+1), runsNumber));
        }
        final List<List<Observations>> checkinDeskOthersObservations = new ArrayList<>();
        for (int i = 0; i < config.getInt("checkInDeskOthers", "numberOfCenters"); i++) {
            checkinDeskOthersObservations.add(new ArrayList<>());
            for (int j = 0; j < config.getInt("checkInDeskOthers", "serversNumber"); j++) {
                checkinDeskOthersObservations.get(i).add(new Observations("%s_%d_%d".formatted(config.getString("checkInDeskOthers", "centerName"), i+1, j+1), runsNumber));
            }
        }
        final List<Observations> boardingPassScannerObservations = new ArrayList<>();
        for (int i = 0; i < config.getInt("boardingPassScanners", "serversNumber"); i++) {
            boardingPassScannerObservations.add(new Observations("%s_%d".formatted(config.getString("boardingPassScanners", "centerName"), i+1), runsNumber));
        }
        final List<Observations> securityCheckObservations = new ArrayList<>();
        for (int i = 0; i < config.getInt("securityChecks", "serversNumber"); i++) {
            securityCheckObservations.add(new Observations("%s_%d".formatted(config.getString("securityChecks", "centerName"), i+1), runsNumber));
        }
        final List<Observations> passportCheckObservations = new ArrayList<>();
        for (int i = 0; i < config.getInt("passportChecks", "serversNumber"); i++) {
            passportCheckObservations.add(new Observations("%s_%d".formatted(config.getString("passportChecks", "centerName"), i+1), runsNumber));
        }
        Observations stampsCheckObservations = new Observations("STAMPS_CHECK", runsNumber);
        final List<Observations> boardingObservations = new ArrayList<>();
        for (int i = 0; i < config.getInt("boarding", "serversNumber"); i++) {
            boardingObservations.add(new Observations("%s_%d".formatted(config.getString("boarding", "centerName"), i+1), runsNumber));
        }
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

            // Initialize other centers
            checkInDesksTarget.reset(rngs);
            checkInDesksOthers.reset(rngs);
            boardingPassScanners.reset(rngs);
            securityChecks.reset(rngs);
            passportChecks.reset(rngs);
            stampsCheck.reset(rngs);
            boarding.reset(rngs);

            MsqEvent event;

            int skip = 1;
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
                        if (eventCount % skip == 0)
                            luggageChecks.updateObservations(luggageObservations, i);
                        break;
                    case ARRIVAL_CHECK_IN_TARGET:
                        checkInDesksTarget.processArrival(event, msqTime, queue);
                        break;
                    case CHECK_IN_TARGET_DONE:
                        checkInDesksTarget.processCompletion(event, msqTime, queue);
                        if (eventCount % skip == 0)
                            checkInDesksTarget.updateObservations(checkInDeskTargetObservations, i);
                        break;
                    case ARRIVAL_CHECK_IN_OTHERS:
                        checkInDesksOthers.processArrival(event, msqTime, queue);
                        break;
                    case CHECK_IN_OTHERS_DONE:
                        checkInDesksOthers.processCompletion(event, msqTime, queue);
                        if (eventCount % skip == 0)
                            checkInDesksOthers.updateObservations(checkinDeskOthersObservations, i);
                        break;
                    case ARRIVAL_BOARDING_PASS_SCANNERS:
                        boardingPassScanners.processArrival(event, msqTime, queue);
                        break;
                    case BOARDING_PASS_SCANNERS_DONE:
                        boardingPassScanners.processCompletion(event, msqTime, queue);
                        if (eventCount % skip == 0)
                            boardingPassScanners.updateObservations(boardingPassScannerObservations, i);
                        break;
                    case ARRIVAL_SECURITY_CHECK:
                        securityChecks.processArrival(event, msqTime, queue);
                        break;
                    case SECURITY_CHECK_DONE:
                        securityChecks.processCompletion(event, msqTime, queue);
                        if (eventCount % skip == 0)
                            securityChecks.updateObservations(securityCheckObservations, i);
                        break;
                    case ARRIVAL_PASSPORT_CHECK:
                        passportChecks.processArrival(event, msqTime, queue);
                        break;
                    case PASSPORT_CHECK_DONE:
                        passportChecks.processCompletion(event, msqTime, queue);
                        passportChecks.updateObservations(passportCheckObservations, i);
                            passportChecks.updateObservations(passportCheckObservations, i);
                        break;
                    case ARRIVAL_STAMP_CHECK:
                        stampsCheck.processArrival(event, msqTime, queue);
                        break;
                    case STAMP_CHECK_DONE:
                        stampsCheck.processCompletion(event, msqTime, queue);
                        passportChecks.updateObservations(passportCheckObservations, i);
                            stampsCheck.updateObservations(stampsCheckObservations, i);
                        break;
                    case ARRIVAL_BOARDING:
                        boarding.processArrival(event, msqTime, queue);
                        break;
                    case BOARDING_DONE:
                        boarding.processCompletion(event, msqTime, queue);
                        passportChecks.updateObservations(passportCheckObservations, i);
                            boarding.updateObservations(boardingObservations, i);
                        break;
                }
                // Saving observations to compute warm up period boundaries
                //luggageChecks.updateObservations(luggageObservations, i);
                //checkInDesksTarget.updateObservations(checkInDeskTargetObservations, i);
                //checkInDesksOthers.updateObservations(checkinDeskOthersObservations, i);
                //boardingPassScanners.updateObservations(boardingPassScannerObservations, i);
                //securityChecks.updateObservations(securityCheckObservations, i);
                //passportChecks.updateObservations(passportCheckObservations, i);
                //stampsCheck.updateObservations(stampsCheckObservations, i);
                //boarding.updateObservations(boardingObservations, i);

                eventCount++;

                number = luggageChecks.getNumberOfJobsInNode() + checkInDesksTarget.getNumberOfJobsInNode() + checkInDesksOthers.getNumberOfJobsInNode() + boarding.getNumberOfJobsInNode()
                        + boardingPassScanners.getNumberOfJobsInNode() + securityChecks.getNumberOfJobsInNode() + passportChecks.getNumberOfJobsInNode() + stampsCheck.getNumberOfJobsInNode() + boarding.getNumberOfJobsInNode();

            }

            System.out.println("EVENT COUNT FOR RUN N°"+i+": " + eventCount);

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
        WelchPlot.writeObservations(SIMULATION_TYPE, luggageObservations);
        WelchPlot.writeObservations(SIMULATION_TYPE, checkInDeskTargetObservations);
        WelchPlot.writeObservations(checkinDeskOthersObservations, SIMULATION_TYPE);
        WelchPlot.writeObservations(SIMULATION_TYPE, boardingObservations);
        WelchPlot.writeObservations(SIMULATION_TYPE, boardingPassScannerObservations);
        WelchPlot.writeObservations(SIMULATION_TYPE, securityCheckObservations);
        WelchPlot.writeObservations(SIMULATION_TYPE, passportCheckObservations);
        WelchPlot.writeObservations(SIMULATION_TYPE, List.of(stampsCheckObservations));

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
