package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;
import org.pmcsn.model.Statistics;

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


    protected int CENTER_INDEX;
    protected Statistics statistics;
    protected final Area area = new Area();
    protected double meanServiceTime;
    protected long numberOfJobsInNode = 0;
    protected double firstArrivalTime = Double.NEGATIVE_INFINITY;
    protected double lastArrivalTime = 0;
    protected double lastCompletionTime = 0;
    protected int batchIndex = 0;
    protected String centerName;
    protected Rngs rngs;
    protected MsqSum sum = new MsqSum();
    protected boolean approximateServiceAsExponential;

    public SingleServer(String centerName, double meanServiceTime, int centerIndex, boolean approximateServiceAsExponential) {
        this.centerName = centerName;
        this.meanServiceTime = meanServiceTime;
        this.CENTER_INDEX = centerIndex;
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
        this.numberOfJobsInNode = 0;
        this.firstArrivalTime = Double.NEGATIVE_INFINITY;
        this.lastArrivalTime = 0;
        this.lastCompletionTime = 0;
    }

    public void resetBatch() {
        // resetting variables
        this.firstArrivalTime = Double.NEGATIVE_INFINITY;
        this.lastArrivalTime = 0;
        this.lastCompletionTime = 0;
        sum.reset();
        area.reset();
    }

    public long getNumberOfJobsInNode() {
        return numberOfJobsInNode;
    }

    public long getCompletions(){
        return sum.served;
    }

    public void setArea(MsqTime time) {
        // TODO: il controllo per l'aggiornamento di area dovrebbe avvenire nel loop principale
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
        if(firstArrivalTime == Double.NEGATIVE_INFINITY){
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
        spawnNextCenterEvent(time, queue);
        if (numberOfJobsInNode > 0) {
            spawnCompletionEvent(time, queue);
        }
    }

    public void saveStats() {
        batchIndex++;
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
