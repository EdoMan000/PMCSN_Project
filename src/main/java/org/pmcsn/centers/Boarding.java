package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.pmcsn.utils.Probabilities.getRandomValueUpToMax;

public class Boarding {
    Rngs rngs;
    private final String centerName;
    private final int streamIndex;
    BoardingSingleFlight[] boardingSingleFlightArray;

    public Boarding(String name, int numberOfCenters, int serversNumber, double meanServiceTime, int streamIndex, boolean approximateServiceAsExponential) {
        this.centerName = name;
        this.boardingSingleFlightArray = new BoardingSingleFlight[numberOfCenters];
        this.streamIndex = streamIndex;
        streamIndex++; // General class uses 1
        for (int i = 0; i < boardingSingleFlightArray.length; i++) {
            boardingSingleFlightArray[i] = new BoardingSingleFlight(name,i + 1, meanServiceTime, serversNumber, streamIndex, approximateServiceAsExponential);
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
        for (BoardingSingleFlight other : boardingSingleFlightArray) {
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
        for (BoardingSingleFlight s : boardingSingleFlightArray) {
            statistics.add(s.getStatistics());
        }
        return statistics;
    }

    public List<BatchStatistics> getBatchStatistics() {
        List<BatchStatistics> statistics = new ArrayList<>();
        for (BoardingSingleFlight s : boardingSingleFlightArray) {
            statistics.add(s.getBatchStatistics());
        }
        return statistics;
    }

    public void saveStats() {
        for(BoardingSingleFlight s : boardingSingleFlightArray){
            s.saveStats();
        }
    }

    public void writeStats(String simulationType){
        for (BoardingSingleFlight s : boardingSingleFlightArray){
            s.writeStats(simulationType);
        }
    }

    public void writeBatchStats(String simulationType){
        for (BoardingSingleFlight s : boardingSingleFlightArray){
            s.writeBatchStats(simulationType);
        }
    }

    public List<MeanStatistics> getBatchMeanStatistics(){
        return Arrays.stream(boardingSingleFlightArray).map(MultiServer::getBatchMeanStatistics).toList();
    }

    public List<MeanStatistics> getMeanStatistics() {
        return Arrays.stream(boardingSingleFlightArray).map(MultiServer::getMeanStatistics).toList();
    }

    public void updateObservations(List<List<Observations>> observations, int run) {
        for (int i = 0; i < observations.size(); i++) {
            boardingSingleFlightArray[i].updateObservations(observations.get(i), run);
        }
    }

    public void stopWarmup(MsqTime time) {
        Arrays.stream(boardingSingleFlightArray).forEach(boardingSingleFlight -> boardingSingleFlight.stopWarmup(time));
    }

    public boolean isDone() {
        return Arrays.stream(boardingSingleFlightArray).allMatch(MultiServer::isDone);
    }
}
