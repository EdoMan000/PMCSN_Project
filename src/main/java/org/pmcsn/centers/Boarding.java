package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;

public class Boarding { //TODO gestione priorità per calcolo delle statistiche

    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Service times
     *  * Queue times
     *  * Inter-arrival times
     *  * Population
     *  * Utilization
     *  * Queue population
     */

    Statistics statistics = new Statistics("BOARDING");

    Statistics batchStatistics = new Statistics("BOARDING");

    //Constants and Variables
    public static long  arrivalsCounter = 0;        /* number of arrivals */
    long numberOfJobsInNode =0;                     /* number in the node */
    static int    SERVERS = 2;                      /* number of servers */
    static int CENTER_INDEX = 63;                   /* index of center to select stream*/
    private final Area area;
    double firstArrivalTime = Double.NEGATIVE_INFINITY;
    double lastArrivalTime = 0;
    double lastCompletionTime = 0;
    public int batchIndex = 0;

    Rngs rngs;

    MsqSum[] sum = new MsqSum[SERVERS];
    MsqServer[] servers = new MsqServer[SERVERS];

    public Boarding() {

        for(int i=0; i<SERVERS ; i++){
            sum[i] = new MsqSum();
            servers[i] = new MsqServer();
        }
        this.area = new Area();
    }

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
        // TODO: questo controllo dovrebbe avvenire nel loop principale
        if (numberOfJobsInNode > 0) {
            area.incNodeArea(width * numberOfJobsInNode);
            area.incQueueArea(width * (numberOfJobsInNode - 1));
            area.incServiceArea(width);
        }
    }
    public void setArea(MsqTime time){
        updateArea(time.next - time.current);
    }

    public void processArrival(MsqEvent arrival, MsqTime time, List<MsqEvent> events){
        int serverId;

        // Updating the first arrival time (we will use it in the statistics)
        if(firstArrivalTime == Double.NEGATIVE_INFINITY){
            firstArrivalTime = arrival.time;
        }
        lastArrivalTime = arrival.time;

        // increment the number of jobs in the node
        numberOfJobsInNode++;

        //remove the event since I'm processing it
        events.remove(arrival);

        if (numberOfJobsInNode <= SERVERS) {
            spawnCompletionEvent(time, events);
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

        //checking if there are jobs in queue, if so the server starts processing one
        if (numberOfJobsInNode >= SERVERS) {
            spawnCompletionEvent(time, events, serverId);
        } else {
            //if there are no jobs in queue the server returns idle and updates the last completion time
            servers[serverId].lastCompletionTime = completion.time;
            servers[serverId].running = false;
        }
    }

    private void spawnCompletionEvent(MsqTime time, List<MsqEvent> events, int serverId) {

//        service = getService(CENTER_INDEX+1);
//        sum[serverId].service += service;
//        sum[serverId].served++;
//
//        //generate a new completion event
//        MsqEvent event = new MsqEvent(time.current + service, true, EventType.BOARDING_DONE, serverId);
//        events.add(event);
//        events.sort(Comparator.comparing(MsqEvent::getTime));

        double service = getService(CENTER_INDEX+1);

        //generate a new completion event
        MsqEvent event = new MsqEvent(time.current + service, true, EventType.BOARDING_DONE, serverId);
        // TODO: inizializzare in costruttore
        event.service = service;
        events.add(event);
        events.sort(Comparator.comparing(MsqEvent::getTime));
    }

    private void spawnCompletionEvent(MsqTime time, List<MsqEvent> events) {

//        //generating service time
//        service         = getService(CENTER_INDEX);
//        // finding one idle server and updating server status
//        serverId               = findOne();
//        servers[serverId].running = true;
//        //update statistics
//        sum[serverId].service += service;
//        sum[serverId].served++;
//        //generate a new completion event
//        MsqEvent event = new MsqEvent(time.current + service, true, EventType.BOARDING_DONE, serverId);
//        events.add(event);
//        events.sort(Comparator.comparing(MsqEvent::getTime));

        double service = getService(CENTER_INDEX);
        // finding one idle server and updating server status
        int serverId               = findOne();
        servers[serverId].running = true;
        MsqEvent event = new MsqEvent(time.current + service, true, EventType.BOARDING_DONE, serverId);
        // TODO: inizializzare in costruttore
        event.service = service;
        events.add(event);
        events.sort(Comparator.comparing(MsqEvent::getTime));
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

    public double getService(int streamIndex)
    {
        rngs.selectStream(streamIndex);
        // 2 min as mean time
        // 0,4 min as std dev
        //return (logNormal(2, 0.4, rngs));
        return exponential(2, rngs);
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
