package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.libraries.Rvgs;
import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

public class BoardingPassScanners {

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
    public static long  arrivalsCounter = 0;        /*number of arrivals*/
    long numberOfJobsInNode =0;                     /*number in the node*/
    static int    SERVERS = 3;                      /* number of servers*/
    long processedJobs = 0;                         /* number of processed jobs*/
    double area   = 0.0;
    double service;

    Rngs rngs;
    Rvgs rvgs;

    MsqSum[] sum = new MsqSum [SERVERS + 1];
    MsqServer[] servers = new MsqServer [SERVERS + 1];

    public BoardingPassScanners(Rngs rngs) {
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

            //TODO: per il momento sto usando lo stream 1 ma ricorda di cambiare
            // (ad ogni centro due streamIndex uno per l'arrivo e uno per il completamento)

            //generating service time
            service         = getService(1);
            // finding one idle server and updating server status
            s               = findOne();
            servers[s].running = true;
            //update statistics
            sum[s].service += service;
            sum[s].served++;
            //generate a new completion event
            MsqEvent event = new MsqEvent(time.current + service, true, EventType.BOARDING_PASS_DONE, s);
            events.add(event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        }
    }


    // The following stuff is copied from the library with some modifications ----------------------------------------

    public void processCompletion(MsqEvent completion, MsqTime time, List<MsqEvent> events) {
        //updating counters
        processedJobs++;
        numberOfJobsInNode--;

        //obtaining the server which is processing the job
        int s = completion.server;

        //checking if there are jobs in queue, if so the server starts processing one
        if (numberOfJobsInNode >= SERVERS) {
            service = getService(1);
            sum[s].service += service;
            sum[s].served++;

            //generate a new completion event
            MsqEvent event = new MsqEvent(time.current + service, true, EventType.BOARDING_PASS_DONE, s);
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
        /* --------------------------------------------
         * generate the next service time with rate 2/3
         * --------------------------------------------
         */
    {
        rngs.selectStream(streamIndex);

        //TODO: cambiare i parametri
        return (erlang(5, 0.3));
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

}
