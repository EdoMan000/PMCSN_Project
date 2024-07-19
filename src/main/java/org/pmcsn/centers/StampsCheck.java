package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.libraries.Rvgs;
import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.uniform;
import static org.pmcsn.utils.Probabilities.isPriority;
import static org.pmcsn.utils.Probabilities.isTargetFlight;

public class StampsCheck {

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
    long processedJobs = 0;                         /* number of processed jobs */
    static int CENTER_INDEX = 0;//TODO                    /* index of center to select stream*/
    double area   = 0.0;
    double service;

    Rngs rngs;
    Rvgs rvgs;

    MsqSum sum = new MsqSum();

    public StampsCheck(Rngs rngs) {
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

        // increment the number of jobs in the node
        numberOfJobsInNode++;

        //remove the event since I'm processing it
        events.remove(arrival);

        if (numberOfJobsInNode == 1) {
            //generating service time
            service         = getService(CENTER_INDEX);

            //update statistics
            sum.service += service;
            sum.served++;
            //generate a new completion event
            MsqEvent event = new MsqEvent(time.current + service, true, EventType.STAMP_CHECK_DONE, 0);
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

        // generating arrival for the next center
        if(isTargetFlight(rngs, CENTER_INDEX+2)){
            if(isPriority(rngs, CENTER_INDEX+3)){
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

        //checking if there are jobs in queue, if so the server starts processing one
        if (numberOfJobsInNode > 0) {
            service = getService(CENTER_INDEX+1);
            sum.service += service;
            sum.served++;

            //generate a new completion event
            MsqEvent event = new MsqEvent(time.current + service, true, EventType.STAMP_CHECK_DONE, 0);
            events.add(event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        }
    }

    public double getService(int streamIndex)
    {
        rngs.selectStream(streamIndex);

        //TODO parametri? erlang con k=10
        return (uniform(0, 10, rngs));
    }

}
