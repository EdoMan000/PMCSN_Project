package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.libraries.Rvgs;
import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;
import java.util.Random;

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

    //Constants and Variables
    public static long  arrivalsCounter = 0;        /* number of arrivals */
    long numberOfJobsInNode =0;                     /* number in the node */
    static int    SERVERS = 2;                      /* number of servers */
    long processedJobs = 0;                         /* number of processed jobs */
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
            MsqEvent event = new MsqEvent(time.current + service, true, EventType.SECURITY_CHECK_DONE, s);
            events.add(event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        }
    }

    public void processCompletion(MsqEvent completion, MsqTime time, List<MsqEvent> events) {
        //updating counters
        processedJobs++;
        numberOfJobsInNode--;

        //remove the event since I'm processing it
        events.remove(completion);

        //obtaining the server which is processing the job
        int s = completion.server;

        if(getCitizen(0.2)){
            if(getTargetFlight(0.0159)){
                /* generate an arrival at boarding*/
                MsqEvent event = new MsqEvent(time.current, true, EventType.ARRIVAL_BOARDING, 0);
                events.add(event);
                events.sort(Comparator.comparing(MsqEvent::getTime));
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
        return (erlang(2, 0.3));
    }

    public double erlang(long n, double b)
        /* ==================================================
         * Returns an Erlang distributed positive real number.
         * NOTE: use n > 0 and b > 0.0
         * ==================================================
         */
    {
        long   i;
        double x = 0.0;

        for (i = 0; i < n; i++)
            x += exponential(b);
        return (x);
    }

    public double exponential(double m)
        /* =========================================================
         * Returns an exponentially distributed positive real number.
         * NOTE: use m > 0.0
         * =========================================================
         */
    {
        return (-m * Math.log(1.0 - rngs.random()));
    }
}
