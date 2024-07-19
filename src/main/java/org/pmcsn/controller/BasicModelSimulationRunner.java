package org.pmcsn.controller;


import org.pmcsn.centers.*;
import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class BasicModelSimulationRunner {
    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Population
     */

    // Constants
    private static final int START = 0;
    private static final long SEED = 123456789L;


    public void runBasicModelSimulation() {
        System.out.println("Running Basic Model Simulation...");

        //Rng setting the seed
        long[] seeds = new long[1024];
        seeds[0] = SEED;
        Rngs rngs = new Rngs();

        // Create lists to store events for statistics
        List<MsqEvent> allEvents = new ArrayList<>();
        MsqTime finalMsqTime = new MsqTime();

        // Declare variables for centers
        LuggageChecks luggageChecks = null;
        CheckInDesksTarget checkInDesksTarget = null;
        CheckInDesksOthers checkInDesksOthers = null;
        BoardingPassScanners boardingPassScanners = null;
        SecurityChecks securityChecks = null;
        PassportChecks passportChecks = null;
        StampsCheck stampsCheck = null;
        Boarding boardingTarget = null;

        for (int i = 0; i < 150; i++) {

            boolean stopArrivals = false;
            double sarrival = START;
            long number = 1;

            rngs.plantSeeds(seeds[i]);

            //Msq initialization
            MsqTime msqTime = new MsqTime();
            msqTime.current = START;
            List<MsqEvent> events = new ArrayList<>();

            // Initialize LuggageChecks
            luggageChecks = new LuggageChecks(rngs, sarrival);

            //generating first arrival
            double time = luggageChecks.getArrival();
            events.add(new MsqEvent(time, true, EventType.ARRIVAL_LUGGAGE_CHECK));

            // TODO: Define CENTER_INDEX variables in all centers so that we have no collisions
            // Initialize other centers
            checkInDesksTarget = new CheckInDesksTarget(rngs);
            checkInDesksOthers = new CheckInDesksOthers(rngs);
            boardingPassScanners = new BoardingPassScanners(rngs);
            securityChecks = new SecurityChecks(rngs);
            passportChecks = new PassportChecks(rngs);
            stampsCheck = new StampsCheck(rngs);
            boardingTarget = new Boarding(rngs);

            MsqEvent event;

            while (!luggageChecks.isEndOfArrivals() && number != 0) {

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
                boardingTarget.setArea(msqTime);

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
                        boardingTarget.processArrival(event, msqTime, events);
                        break;
                    case BOARDING_DONE:
                        boardingTarget.processCompletion(event, msqTime, events);
                        break;
                }

                number = luggageChecks.getNumberOfJobsInNode() + checkInDesksTarget.getNumberOfJobsInNode() + checkInDesksOthers.getNumberOfJobsInNode() + boardingTarget.getNumberOfJobsInNode()
                        + boardingPassScanners.getNumberOfJobsInNode() + securityChecks.getNumberOfJobsInNode() + passportChecks.getNumberOfJobsInNode() + stampsCheck.getNumberOfJobsInNode() + boardingTarget.getNumberOfJobsInNode();

            }

            // Collect all events for statistics
            allEvents.addAll(events);
            finalMsqTime = msqTime;

            // Generating next seed
            rngs.selectStream(255);
            seeds[i + 1] = rngs.getSeed();
        }

        //TODO understand what replicationIndex parameter is used for
        luggageChecks.computeAndPrintStats(0, finalMsqTime, allEvents);
        checkInDesksTarget.computeAndPrintStats(0, finalMsqTime, allEvents);
        checkInDesksOthers.computeAndPrintStats(0, finalMsqTime, allEvents);
        boardingPassScanners.computeAndPrintStats(0, finalMsqTime, allEvents);
        securityChecks.computeAndPrintStats(0, finalMsqTime, allEvents);
        passportChecks.computeAndPrintStats(0, finalMsqTime, allEvents);
        stampsCheck.computeAndPrintStats(0, finalMsqTime, allEvents);
        boardingTarget.computeAndPrintStats(0, finalMsqTime, allEvents);
    }

    private MsqEvent getNextEvent(List<MsqEvent> events) {

        if (events == null || events.isEmpty()) {
            return null; // or throw an exception depending on your use case
        }

        List<MsqEvent> eventsWithPriority = new ArrayList<>(events);
        eventsWithPriority.removeIf(event -> !event.hasPriority);
        eventsWithPriority.sort(Comparator.comparing(MsqEvent::getTime));
        MsqEvent minEventPrio = eventsWithPriority.getFirst();

        List<MsqEvent> eventsWithoutPriority = new ArrayList<>(events);
        eventsWithoutPriority.removeIf(event -> event.hasPriority);
        eventsWithoutPriority.sort(Comparator.comparing(MsqEvent::getTime));
        MsqEvent minEvent = eventsWithoutPriority.getFirst();

        // minEventPrio has the lowest time of all events
        if (minEventPrio.time < minEvent.time) {
            minEvent = minEventPrio;

            // minEventPrio has the same time of other events (but they have equal or following type)
        } else if (minEventPrio.time == minEvent.time && minEventPrio.type.ordinal() <= minEvent.type.ordinal()) {
            minEvent = minEventPrio;
        }
        // minEventPrio has the same time of other events but they have prior type so they need to be processed before
        // (minEvent does not need to be changed)


        // Now I can check as before
        for (MsqEvent event : events) {
            if (event.getTime() > minEvent.getTime()) {
                break; // Since the list is sorted by time, no need to check further

                // note that minEvent is changed only if the type is prior the current type (does not interfere with priority)
            } else if (event.getTime() == minEvent.getTime() && event.type.ordinal() < minEvent.type.ordinal()) {
                minEvent = event;
            }
        }

        return minEvent;

    }

}
