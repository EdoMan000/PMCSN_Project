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

public class BasicSimulationRunner {
    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Population
     */

    // Constants
    private static final int START = 0;
    private static final long SEED = 123456789L;
    private static final String SIMULATION_TYPE = "BASIC_SIMULATION";


    public void runBasicSimulation() {
        System.out.println("\nRunning Basic Simulation...");

        //Rng setting the seed
        long[] seeds = new long[1024];
        seeds[0] = SEED;
        Rngs rngs = new Rngs();

        // Declare variables for centers
        LuggageChecks luggageChecks = new LuggageChecks(6, (24 * 60) / 6300.0, 1);
        CheckInDesksTarget checkInDesksTarget = new CheckInDesksTarget();
        CheckInDesksOthers checkInDesksOthers = new CheckInDesksOthers();
        BoardingPassScanners boardingPassScanners = new BoardingPassScanners();
        SecurityChecks securityChecks = new SecurityChecks();
        PassportChecks passportChecks = new PassportChecks();
        StampsCheck stampsCheck = new StampsCheck(0.1);
        Boarding boarding = new Boarding();

        for (int i = 0; i < 150; i++) {

            double sarrival = START;
            long number = 1;

            rngs.plantSeeds(seeds[i]);

            //Msq initialization
            MsqTime msqTime = new MsqTime();
            msqTime.current = START;
            List<MsqEvent> events = new ArrayList<>();

            // Initialize LuggageChecks
            luggageChecks.reset(rngs, sarrival);

            //generating first arrival
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

            MsqEvent event;

            int eventCount = 0;

            // need to use OR because both the conditions should be false
            while (!luggageChecks.isEndOfArrivals() || number != 0) {
                // TODO: getNextEvent dovrebbe rimuovere l'evento dalla lista
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
        // Writing statistics csv with data from all runs
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

        // controllo di consistenza sul numero di jobs processati
        long jobServedEntrances = luggageChecks.getTotalNumberOfJobsServed();
        System.out.println("TOT Luggage Checks Jobs Served = " + jobServedEntrances);

        long checkInDesksTargetJobsServed = checkInDesksTarget.getJobsServed();
        System.out.println("Check-In Desks Target: Jobs Served = " + checkInDesksTargetJobsServed);

        long jobServedCheckIns = checkInDesksTargetJobsServed;
        for (int i = 0; i < checkInDesksOthers.numberOfCenters; i++) {
            long jobsServed = checkInDesksOthers.getJobsServed(i);
            jobServedCheckIns += jobsServed;
            System.out.println("Check-In Desks Others Center " + i + ": Jobs Served = " + jobsServed);
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
