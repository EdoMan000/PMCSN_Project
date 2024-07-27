package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.List;

public abstract class Multiserver {

    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Service times
     *  * Queue times
     *  * Inter-arrival times
     *  * Population
     *  * Utilization
     *  * Queue population
     */

    public Multiserver(String centerName, double meanServiceTime, int numServers, int centerIndex) {
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
    }

    //********************************** VARIABLES *********************************************
    protected long numberOfJobsInNode =0;                     /* number in the node */
    protected int    SERVERS;                     /* number of servers */
    protected int CENTER_INDEX;                   /* index of center to select stream*/
    protected Area area;
    protected double firstArrivalTime = Double.NEGATIVE_INFINITY;
    protected double lastArrivalTime = 0;
    protected double lastCompletionTime = 0;
    protected int batchIndex = 0;
    protected double meanServiceTime;
    protected String centerName;

    protected Rngs rngs;

    protected MsqSum[] sum;
    protected MsqServer[] servers;

    protected Statistics statistics;

    //********************************** ABSTRACT METHODS *********************************************
    abstract void spawnNextCenterEvent(MsqTime time, List<MsqEvent> events);
    abstract void spawnCompletionEvent(MsqTime time, List<MsqEvent> events, int serverId);
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

    public void updateArea(double width) {

        area.incNodeArea(width * numberOfJobsInNode);

        long height;
        if (getNumberIdleServers() > 0) height = 0;
        else height = numberOfJobsInNode - SERVERS;

        area.incQueueArea(width * height);
        area.incServiceArea(width);

    }

    public void setArea(MsqTime time){
        updateArea(time.next - time.current);
    }

    public void processArrival(MsqEvent arrival, MsqTime time, List<MsqEvent> events){

        // increment the number of jobs in the node
        numberOfJobsInNode++;

        // Updating the first arrival time (we will use it in the statistics)
        if(firstArrivalTime == Double.NEGATIVE_INFINITY){
            firstArrivalTime = arrival.time;
        }
        lastArrivalTime = arrival.time;

        //remove the event since I'm processing it
        events.remove(arrival);

        if (numberOfJobsInNode <= SERVERS) {
            int serverId               = findOne();
            servers[serverId].running = true;
            spawnCompletionEvent(time, events, serverId);
        }
    }

    public void processCompletion(MsqEvent completion, MsqTime time, List<MsqEvent> events) {
        //updating counters
        numberOfJobsInNode--;

        int serverId = completion.serverId;
        sum[serverId].service += completion.time;
        sum[serverId].served++;
        lastCompletionTime = completion.time;

        //remove the event since I'm processing it
        events.remove(completion);

        // generating arrival for the next center
        spawnNextCenterEvent(time, events);

        //checking if there are jobs in queue, if so the server starts processing one
        if (numberOfJobsInNode >= SERVERS) {
            spawnCompletionEvent(time, events, serverId);
        } else {
            //if there are no jobs in queue the server returns idle and updates the last completion time
            servers[serverId].lastCompletionTime = completion.time;
            servers[serverId].running = false;
        }
    }

    public int findOne() {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */

        int s;
        int i = 0;

        while (servers[i].running)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;

        // if it's the last server then simply return
        if(s == SERVERS) return s;

        while (i < SERVERS-1) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */

            if (!servers[i].running && (servers[i].lastCompletionTime < servers[s].lastCompletionTime))
                s = i;
        }
        return (s);
    }

    public int getNumberIdleServers(){
        int i;
        int num = 0;

        for(i=0; i<SERVERS; i++){
            if(!servers[i].running) num++;
        }

        return num;
    }

    public void saveStats() {
        batchIndex++;
        statistics.saveStats(area, sum, lastArrivalTime, lastCompletionTime);
    }
    public void writeStats(String simulationType){
        statistics.writeStats(simulationType);
    }

    public Statistics.MeanStatistics getMeanStatistics() {
        return statistics.getMeanStatistics();
    }


}
