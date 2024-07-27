package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Distributions.logNormal;
import static org.pmcsn.utils.Probabilities.getCheckInDesks;
import static org.pmcsn.utils.StatisticsUtils.computeMean;

public class CheckInDesksOthers {
    Rngs rngs;
    CheckInDesksSingleFlight[] checkInDesksSingleFlights;
    public int numberOfCenters;

    public CheckInDesksOthers(int nodesNumber, int serversNumber, double meanServiceTime, int centerIndex, boolean approximateServiceAsExponential) {
        this.checkInDesksSingleFlights = new CheckInDesksSingleFlight[nodesNumber];
        for (int i = 0; i < checkInDesksSingleFlights.length; i++) {
            checkInDesksSingleFlights[i] = new CheckInDesksSingleFlight("CHECK_IN_OTHERS", meanServiceTime, serversNumber, centerIndex + (2 * i), i + 1, approximateServiceAsExponential);
        }
        this.numberOfCenters = checkInDesksSingleFlights.length;
    }

    public void reset(Rngs rngs) {
        this.rngs = rngs;
        for (CheckInDesksSingleFlight checkInDesksSingleFlight : checkInDesksSingleFlights) {
            checkInDesksSingleFlight.reset(rngs);
        }
    }

    public void processArrival(MsqEvent arrival, MsqTime time, EventQueue queue) {
        int index = getCheckInDesks(rngs, 11); //TODO sameStreamIndex here???
        if (index < 1 || index > checkInDesksSingleFlights.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        checkInDesksSingleFlights[index - 1].processArrival(arrival, time, queue);
    }


    public void processCompletion(MsqEvent completion, MsqTime time, EventQueue queue) {
        int index = completion.nodeId;
        if (index < 1 || index > checkInDesksSingleFlights.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        checkInDesksSingleFlights[index - 1].processCompletion(completion, time, queue);
    }

    public long getNumberOfJobsInNode() {
        return Arrays.stream(checkInDesksSingleFlights).mapToLong(Multiserver::getNumberOfJobsInNode).sum();
    }

    public void resetBatch(int center ) {
        checkInDesksSingleFlights[center].resetBatch();

    }

    public long getJobsServed(int center){
        return checkInDesksSingleFlights[center].getCompletions();
    }

    public int getBatchIndex(int center){
        return checkInDesksSingleFlights[center].batchIndex;
    }

    public List<Statistics> getStatistics(){
        List<Statistics> statistics = new ArrayList<>();
        for (int i = 1; i < checkInDesksSingleFlights.length; i++) {

            statistics.add(checkInDesksSingleFlights[i].getStatistics());

        }

        return statistics;
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

    public void updateArea(MsqTime time){
        for(CheckInDesksSingleFlight singleFlight : checkInDesksSingleFlights){
            singleFlight.setArea(time);
        }
    }

    public void saveStats() {
        for (CheckInDesksSingleFlight c : checkInDesksSingleFlights){
            c.saveStats();
        }
    }

    public void saveStats(int center) {
        checkInDesksSingleFlights[center].saveStats();
    }

    public void writeStats(String simulationType){
        for (CheckInDesksSingleFlight c : checkInDesksSingleFlights){
            c.writeStats(simulationType);
        }
    }

    public Statistics.MeanStatistics getMeanStatistics(){
        List<Double> meanResponseTimeList = new ArrayList<>();
        List<Double> meanServiceTimeList = new ArrayList<>();
        List<Double> meanQueueTimeList = new ArrayList<>();
        List<Double> lambdaList = new ArrayList<>();
        List<Double> meanSystemPopulationList = new ArrayList<>();
        List<Double> meanUtilizationList = new ArrayList<>();
        List<Double> meanQueuePopulationList = new ArrayList<>();
        Statistics.MeanStatistics ms;

        // obtaining the mean for all centers
        for(CheckInDesksSingleFlight c : checkInDesksSingleFlights){
            ms = c.getMeanStatistics();
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

    private static class CheckInDesksSingleFlight extends Multiserver {
        private final int nodeId;

        public CheckInDesksSingleFlight(String name, double meanServiceTime, int numOfServers, int centerIndex, int nodeId, boolean approximateServiceAsExponential) {
            super(name + nodeId, meanServiceTime, numOfServers, centerIndex, approximateServiceAsExponential);
            this.nodeId = nodeId;
        }

        public long getCompletions() {
            return Arrays.stream(sum).mapToLong(s -> s.served).sum();
        }

        @Override
        public void spawnNextCenterEvent(MsqTime time, EventQueue queue) {
            MsqEvent event = new MsqEvent(EventType.ARRIVAL_BOARDING_PASS_SCANNERS, time.current);
            queue.add(event);
        }

        public void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId) {
            double service = getService(CENTER_INDEX+1);
            MsqEvent event = new MsqEvent(EventType.CHECK_IN_OTHERS_DONE, time.current + service, service, serverId, nodeId);
            queue.add(event);
        }

        public double getService(int streamIndex) {
            rngs.selectStream(streamIndex);
            if(approximateServiceAsExponential){
                return exponential(meanServiceTime, rngs);
            }
            return (logNormal(meanServiceTime, meanServiceTime*0.2, rngs));
        }
    }
}
