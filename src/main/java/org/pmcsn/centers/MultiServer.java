package org.pmcsn.centers;

import org.pmcsn.conf.Config;
import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.Arrays;
import java.util.List;

public abstract class MultiServer {
    protected long numberOfJobsInNode = 0;
    protected int SERVERS;
    protected int streamIndex;
    protected Area area;
    protected double firstArrivalTime = Double.NEGATIVE_INFINITY;
    protected double lastArrivalTime = 0;
    protected double lastCompletionTime = 0;
    protected double meanServiceTime;
    protected String centerName;
    protected boolean approximateServiceAsExponential;
    protected Rngs rngs;
    protected long lastJobsServed = 0;
    protected int batchSize;
    protected int batchesNumber;
    private double currentBatchStartTime;

    protected MsqSum[] sum;
    protected MsqServer[] servers;

    protected BasicStatistics statistics;
    protected BatchStatistics batchStatistics;

    public MultiServer(String centerName, double meanServiceTime, int serversNumber, int streamIndex, boolean approximateServiceAsExponential) {
        Config config  = new Config();
        batchSize = config.getInt("general", "batchSize");
        batchesNumber = config.getInt("general", "numBatches");
        this.centerName = centerName;
        this.meanServiceTime = meanServiceTime;
        this.SERVERS = serversNumber;
        this.streamIndex = streamIndex;
        this.sum =  new MsqSum[SERVERS];
        this.servers = new MsqServer[SERVERS];
        for(int i=0; i<SERVERS ; i++){
            sum[i] = new MsqSum();
            servers[i] = new MsqServer();
        }
        this.area = new Area();
        this.statistics = new BasicStatistics(centerName);
        this.batchStatistics = new BatchStatistics(centerName, batchSize, batchesNumber);
        this.approximateServiceAsExponential = approximateServiceAsExponential;
    }

    //********************************** ABSTRACT METHODS *********************************************
    abstract void spawnNextCenterEvent(MsqTime time, EventQueue queue);
    abstract void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId);
    abstract double getService(int streamIndex);

    //********************************** CONCRETE METHODS *********************************************
    public void reset(Rngs rngs) {
        this.rngs = rngs;
        // resetting variables
        lastJobsServed = 0;
        this.numberOfJobsInNode =0;
        area.reset();
        this.firstArrivalTime = Double.NEGATIVE_INFINITY;
        this.lastArrivalTime = 0;
        this.lastCompletionTime = 0;
        for(int i=0; i<SERVERS ; i++){
            sum[i].reset();
            servers[i].reset();
        }
    }

    public void resetBatch(MsqTime time) {
        area.reset();
        Arrays.stream(sum).forEach(MsqSum::reset);
        currentBatchStartTime = time.current;
    }

    public long getJobsServed() {
        return Arrays.stream(sum).mapToLong(x -> x.served).sum();
    }

    public BasicStatistics getStatistics(){
        return statistics;
    }

    public BatchStatistics getBatchStatistics() {
        return batchStatistics;
    }

    public long getNumberOfJobsInNode() {
        return numberOfJobsInNode;
    }

    public void setArea(MsqTime time){
        double width = time.next - time.current;
        area.incNodeArea(width * numberOfJobsInNode);
        long busyServers = Arrays.stream(servers).filter(x -> x.running).count();
        area.incQueueArea(width * (numberOfJobsInNode - busyServers));
        area.incServiceArea(width);
    }

    public void processArrival(MsqEvent arrival, MsqTime time, EventQueue queue){
        // increment the number of jobs in the node
        numberOfJobsInNode++;

        // Updating the first arrival time (we will use it in the statistics)
        if(firstArrivalTime == Double.NEGATIVE_INFINITY){
            firstArrivalTime = arrival.time;
        }
        lastArrivalTime = arrival.time;

        if (numberOfJobsInNode <= SERVERS) {
            int serverId = findOne();
            servers[serverId].running = true;
            spawnCompletionEvent(time, queue, serverId);
        }
    }

    public void processCompletion(MsqEvent completion, MsqTime time, EventQueue queue) {
        numberOfJobsInNode--;
        int serverId = completion.serverId;
        sum[serverId].service += completion.service;
        sum[serverId].served++;
        lastCompletionTime = completion.time;
        spawnNextCenterEvent(time, queue);
        if (numberOfJobsInNode >= SERVERS) {
            spawnCompletionEvent(time, queue, serverId);
        } else {
            servers[serverId].lastCompletionTime = completion.time;
            servers[serverId].running = false;
        }
    }

    public int findOne() {
        int s;
        int i = 0;
        while (servers[i].running)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;
        if (s == SERVERS) return s;
        while (i < SERVERS-1) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */
            if (!servers[i].running && (servers[i].lastCompletionTime < servers[s].lastCompletionTime))
                s = i;
        }
        return s;
    }

    public void saveStats() {
        statistics.saveStats(area, sum, lastArrivalTime, lastCompletionTime, true);
    }

    public void writeStats(String simulationType){
        statistics.writeStats(simulationType);
    }

    public void writeBatchStats(String simulationType){
        batchStatistics.writeStats(simulationType);
    }

    public void saveBatchStats(MsqTime time) {
        if(getJobsServed() > 0 && getJobsServed() > lastJobsServed ){
            batchStatistics.saveStats(area, sum, lastArrivalTime, lastCompletionTime, false, currentBatchStartTime);
            if(getJobsServed() % batchSize == 0) {
                lastJobsServed = getJobsServed();
                resetBatch(time);
            }
        }
    }


    public MeanStatistics getMeanStatistics() {
        return statistics.getMeanStatistics();
    }

    public MeanStatistics getBatchMeanStatistics() {
        return batchStatistics.getMeanStatistics();
    }

    public void updateObservations(List<Observations> observationsList, int run) {
        for (int i = 0; i < observationsList.size(); i++) {
            updateObservation(observationsList.get(i), run, i);
        }
    }

    private void updateObservation(Observations observations, int run, int serverId) {
        long numberOfJobsServed = 0;
        for (MsqSum s : sum) {
            numberOfJobsServed += s.served;
        }
        if (lastArrivalTime == 0 || numberOfJobsServed == 0 || servers[serverId].lastCompletionTime == 0.0) {
            return;
        }
        double lambda = numberOfJobsServed / lastArrivalTime;
        double meanNodePopulation = area.getNodeArea() / servers[serverId].lastCompletionTime;
        double meanResponseTime = meanNodePopulation / lambda;
        observations.saveObservation(run, Observations.INDEX.RESPONSE_TIME, meanResponseTime);
    }
}
