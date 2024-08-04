package org.pmcsn.centers;

import org.pmcsn.conf.Config;
import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Probabilities.getRandomValueUpToMax;
import static org.pmcsn.utils.StatisticsUtils.computeMean;

public class LuggageChecks {
    private final String centerName;
    Rngs rngs;
    LuggageChecksSingleEntrance[] luggageChecksSingleEntrances;
    private final double interArrivalTime;
    double sarrival;
    // all the times are measured in min
    int STOP;
    boolean endOfArrivals = false;
    private final int streamIndex;

    public LuggageChecks(String centerName, int numberOfCenters, double interArrivalTime, double meanServiceTime, int streamIndex, boolean approximateServiceAsExponential) {
        this.centerName = centerName;
        this.interArrivalTime = interArrivalTime;
        this.luggageChecksSingleEntrances = new LuggageChecksSingleEntrance[numberOfCenters];
        this.streamIndex = streamIndex;
        streamIndex+=2; // General class uses 2
        for (int i = 0; i < luggageChecksSingleEntrances.length; i++) {
            luggageChecksSingleEntrances[i] = new LuggageChecksSingleEntrance(centerName, i + 1, streamIndex, meanServiceTime, approximateServiceAsExponential);
        }
        Config config = new Config();
        STOP = config.getInt("general", "observationTime");
    }

    public void reset(Rngs rngs, double sarrival) {
        this.rngs = rngs;
        this.sarrival = sarrival;
        this.endOfArrivals = false;
        for (LuggageChecksSingleEntrance center : luggageChecksSingleEntrances) {
            center.reset(rngs);
        }
    }

    public void setSTOP(int STOP) {
        this.STOP = STOP;
    }

    public boolean isEndOfArrivals(){
        return endOfArrivals;
    }

    public void processArrival(MsqEvent arrival, MsqTime time, EventQueue queue) {
        int index = getRandomValueUpToMax(rngs, streamIndex, luggageChecksSingleEntrances.length);
        if (index < 1 || index > luggageChecksSingleEntrances.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        luggageChecksSingleEntrances[index - 1].processArrival(arrival, time, queue);

        // Generating a new arrival
        double nextArrival = getArrival();

        // Checking if the next arrival exceeds time limit
        if(nextArrival < STOP){
            MsqEvent event = new MsqEvent(EventType.ARRIVAL_LUGGAGE_CHECK, nextArrival);
            queue.add(event);
        } else {
            endOfArrivals = true;
        }

    }

    public void processCompletion(MsqEvent arrival, MsqTime time, EventQueue queue){
        int index = arrival.nodeId;
        if (index < 1 || index > luggageChecksSingleEntrances.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        luggageChecksSingleEntrances[index - 1].processCompletion(arrival, time, queue);
    }

    public double getArrival() {
        rngs.selectStream(streamIndex + 1);
        sarrival += exponential(interArrivalTime, rngs);
        return (sarrival);
    }

    public long getTotalNumberOfJobsInNode() {
        return Arrays.stream(luggageChecksSingleEntrances).mapToLong(c -> c.numberOfJobsInNode).sum();
    }

    public long[] getNumberOfJobsPerCenter() {
        return Arrays.stream(luggageChecksSingleEntrances).mapToLong(SingleServer::getJobsServed).toArray();
    }

    public long getTotalNumberOfJobsServed() {
        return Arrays.stream(luggageChecksSingleEntrances).mapToLong(LuggageChecksSingleEntrance::getJobsServed).sum();
    }

    public int getJobsServed(int center){
        return (int) luggageChecksSingleEntrances[center].getJobsServed();
    }

    public void setAreaForAll(MsqTime time){
        for (LuggageChecksSingleEntrance singleEntrance : luggageChecksSingleEntrances) {
            singleEntrance.setArea(time);
        }
    }

    public List<BatchStatistics> getBatchStatistics(){
        List<BatchStatistics> statistics = new ArrayList<>();
        for (LuggageChecksSingleEntrance s : luggageChecksSingleEntrances) {
            statistics.add(s.getBatchStatistics());
        }
        return statistics;
    }


    public List<BasicStatistics> getStatistics(){
        List<BasicStatistics> statistics = new ArrayList<>();
        for (LuggageChecksSingleEntrance s : luggageChecksSingleEntrances) {
            statistics.add(s.getStatistics());
        }
        return statistics;
    }

    public void saveStats() {
        for (LuggageChecksSingleEntrance center : luggageChecksSingleEntrances) {
            center.saveStats();
        }
    }

    public void saveBatchStats(MsqTime time) {
        for (LuggageChecksSingleEntrance center : luggageChecksSingleEntrances) {
            center.saveBatchStats(time);
        }
    }

    public List<List<Double>> getMeans() {
        List<List<Double>> means = new ArrayList<>();
        Arrays.stream(luggageChecksSingleEntrances).forEach(s -> means.add(s.getMeanSystemPopulationList()));
        return means;
    }

    public void saveStats(int center) {
        luggageChecksSingleEntrances[center].saveStats();
    }

    public void writeStats(String simulationType){
        for (LuggageChecksSingleEntrance center : luggageChecksSingleEntrances) {
            center.writeStats(simulationType);
        }
    }

    public void writeBatchStats(String simulationType){
        for (LuggageChecksSingleEntrance center : luggageChecksSingleEntrances) {
            center.writeBatchStats(simulationType);
        }
    }

    public List<MeanStatistics> getBatchMeanStatistics(){
        return Arrays.stream(luggageChecksSingleEntrances).map(SingleServer::getBatchMeanStatistics).toList();
    }

    public List<MeanStatistics> getMeanStatistics() {
        return Arrays.stream(luggageChecksSingleEntrances).map(SingleServer::getMeanStatistics).toList();
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

    public void updateObservations(List<Observations> observationsList, int run) {
        for (int i = 0; i < luggageChecksSingleEntrances.length; ++i) {
            luggageChecksSingleEntrances[i].updateObservations(observationsList.get(i), run);
        }
    }
}
