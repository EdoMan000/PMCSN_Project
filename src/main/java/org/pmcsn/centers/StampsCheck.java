package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Probabilities.isPriority;
import static org.pmcsn.utils.Probabilities.isTargetFlight;

public class StampsCheck {

    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Service times
     *  * Queue times
     *  * Inter-arrival times
     *  * Population
     *  * Utilization
     *  * Queue population
     */

    Statistics statistics = new Statistics("STAMP_CHECK");

    //Constants and Variables
    public static long  arrivalsCounter = 0;        /* number of arrivals */
    long numberOfJobsInNode =0;                     /* number in the node */
    long numberOfJobsServed = 0;                    /* number of processed jobs */
    static int CENTER_INDEX = 59;                   /* index of center to select stream*/
    double area   = 0.0;
    double service;
    double firstArrivalTime = Double.NEGATIVE_INFINITY;
    double lastArrivalTime = 0;
    double lastCompletionTime = 0;

    Rngs rngs;

    MsqSum sum = new MsqSum();

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

        sum.served = 0;
        sum.service = 0;
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

        // increment the number of jobs in the node
        numberOfJobsInNode++;

        // Updating the first arrival time (we will use it in the statistics)
        if(firstArrivalTime == Double.NEGATIVE_INFINITY){
            firstArrivalTime = arrival.time;
        }
        lastArrivalTime = arrival.time;

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
        numberOfJobsServed++;
        numberOfJobsInNode--;

        lastCompletionTime = completion.time;

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
        // between 1 and 2 minutes
        //return (uniform(1 , 2, rngs));
        return (exponential(0.1, rngs));
    }

    public void saveStats() {
        MsqSum[] sums = new MsqSum[1];
        sums[0] = this.sum;
        statistics.saveStats(1, numberOfJobsServed, area, sums, firstArrivalTime, lastArrivalTime, lastCompletionTime);
    }
    public void writeStats(String simulationType){
        statistics.writeStats(simulationType);
    }

    public Statistics.MeanStatistics getMeanStatistics() {
        return statistics.getMeanStatistics();
    }

}
