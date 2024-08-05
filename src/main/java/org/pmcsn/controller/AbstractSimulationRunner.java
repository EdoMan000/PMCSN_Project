package org.pmcsn.controller;

import org.pmcsn.centers.*;
import org.pmcsn.conf.Config;
import org.pmcsn.model.EventQueue;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;

public class AbstractSimulationRunner {
    private LuggageChecks luggageChecks;
    private CheckInDesksTarget checkInDesksTarget;
    private CheckInDesksOthers checkInDesksOthers;
    private BoardingPassScanners boardingPassScanners;
    private SecurityChecks securityChecks;
    private PassportChecks passportChecks;
    private StampsCheck stampsCheck;
    private BoardingTarget boardingTarget;
    private BoardingOthers boardingOthers;
    private final EventQueue eventQueue = new EventQueue();
    private final Config config = new Config();

    protected void processCurrentEvent(MsqEvent event, MsqTime msqTime, EventQueue events) {
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
}
