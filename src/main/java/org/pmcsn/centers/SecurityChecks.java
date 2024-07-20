package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.libraries.Rvgs;
import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.erlang;
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
    static int    SERVERS = 4;                      /* number of servers */
    long numberOfJobsServed = 0;                    /* number of processed jobs */
    static int CENTER_INDEX = 52;                   /* index of center to select stream*/
    double area   = 0.0;
    double service;
    double firstArrivalTime = Double.NEGATIVE_INFINITY;
    double lastArrivalTime = 0;
    double lastCompletionTime = 0;

    Rngs rngs;
    Rvgs rvgs;

    MsqSum[] sum = new MsqSum [SERVERS + 1];
    MsqServer[] servers = new MsqServer [SERVERS + 1];

    public SecurityChecks(Rngs rngs) {
        this.rngs = rngs;
        this.rvgs = new Rvgs(rngs);
    }

    public long getNumberOfJobsInNode() {
        return numberOfJobsInNode;
    }

    public void setArea(MsqTime time){
        area += (time.next - time.current) * numberOfJobsInNode;
    }

    public void processArrival(MsqEvent arrival, MsqTime time, List<MsqEvent> events){
        int s;

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
            //generating service time
            service         = getService(CENTER_INDEX);
            // finding one idle server and updating server status
            s               = findOne();
            servers[s].running = true;
            //update statistics
            sum[s].service += service;
            sum[s].served++;
            //generate a new completion event
            MsqEvent event = new MsqEvent(time.current + service, true, EventType.SECURITY_CHECK_DONE, s);
            events.add(event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        }
    }

    public void processCompletion(MsqEvent completion, MsqTime time, List<MsqEvent> events) {
        //updating counters
        numberOfJobsServed++;
        numberOfJobsInNode--;

        lastCompletionTime = completion.time;

        //remove the event since I'm processing it
        events.remove(completion);

        //obtaining the server which is processing the job
        int s = completion.server;

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

        //checking if there are jobs in queue, if so the server starts processing one
        if (numberOfJobsInNode >= SERVERS) {
            service = getService(CENTER_INDEX+1);
            sum[s].service += service;
            sum[s].served++;

            //generate a new completion event
            MsqEvent event = new MsqEvent(time.current + service, true, EventType.SECURITY_CHECK_DONE, s);
            events.add(event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        } else {
            //if there are no jobs in queue the server returns idle and updates the last completion time
            servers[s].lastCompletionTime = completion.time;
            servers[s].running = false;
        }
    }

    int findOne() {
        /* -----------------------------------------------------
         * return the index of the available server idle longest
         * -----------------------------------------------------
         */
        int s;
        int i = 0;

        while (servers[i].running)       /* find the index of the first available */
            i++;                        /* (idle) server                         */
        s = i;
        while (i < SERVERS) {         /* now, check the others to find which   */
            i++;                        /* has been idle longest                 */
            if (!servers[i].running && (servers[i].lastCompletionTime < servers[s].lastCompletionTime))
                s = i;
        }
        return (s);
    }

    public double getService(int streamIndex)
    {
        rngs.selectStream(streamIndex);

        //TODO parametri? erlang con k=2
        return (erlang(2, 0.3, rngs));
    }

    public void saveStats() {
        statistics.saveStats(SERVERS, numberOfJobsServed, area, sum, firstArrivalTime, lastArrivalTime, lastCompletionTime);
    }
    public void writeStats(String simulationType){
        statistics.writeStats(simulationType);
    }
}
