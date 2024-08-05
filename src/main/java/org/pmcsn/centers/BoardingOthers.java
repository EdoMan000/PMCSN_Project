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
    private final String centerName;
    private final int streamIndex;
    BoardingOtherSingleFlight[] boardingSingleFlightArray;

    public BoardingOthers(String name, int numberOfCenters, int serversNumber, double meanServiceTime, int streamIndex, boolean approximateServiceAsExponential) {
        this.centerName = name;
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


    public long[] getTotalNumberOfJobsServed() {
        return Arrays.stream(boardingSingleFlightArray).mapToLong(MultiServer::getTotalNumberOfJobsServed).toArray();
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

    public List<BasicStatistics> getStatistics(){
        List<BasicStatistics> statistics = new ArrayList<>();
        for (BoardingOtherSingleFlight s : boardingSingleFlightArray) {
            statistics.add(s.getStatistics());
        }
        return statistics;
    }

    public List<BatchStatistics> getBatchStatistics() {
        List<BatchStatistics> statistics = new ArrayList<>();
        for (BoardingOtherSingleFlight s : boardingSingleFlightArray) {
            statistics.add(s.getBatchStatistics());
        }
        return statistics;
    }

    public void saveStats() {
        for(BoardingOtherSingleFlight s : boardingSingleFlightArray){
            s.saveStats();
        }
    }

    public void writeStats(String simulationType){
        for (BoardingOtherSingleFlight s : boardingSingleFlightArray){
            s.writeStats(simulationType);
        }
    }

    public void writeBatchStats(String simulationType){
        for (BoardingOtherSingleFlight s : boardingSingleFlightArray){
            s.writeBatchStats(simulationType);
        }
    }


    public List<MeanStatistics> getBatchMeanStatistics(){
        return Arrays.stream(boardingSingleFlightArray).map(MultiServer::getBatchMeanStatistics).toList();
    }

    public List<MeanStatistics> getMeanStatistics() {
        return Arrays.stream(boardingSingleFlightArray).map(MultiServer::getMeanStatistics).toList();
    }

    private MeanStatistics retrieveMeanStats(List<MeanStatistics> means) {
        List<Double> meanResponseTimeList = new ArrayList<>();
        List<Double> meanServiceTimeList = new ArrayList<Double>();
        List<Double> meanQueueTimeList = new ArrayList<Double>();
        List<Double> lambdaList = new ArrayList<Double>();
        List<Double> meanSystemPopulationList = new ArrayList<Double>();
        List<Double> meanUtilizationList = new ArrayList<Double>();
        List<Double> meanQueuePopulationList = new ArrayList<Double>();
        for (MeanStatistics ms : means) {
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
        return new MeanStatistics(centerName, meanResponseTime, meanServiceTime, meanQueueTime, lambda, meanSystemPopulation, meanUtilization, meanQueuePopulation);
    }

    public void updateObservations(List<List<Observations>> observations, int run) {
        for (int i = 0; i < observations.size(); i++) {
            boardingSingleFlightArray[i].updateObservations(observations.get(i), run);
        }
    }

    public void stopWarmup() {
        Arrays.stream(boardingSingleFlightArray).forEach(MultiServer::stopWarmup);
    }

    public boolean isDone() {
        return Arrays.stream(boardingSingleFlightArray).allMatch(MultiServer::isDone);
    }
}
