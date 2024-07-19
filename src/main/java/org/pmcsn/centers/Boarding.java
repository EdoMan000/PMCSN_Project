package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.libraries.Rvgs;
import org.pmcsn.model.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.pmcsn.model.Statistics.printStats;
import static org.pmcsn.utils.Distributions.erlang;

public class Boarding {

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
    public static long  arrivalsCounter = 0;        /* number of arrivals */
    long numberOfJobsInNode =0;                     /* number in the node */
    static int    SERVERS = 2;                      /* number of servers */
    long numberOfJobsServed = 0;                         /* number of processed jobs */
    static int CENTER_INDEX = 0;//TODO                    /* index of center to select stream*/
    double area   = 0.0;
    double service;

    Rngs rngs;
    Rvgs rvgs;

    MsqSum[] sum = new MsqSum [SERVERS + 1];
    MsqServer[] servers = new MsqServer [SERVERS + 1];

    public Boarding(Rngs rngs) {
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
            MsqEvent event = new MsqEvent(time.current + service, true, EventType.BOARDING_DONE, s);
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

        //obtaining the server which is processing the job
        int s = completion.server;

        //checking if there are jobs in queue, if so the server starts processing one
        if (numberOfJobsInNode >= SERVERS) {
            service = getService(CENTER_INDEX+1);
            sum[s].service += service;
            sum[s].served++;

            //generate a new completion event
            MsqEvent event = new MsqEvent(time.current + service, true, EventType.BOARDING_DONE, s);
            events.add(event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        } else {
            //if there are no jobs in queue the server returns idle and updates the last completion time
            servers[s].lastCompletionTime = completion.time;
            servers[s].running = false;
        }
    }

    private boolean getTargetFlight(double beta) {
        rngs.selectStream(CENTER_INDEX+2);
        return rngs.random() < beta;
    }

    private boolean getCitizen(double beta) {
        rngs.selectStream(CENTER_INDEX+3);
        return rngs.random() < beta;
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

    public void computeAndPrintStats(int replicationIndex, MsqTime time, List<MsqEvent> events) {
        List<MsqEvent> boardingEvents = new ArrayList<>(events);
        boardingEvents.removeIf(event -> !(event.type==EventType.ARRIVAL_BOARDING || event.type==EventType.BOARDING_DONE));
        printStats("BOARDING", SERVERS, numberOfJobsServed, this.area, this.sum, time, boardingEvents, replicationIndex);
    }
}
