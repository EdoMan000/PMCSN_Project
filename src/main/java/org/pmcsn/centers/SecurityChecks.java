package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Probabilities.*;

public class SecurityChecks {

    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Service times
     *  * Queue times
     *  * Inter-arrival times
     *  * Population
     *  * Utilization
     *  * Queue population
     */

    Statistics statistics = new Statistics("SECURITY_CHECKS");

    //Constants and Variables
    public static long  arrivalsCounter = 0;        /* number of arrivals */
    long numberOfJobsInNode =0;                     /* number in the node */
    static int    SERVERS = 8;                      /* number of servers */
    static int CENTER_INDEX = 52;                   /* index of center to select stream*/
    private final Area area;
    double firstArrivalTime = Double.NEGATIVE_INFINITY;
    double lastArrivalTime = 0;
    double lastCompletionTime = 0;
    public int batchIndex = 0;

    Rngs rngs;

    MsqSum[] sum = new MsqSum[SERVERS];
    MsqServer[] servers = new MsqServer[SERVERS];

    public SecurityChecks() {

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

    public long getJobsServed(){
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

    private void spawnNextCenterEvent(MsqTime time, List<MsqEvent> events) {
        if(isCitizen(rngs,CENTER_INDEX+2)){
            if(isTargetFlight(rngs, CENTER_INDEX+3)){
                if(isPriority(rngs, CENTER_INDEX+4)){
                    /* generate an arrival at boarding with priority*/
                    MsqEvent event = new MsqEvent(time.current, true, EventType.ARRIVAL_BOARDING, 0, true);
                    events.add(event);
                    events.sort(Comparator.comparing(MsqEvent::getTime));
                } else {
                    /* generate an arrival at boarding without priority*/
                    MsqEvent event = new MsqEvent(time.current, true, EventType.ARRIVAL_BOARDING, 0);
                    events.add(event);
                    events.sort(Comparator.comparing(MsqEvent::getTime));
                }
            }
        }else{
            /* generate an arrival at passport check*/
            MsqEvent event = new MsqEvent(time.current, true, EventType.ARRIVAL_PASSPORT_CHECK, 0);
            events.add(event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        }
    }

    private void spawnCompletionEvent(MsqTime time, List<MsqEvent> events, int serverId) {

//        service = getService(CENTER_INDEX+1);
//        sum[s].service += service;
//        sum[s].served++;
//
//        //generate a new completion event
//        MsqEvent event = new MsqEvent(time.current + service, true, EventType.SECURITY_CHECK_DONE, s);
//        events.add(event);
//        events.sort(Comparator.comparing(MsqEvent::getTime));

        double service = getService(CENTER_INDEX+1);

        //generate a new completion event
        MsqEvent event = new MsqEvent(time.current + service, true, EventType.SECURITY_CHECK_DONE, serverId);
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
//        MsqEvent event = new MsqEvent(time.current + service, true, EventType.SECURITY_CHECK_DONE, serverId);
//        events.add(event);
//        events.sort(Comparator.comparing(MsqEvent::getTime));

        double service = getService(CENTER_INDEX);
        // finding one idle server and updating server status
        int serverId               = findOne();
        servers[serverId].running = true;
        MsqEvent event = new MsqEvent(time.current + service, true, EventType.SECURITY_CHECK_DONE, serverId);
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

        // 5 min as mean service time
        // 1 min as standard deviation
        //return (logNormal(5, 1, rngs));
        return (exponential(1.8, rngs));
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
