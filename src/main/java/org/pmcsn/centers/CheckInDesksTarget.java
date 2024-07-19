package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.libraries.Rvgs;
import org.pmcsn.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.pmcsn.model.Statistics.printStats;
import static org.pmcsn.utils.Distributions.erlang;

public class CheckInDesksTarget {

    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Service times
     *  * Queue times
     *  * Interarrival times
     *  * Population
     *  * Utilizations
     *  * Queue population
     */

    Statistics statistics;

    //Constants and Variables
    public static long  arrivalsCounter = 0;        /*number of arrivals*/
    long numberOfJobsInNode =0;                     /*number in the node*/
    static int    SERVERS = 3;                      /* number of servers*/
    long numberOfJobsServed = 0;                         /* number of processed jobs*/
    static int CENTER_INDEX = 0;//TODO                    /* index of center to select stream*/
    double area   = 0.0;
    double service;

    Rngs rngs;
    Rvgs rvgs;

    MsqSum[] sum = new MsqSum [SERVERS + 1];
    MsqServer[] servers = new MsqServer [SERVERS + 1];

    public CheckInDesksTarget(Rngs rngs) {
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
        /* --------------------------------------------
         * generate the next service time with rate 2/3
         * --------------------------------------------
         */
    {
        rngs.selectStream(streamIndex);

        //TODO: cambiare i parametri
        return (erlang(5, 0.3, rngs));
    }

    public void computeAndPrintStats(int replicationIndex, MsqTime time, List<MsqEvent> events) {
        List<MsqEvent> checkInDesksTargetEvents = new ArrayList<>(events);
        checkInDesksTargetEvents.removeIf(event -> !(event.type==EventType.ARRIVAL_CHECK_IN_TARGET || event.type==EventType.CHECK_IN_TARGET_DONE));
        printStats("CHECK_IN_TARGET", SERVERS, numberOfJobsServed, this.area, this.sum, time, checkInDesksTargetEvents, replicationIndex);
    }

}
