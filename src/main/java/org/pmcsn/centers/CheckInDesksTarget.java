package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;

public class CheckInDesksTarget {

    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Service times
     *  * Queue times
     *  * Inter-arrival times
     *  * Population
     *  * Utilization
     *  * Queue population
     */

    Statistics statistics = new Statistics("CHECK_IN_TARGET");

    //Constants and Variables
    public static long  arrivalsCounter = 0;        /*number of arrivals*/
    long numberOfJobsInNode =0;                     /*number in the node*/
    static int    SERVERS = 3;                      /* number of servers*/
    long numberOfJobsServed = 0;                    /* number of processed jobs*/
    static int CENTER_INDEX = 10;                   /* index of center to select stream*/
    double area   = 0.0;
    double service;
    double firstArrivalTime = Double.NEGATIVE_INFINITY;
    double lastArrivalTime = 0;
    double lastCompletionTime = 0;
    public int batchIndex = 0;

    Rngs rngs;

    MsqSum[] sum = new MsqSum[SERVERS];
    MsqServer[] servers = new MsqServer[SERVERS];

    public CheckInDesksTarget() {

        for(int i=0; i<SERVERS ; i++){
            sum[i] = new MsqSum();
            servers[i] = new MsqServer();
        }
    }

    public void reset(Rngs rngs) {
        this.rngs = rngs;

        // resetting variables
        this.numberOfJobsInNode =0;
        this.numberOfJobsServed = 0;
        this.area   = 0.0;
        this.service = 0;
        this.firstArrivalTime = Double.NEGATIVE_INFINITY;
        this.lastArrivalTime = 0;
        this.lastCompletionTime = 0;

        for(int i=0; i<SERVERS ; i++){
            sum[i].served = 0;
            sum[i].service = 0;
            servers[i].running = false;
            servers[i].lastCompletionTime = 0;
        }
    }

    public void resetBatch() {
        // resetting variables
        this.numberOfJobsServed = 0;
        this.area   = 0.0;
        this.service = 0;
        this.firstArrivalTime = Double.NEGATIVE_INFINITY;
        this.lastArrivalTime = 0;
        this.lastCompletionTime = 0;

        for(int i=0; i<SERVERS ; i++){
            sum[i].served = 0;
            sum[i].service = 0;
            servers[i].running = false;
            servers[i].lastCompletionTime = 0;
        }
    }

    public long getJobsServed(){
        return numberOfJobsServed;
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
            MsqEvent event = new MsqEvent(time.current + service, true, EventType.CHECK_IN_TARGET_DONE, s);
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

        // generating arrival for the next center
        MsqEvent next_center_event = new MsqEvent(time.current, true, EventType.ARRIVAL_BOARDING_PASS_SCANNERS, 0);
        events.add(next_center_event);
        events.sort(Comparator.comparing(MsqEvent::getTime));

        //obtaining the server which is processing the job
        int s = completion.server;

        //checking if there are jobs in queue, if so the server starts processing one
        if (numberOfJobsInNode >= SERVERS) {
            service = getService(CENTER_INDEX+1);
            sum[s].service += service;
            sum[s].served++;

            //generate a new completion event
            MsqEvent completion_event = new MsqEvent(time.current + service, true, EventType.CHECK_IN_TARGET_DONE, s);
            events.add(completion_event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        } else {
            //if there are no jobs in queue the server returns idle and updates the last completion time
            servers[s].lastCompletionTime = completion.time;
            servers[s].running = false;
        }
    }


    // The following stuff is copied from the library with some modifications ----------------------------------------

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
        /* --------------------------------------------
         * generate the next service time with rate 2/3
         * --------------------------------------------
         */
    {
        rngs.selectStream(streamIndex);
        // mean time 10 min
        // std dev 2 min (20% since it has low variability)
        //return (logNormal(10, 2, rngs));
        return exponential(10, rngs);
    }


    public void saveStats() {
        batchIndex++;
        statistics.saveStats(SERVERS, numberOfJobsServed, area, sum, firstArrivalTime, lastArrivalTime, lastCompletionTime);
    }
    public void writeStats(String simulationType){
        statistics.writeStats(simulationType);
    }

    public Statistics.MeanStatistics getMeanStatistics() {
        return statistics.getMeanStatistics();
    }

}
