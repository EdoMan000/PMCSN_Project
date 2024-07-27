package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;
import org.pmcsn.model.Statistics;

import java.util.Arrays;

public abstract class Multiserver {
    protected long numberOfJobsInNode = 0;
    protected int SERVERS;
    protected int CENTER_INDEX;
    protected Area area;
    protected double firstArrivalTime = Double.NEGATIVE_INFINITY;
    protected double lastArrivalTime = 0;
    protected double lastCompletionTime = 0;
    protected int batchIndex = 0;
    protected double meanServiceTime;
    protected String centerName;
    protected boolean approximateServiceAsExponential;
    protected Rngs rngs;

    protected MsqSum[] sum;
    protected MsqServer[] servers;

    protected Statistics statistics;

    public Multiserver(String centerName, double meanServiceTime, int numServers, int centerIndex, boolean approximateServiceAsExponential) {
        this.centerName = centerName;
        this.meanServiceTime = meanServiceTime;
        this.SERVERS = numServers;
        this.CENTER_INDEX = centerIndex;
        this.sum =  new MsqSum[SERVERS];
        this.servers = new MsqServer[SERVERS];
        for(int i=0; i<SERVERS ; i++){
            sum[i] = new MsqSum();
            servers[i] = new MsqServer();
        }
        this.area = new Area();
        this.statistics = new Statistics(centerName);
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

    public void resetBatch() {
        // resetting variables
        area.reset();
        this.firstArrivalTime = Double.NEGATIVE_INFINITY;
        this.lastArrivalTime = 0;
        this.lastCompletionTime = 0;

        for(int i=0; i<SERVERS ; i++){
            sum[i].reset();
            servers[i].reset();
        }
    }

    public long getJobsServed() {
        long numberOfJobsServed = 0;
        for(int i=0; i<SERVERS ; i++){
            numberOfJobsServed += sum[i].served;
        };
        return numberOfJobsServed;
    }

    public long getNumberOfJobsInNode() {
        return numberOfJobsInNode;
    }

    public void setArea(MsqTime time){
        double width = time.next - time.current;
        area.incNodeArea(width * numberOfJobsInNode);
        long busyServers = Arrays.stream(servers).filter(s -> s.running).count();
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
        if(s == SERVERS) return s;
        while (i < SERVERS-1) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */
            if (!servers[i].running && (servers[i].lastCompletionTime < servers[s].lastCompletionTime))
                s = i;
        }
        return (s);
    }

    public void saveStats() {
        batchIndex++;
        statistics.saveStats(area, sum, lastArrivalTime, lastCompletionTime, true);
    }
    public void writeStats(String simulationType){
        statistics.writeStats(simulationType);
    }

    public Statistics.MeanStatistics getMeanStatistics() {
        return statistics.getMeanStatistics();
    }


}
