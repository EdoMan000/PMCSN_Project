package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.pmcsn.utils.Probabilities.getRandomValueUpToMax;
import static org.pmcsn.utils.StatisticsUtils.computeMean;

public class BoardingOthers {
    Rngs rngs;
    private final String name;
    BoardingOtherSingleFlight[] boardingSingleFlightArray;

    public BoardingOthers(String name, int numberOfCenters, int serversNumber, double meanServiceTime, int centerIndex, boolean approximateServiceAsExponential) {
        this.name = name;
        this.boardingSingleFlightArray = new BoardingOtherSingleFlight[numberOfCenters];
        for (int i = 0; i < boardingSingleFlightArray.length; i++) {
            boardingSingleFlightArray[i] = new BoardingOtherSingleFlight(name,i + 1, meanServiceTime, serversNumber, centerIndex + (2 * i), approximateServiceAsExponential);
        }
    }

    public void setAreaForAll(MsqTime time) {
        Arrays.stream(boardingSingleFlightArray).forEach(s -> s.setArea(time));
    }

    public long[] getNumberOfJobsPerCenter() {
        return Arrays.stream(boardingSingleFlightArray).mapToLong(MultiServer::getJobsServed).toArray();
    }

    public void reset(Rngs rngs) {
        this.rngs = rngs;
        for (BoardingOtherSingleFlight other : boardingSingleFlightArray) {
            other.reset(rngs);
        }
    }

    public void processArrival(MsqEvent arrival, MsqTime time, EventQueue queue) {
        int index = getRandomValueUpToMax(rngs, 11, boardingSingleFlightArray.length); //TODO same StreamIndex here???
        if (index < 1 || index > boardingSingleFlightArray.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        boardingSingleFlightArray[index - 1].processArrival(arrival, time, queue);
    }


    public void processCompletion(MsqEvent completion, MsqTime time, EventQueue queue) {
        int index = completion.nodeId;
        if (index < 1 || index > boardingSingleFlightArray.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        boardingSingleFlightArray[index - 1].processCompletion(completion, time, queue);
    }

    public long getTotalNumberOfJobsInNode() {
        return Arrays.stream(boardingSingleFlightArray).mapToLong(MultiServer::getNumberOfJobsInNode).sum();
    }

    public void resetBatch() {
        Arrays.stream(boardingSingleFlightArray).forEach(MultiServer::resetBatch);
    }

    public long getJobsServed(int center){
        return boardingSingleFlightArray[center].getJobsServed();
    }

    public int getBatchIndex(int center){
        return boardingSingleFlightArray[center].batchIndex;
    }

    public List<Statistics> getStatistics(){
        List<Statistics> statistics = new ArrayList<>();
        for (int i = 1; i < boardingSingleFlightArray.length; i++) {

            statistics.add(boardingSingleFlightArray[i].getStatistics());

        }

        return statistics;
    }

    public int getMinBatchIndex() {
        // Assume there's at least one center in the array
        if (boardingSingleFlightArray.length == 0) {
            throw new IllegalStateException("No centers available");
        }

        int minBatchIndex = boardingSingleFlightArray[0].batchIndex;
        for (int i = 1; i < boardingSingleFlightArray.length; i++) {
            if (boardingSingleFlightArray[i].batchIndex < minBatchIndex) {
                minBatchIndex = boardingSingleFlightArray[i].batchIndex;
            }
        }
        return minBatchIndex;
    }

    public void saveStats() {
        for(BoardingOtherSingleFlight other : boardingSingleFlightArray){
            other.saveStats();
        }
    }

    public void saveStats(int center) {
        boardingSingleFlightArray[center].saveStats();
    }

    public void writeStats(String simulationType){
        for (BoardingOtherSingleFlight c : boardingSingleFlightArray){
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
        for(BoardingOtherSingleFlight c : boardingSingleFlightArray){
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
        for (int i = 0; i < boardingSingleFlightArray.length; i++) {
            boardingSingleFlightArray[i].updateObservations(observations.get(i), run);
        }
    }
}