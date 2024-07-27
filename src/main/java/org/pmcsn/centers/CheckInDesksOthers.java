package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.libraries.Rvgs;
import org.pmcsn.model.*;

import java.util.*;

import static org.pmcsn.model.Statistics.computeMean;
import static org.pmcsn.utils.Distributions.*;
import static org.pmcsn.utils.Probabilities.getCheckInDesks;

public class CheckInDesksOthers {

    Rngs rngs;
    CheckInDesksSingleFlight[] checkInDesksSingleFlights;
    public int numberOfCenters;
    int CENTER_INDEX = 12;

    public CheckInDesksOthers() {
        this.checkInDesksSingleFlights = new CheckInDesksSingleFlight[19];
        for (int i = 0; i < checkInDesksSingleFlights.length; i++) {
            checkInDesksSingleFlights[i] = new CheckInDesksSingleFlight("CHECK_IN_OTHERS",10, 3, CENTER_INDEX + (2 * i) , i + 1);
        }
        this.numberOfCenters = checkInDesksSingleFlights.length;
    }

    public void reset(Rngs rngs) {
        this.rngs = rngs;
        for (int i = 0; i < checkInDesksSingleFlights.length; i++) {
            checkInDesksSingleFlights[i].reset(rngs);
        }
    }

    public void processArrival(MsqEvent arrival, MsqTime time, List<MsqEvent> events) {
        int index = getCheckInDesks(rngs, 11); //TODO sameStreamIndex here???
        if (index < 1 || index > checkInDesksSingleFlights.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        checkInDesksSingleFlights[index - 1].processArrival(arrival, time, events);
    }


    public void processCompletion(MsqEvent completion, MsqTime time, List<MsqEvent> events) {
        int index = completion.centerID;
        if (index < 1 || index > checkInDesksSingleFlights.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        checkInDesksSingleFlights[index - 1].processCompletion(completion, time, events);
    }

    public long getNumberOfJobsInNode() {

        int numberOfJobsInNode = 0;

        for(int index=1; index<=checkInDesksSingleFlights.length; index++){
            numberOfJobsInNode += checkInDesksSingleFlights[index-1].numberOfJobsInNode;
        }

        return numberOfJobsInNode;
    }

    public void resetBatch(int center ) {
        checkInDesksSingleFlights[center].resetBatch();

    }

    public int getJobsServed(int center){
        return (int) checkInDesksSingleFlights[center].getCompletions();
    }

    public int getBatchIndex(int center){
        return checkInDesksSingleFlights[center].batchIndex;
    }

    public int getMinBatchIndex() {
        // Assume there's at least one center in the array
        if (checkInDesksSingleFlights.length == 0) {
            throw new IllegalStateException("No centers available");
        }

        int minBatchIndex = checkInDesksSingleFlights[0].batchIndex;
        for (int i = 1; i < checkInDesksSingleFlights.length; i++) {
            if (checkInDesksSingleFlights[i].batchIndex < minBatchIndex) {
                minBatchIndex = checkInDesksSingleFlights[i].batchIndex;
            }
        }
        return minBatchIndex;
    }

    public void setArea(MsqTime time){
        for(CheckInDesksSingleFlight singleFlight : checkInDesksSingleFlights){
            singleFlight.updateArea(time.next - time.current);
        }
    }

    public void saveStats() {
        for(int index=1; index<=checkInDesksSingleFlights.length; index++){
            checkInDesksSingleFlights[index-1].saveStats();
        }
    }

    public void saveStats(int center) {
        checkInDesksSingleFlights[center].saveStats();
    }

    public void writeStats(String simulationType){
        for(int index=1; index<=checkInDesksSingleFlights.length; index++){
            checkInDesksSingleFlights[index-1].writeStats(simulationType);
        }
    }


    public Statistics.MeanStatistics getMeanStatistics(){
        List<Double> meanResponseTimeList = new ArrayList<>();
        List<Double> meanServiceTimeList = new ArrayList<Double>();
        List<Double> meanQueueTimeList = new ArrayList<Double>();
        List<Double> lambdaList = new ArrayList<Double>();
        List<Double> meanSystemPopulationList = new ArrayList<Double>();
        List<Double> meanUtilizationList = new ArrayList<Double>();
        List<Double> meanQueuePopulationList = new ArrayList<Double>();
        Statistics.MeanStatistics ms;

        // obtaining the mean for all centers
        for(int index=1; index<=checkInDesksSingleFlights.length; index++){
            ms = checkInDesksSingleFlights[index-1].getMeanStatistics();
            meanResponseTimeList.add(ms.meanResponseTime);
            meanServiceTimeList.add(ms.meanServiceTime);
            meanQueueTimeList.add(ms.meanQueueTime);
            lambdaList.add(ms.lambda);
            meanSystemPopulationList.add(ms.meanSystemPopulation);
            meanUtilizationList.add(ms.meanUtilization);
            meanQueuePopulationList.add(ms.meanQueuePopulation);
        }

        double meanResponseTime = computeMean(meanResponseTimeList);
        double meanServiceTime = computeMean(meanServiceTimeList);
        double meanQueueTime = computeMean(meanQueueTimeList);
        double lambda = computeMean(lambdaList);
        double meanSystemPopulation = computeMean(meanSystemPopulationList);
        double meanUtilization = computeMean(meanUtilizationList);
        double meanQueuePopulation = computeMean(meanQueuePopulationList);

        return new Statistics.MeanStatistics("CHECK-IN OTHERS", meanResponseTime, meanServiceTime, meanQueueTime, lambda, meanSystemPopulation, meanUtilization, meanQueuePopulation);

    }


    private class CheckInDesksSingleFlight extends Multiserver{


        public CheckInDesksSingleFlight(String name, double meanServiceTime, int numOfServers, int centerIndex, int centerID) {
            super(name + centerID, meanServiceTime, numOfServers, centerIndex);
            this.centerID = centerID;
        }

        private int centerID;

        public long getCompletions() {
            long numberOfJobsServed = 0;
            for(int i=0; i<SERVERS ; i++){
                numberOfJobsServed += sum[i].served;
            };
            return numberOfJobsServed;
        }

        public void spawnNextCenterEvent(MsqTime time, List<MsqEvent> events) {
            MsqEvent next_center_event = new MsqEvent(time.current, true, EventType.ARRIVAL_BOARDING_PASS_SCANNERS, 0);
            events.add(next_center_event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        }

        public void spawnCompletionEvent(MsqTime time, List<MsqEvent> events, int serverId) {

            double service = getService(CENTER_INDEX+1);

            //generate a new completion event
            MsqEvent event = new MsqEvent(time.current + service, true, EventType.CHECK_IN_OTHERS_DONE, serverId, centerID);
            // TODO: inizializzare in costruttore
            event.service = service;
            events.add(event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        }


        public double getService(int streamIndex)
            /* --------------------------------------------
             * generate the next service time with rate 2/3
             * --------------------------------------------
             */
        {
            rngs.selectStream(streamIndex);

            rngs.selectStream(streamIndex);
            // mean time 10 min
            // std dev 2 min (20% since it has low variability)
            //return (logNormal(10, 2, rngs));
            return exponential(10, rngs);
        }

    }
}
