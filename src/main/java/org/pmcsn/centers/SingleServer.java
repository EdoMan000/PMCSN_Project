package org.pmcsn.centers;

import org.pmcsn.conf.Config;
import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;
import org.pmcsn.model.Statistics;

import java.util.ArrayList;
import java.util.List;

public abstract class SingleServer {

    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Service times
     *  * Queue times
     *  * Inter-arrival times
     *  * Population
     *  * Utilization
     *  * Queue population
     */


    protected int streamindex;
    protected Statistics statistics;
    protected final Area area = new Area();
    protected double meanServiceTime;
    protected long numberOfJobsInNode = 0;
    protected double firstArrivalTime = Double.NEGATIVE_INFINITY;
    protected double lastArrivalTime = 0;
    protected double lastCompletionTime = 0;
    protected String centerName;
    protected Rngs rngs;
    protected MsqSum sum = new MsqSum();
    protected boolean approximateServiceAsExponential;
    long lastJobsServed = 0;
    private final List<Double> batchPoints = new ArrayList<>();
    private final List<Double> meanResponseTimeListBatch = new ArrayList<>();
    private int batchSize;
    private int batchesNumber;

    public SingleServer(String centerName, double meanServiceTime, int streamIndex, boolean approximateServiceAsExponential) {
        Config config  = new Config();
        batchSize = config.getInt("general", "batchSize");
        batchesNumber = config.getInt("general", "numBatches");
        this.centerName = centerName;
        this.meanServiceTime = meanServiceTime;
        this.streamindex = streamIndex;
        this.statistics = new Statistics(centerName);
        this.approximateServiceAsExponential = approximateServiceAsExponential;
    }

    //********************************** ABSTRACT METHODS *********************************************
    abstract void spawnNextCenterEvent(MsqTime time, EventQueue queue);
    abstract void spawnCompletionEvent(MsqTime time, EventQueue queue);
    abstract double getService(int streamIndex);

    //********************************** CONCRETE METHODS *********************************************

    public void reset(Rngs rngs) {
        this.rngs = rngs;
        area.reset();
        sum.reset();
        // resetting variables
        lastJobsServed = 0;
        this.numberOfJobsInNode = 0;
        this.firstArrivalTime = Double.NEGATIVE_INFINITY;
        this.lastArrivalTime = 0;
        this.lastCompletionTime = 0;
    }

    public void resetBatch() {
        statistics.clear();
    }

    public long getNumberOfJobsInNode() {
        return numberOfJobsInNode;
    }

    public long getJobsServed(){
        return sum.served;
    }

    public void setArea(MsqTime time) {
        if (numberOfJobsInNode > 0) {
            double width = time.next - time.current;
            area.incNodeArea(width * numberOfJobsInNode);
            area.incQueueArea(width * (numberOfJobsInNode - 1));
            area.incServiceArea(width);
        }
    }

    public void processArrival(MsqEvent arrival, MsqTime time, EventQueue queue){
        // increment the number of jobs in the node
        numberOfJobsInNode++;

        // Updating the first arrival time (we will use it in the statistics)
        if(firstArrivalTime == Double.NEGATIVE_INFINITY) {
            firstArrivalTime = arrival.time;
        }
        lastArrivalTime = arrival.time;

        if (numberOfJobsInNode == 1) {
            spawnCompletionEvent(time, queue);
        }
    }

    public void processCompletion(MsqEvent completion, MsqTime time, EventQueue queue) {
        numberOfJobsInNode--;
        sum.served++;
        sum.service += completion.service;
        lastCompletionTime = completion.time;
        saveBatch();
        spawnNextCenterEvent(time, queue);
        if (numberOfJobsInNode > 0) {
            spawnCompletionEvent(time, queue);
        }
    }

    public List<Double> getMeanResponseTimeListBatch() {
        return meanResponseTimeListBatch;
    }

    private void saveBatch() {
        double meanSystemPopulation = area.getNodeArea() / lastCompletionTime;
        batchPoints.add(meanSystemPopulation);
        if (batchPoints.size() == batchSize) {
            double average = batchPoints.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            appendToBatch(meanResponseTimeListBatch, average);
            batchPoints.clear();
        }
    }

    private void appendToBatch(List<Double> batch, double value) {
        if (batch.size() < batchesNumber) {
            batch.add(value);
        }
    }

    public void saveStats(int batchesNumber) {
        MsqSum[] sums = new MsqSum[1];
        sums[0] = this.sum;
        statistics.saveStats(area, sums, lastArrivalTime, lastCompletionTime, false, batchesNumber);
    }

    public void saveStats() {
        MsqSum[] sums = new MsqSum[1];
        sums[0] = this.sum;
        statistics.saveStats(area, sums, lastArrivalTime, lastCompletionTime, false);
    }

    public void writeStats(String simulationType){
        statistics.writeStats(simulationType);
    }

    public Statistics.MeanStatistics getMeanStatistics() {
        return statistics.getMeanStatistics();
    }

    public Statistics getStatistics(){
        return this.statistics;
    }

    public void saveBatchStats(int batchSize, int batchesNumber) {
        if(getJobsServed() > 0 && getJobsServed() > lastJobsServed && getJobsServed() % batchSize == 0){
            saveStats(batchesNumber);
            lastJobsServed = getJobsServed();
        }
    }

    public void updateObservations(Observations observations, int run) {
        if (lastArrivalTime == 0 || lastCompletionTime == 0) {
            return;
        }
        double lambda = sum.served / lastArrivalTime;
        double meanNodePopulation = area.getNodeArea() / lastCompletionTime;
        double meanResponseTime = meanNodePopulation / lambda;
        observations.saveObservation(run, Observations.INDEX.RESPONSE_TIME, meanResponseTime);
    }
}
