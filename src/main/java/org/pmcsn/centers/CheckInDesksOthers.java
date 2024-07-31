package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.pmcsn.utils.Probabilities.getRandomValueUpToMax;
import static org.pmcsn.utils.StatisticsUtils.computeMean;

public class CheckInDesksOthers {
    Rngs rngs;
    private final String name;
    CheckInDesksOtherSingleFlight[] checkInDesksSingleFlightArray;

    public CheckInDesksOthers(String name, int numberOfCenters, int serversNumber, double meanServiceTime, int centerIndex, boolean approximateServiceAsExponential) {
        this.name = name;
        this.checkInDesksSingleFlightArray = new CheckInDesksOtherSingleFlight[numberOfCenters];
        for (int i = 0; i < checkInDesksSingleFlightArray.length; i++) {
            checkInDesksSingleFlightArray[i] = new CheckInDesksOtherSingleFlight(name,i + 1, meanServiceTime, serversNumber, centerIndex + (2 * i), approximateServiceAsExponential);
        }
    }

    public void reset(Rngs rngs) {
        this.rngs = rngs;
        for (CheckInDesksOtherSingleFlight s : checkInDesksSingleFlightArray) {
            s.reset(rngs);
        }
    }

    public void processArrival(MsqEvent arrival, MsqTime time, EventQueue queue) {
        int index = getRandomValueUpToMax(rngs, 11, checkInDesksSingleFlightArray.length); //TODO sameStreamIndex here???
        if (index < 1 || index > checkInDesksSingleFlightArray.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        checkInDesksSingleFlightArray[index - 1].processArrival(arrival, time, queue);
    }


    public void processCompletion(MsqEvent completion, MsqTime time, EventQueue queue) {
        int index = completion.nodeId;
        if (index < 1 || index > checkInDesksSingleFlightArray.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        checkInDesksSingleFlightArray[index - 1].processCompletion(completion, time, queue);
    }

    public long getTotalNumberOfJobsInNode() {
        return Arrays.stream(checkInDesksSingleFlightArray).mapToLong(MultiServer::getNumberOfJobsInNode).sum();
    }

    public void resetBatch() {
        Arrays.stream(checkInDesksSingleFlightArray).forEach(MultiServer::resetBatch);
    }

    public long[] getNumberOfJobsPerCenter() {
        return Arrays.stream(checkInDesksSingleFlightArray).mapToLong(MultiServer::getJobsServed).toArray();
    }

    public long getJobsServed(int center){
        return checkInDesksSingleFlightArray[center].getCompletions();
    }

    public int getBatchIndex(int center){
        return checkInDesksSingleFlightArray[center].batchIndex;
    }

    public List<Statistics> getStatistics(){
        List<Statistics> statistics = new ArrayList<>();
        for (CheckInDesksOtherSingleFlight s : checkInDesksSingleFlightArray) {
            statistics.add(s.getStatistics());
        }
        return statistics;
    }

    public int getMinBatchIndex() {
        // Assume there's at least one center in the array
        if (checkInDesksSingleFlightArray.length == 0) {
            throw new IllegalStateException("No centers available");
        }

        int minBatchIndex = checkInDesksSingleFlightArray[0].batchIndex;
        for (int i = 1; i < checkInDesksSingleFlightArray.length; i++) {
            if (checkInDesksSingleFlightArray[i].batchIndex < minBatchIndex) {
                minBatchIndex = checkInDesksSingleFlightArray[i].batchIndex;
            }
        }
        return minBatchIndex;
    }

    public void setAreaForAll(MsqTime time){
        for(CheckInDesksOtherSingleFlight s : checkInDesksSingleFlightArray){
            s.setArea(time);
        }
    }

    public void saveStats() {
        for (CheckInDesksOtherSingleFlight s : checkInDesksSingleFlightArray){
            s.saveStats();
        }
    }

    public void saveStats(int center) {
        checkInDesksSingleFlightArray[center].saveStats();
    }

    public void writeStats(String simulationType){
        for (CheckInDesksOtherSingleFlight s : checkInDesksSingleFlightArray){
            s.writeStats(simulationType);
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
        for(CheckInDesksOtherSingleFlight c : checkInDesksSingleFlightArray){
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
        return new Statistics.MeanStatistics(name, meanResponseTime, meanServiceTime, meanQueueTime, lambda, meanSystemPopulation, meanUtilization, meanQueuePopulation);
    }

    public void updateObservations(List<List<Observations>> observations, int run) {
        for (int i = 0; i < checkInDesksSingleFlightArray.length; i++) {
            checkInDesksSingleFlightArray[i].updateObservations(observations.get(i), run);
        }
    }
}
