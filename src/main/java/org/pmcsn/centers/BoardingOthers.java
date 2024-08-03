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
    private final int streamIndex;
    BoardingOtherSingleFlight[] boardingSingleFlightArray;

    public BoardingOthers(String name, int numberOfCenters, int serversNumber, double meanServiceTime, int streamIndex, boolean approximateServiceAsExponential) {
        this.name = name;
        this.boardingSingleFlightArray = new BoardingOtherSingleFlight[numberOfCenters];
        this.streamIndex = streamIndex;
        streamIndex++; // General class uses 1
        for (int i = 0; i < boardingSingleFlightArray.length; i++) {
            boardingSingleFlightArray[i] = new BoardingOtherSingleFlight(name,i + 1, meanServiceTime, serversNumber, streamIndex, approximateServiceAsExponential);
        }
    }

    public void setAreaForAll(MsqTime time) {
        Arrays.stream(boardingSingleFlightArray).forEach(s -> s.setArea(time));
    }

    public void saveBatchStats(int batchSize, int batchesNumber) {
        for(BoardingOtherSingleFlight other : boardingSingleFlightArray){
            other.saveBatchStats(batchSize, batchesNumber);
        }
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
        int index = getRandomValueUpToMax(rngs, streamIndex, boardingSingleFlightArray.length);
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

    public void resetBatch(int center) {
        boardingSingleFlightArray[center].resetBatch();
    }

    public void resetBatch() {
        Arrays.stream(boardingSingleFlightArray).forEach(MultiServer::resetBatch);
    }

    public long getJobsServed(int center){
        return boardingSingleFlightArray[center].getJobsServed();
    }

    public List<Statistics> getStatistics(){
        List<Statistics> statistics = new ArrayList<>();
        for (BoardingOtherSingleFlight s : boardingSingleFlightArray) {
            statistics.add(s.getStatistics());
        }
        return statistics;
    }

    public void saveStats() {
        for(BoardingOtherSingleFlight s : boardingSingleFlightArray){
            s.saveStats();
        }
    }

    public void saveStats(int center) {
        boardingSingleFlightArray[center].saveStats();
    }

    public void writeStats(String simulationType){
        for (BoardingOtherSingleFlight s : boardingSingleFlightArray){
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
        for (int i = 0; i < observations.size(); i++) {
            boardingSingleFlightArray[i].updateObservations(observations.get(i), run);
        }
    }
}
