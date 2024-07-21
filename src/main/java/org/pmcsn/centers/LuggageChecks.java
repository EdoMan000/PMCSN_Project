package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Probabilities.getEntrance;
import static org.pmcsn.utils.Probabilities.isTargetFlight;

public class LuggageChecks {

    Rngs rngs;
    LuggageChecksSingleEntrance[] luggageChecksSingleEntrances;
    double sarrival;
    // all the times are measured in min
    int STOP = 180;
    boolean endOfArrivals = false;

    public LuggageChecks() {
        this.luggageChecksSingleEntrances = new LuggageChecksSingleEntrance[3];
        for (int i = 0; i < luggageChecksSingleEntrances.length; i++) {
            luggageChecksSingleEntrances[i] = new LuggageChecksSingleEntrance(i + 1);
            luggageChecksSingleEntrances[i].CENTER_INDEX = 1 + (3 * i);
        }
    }

    public void reset(Rngs rngs, double sarrival) {
        this.rngs = rngs;
        this.sarrival = sarrival;
        this.endOfArrivals = false;

        for (int i = 0; i < luggageChecksSingleEntrances.length; i++) {
            luggageChecksSingleEntrances[i].reset(rngs);
        }

    }


    public double getSarrival(){
        return sarrival;
    }

    public boolean isEndOfArrivals(){
        return endOfArrivals;
    }

    public void processArrival(MsqEvent arrival, MsqTime time, List<MsqEvent> events) {

        int index = getEntrance(rngs, 67);
        if (index < 1 || index > 3) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        luggageChecksSingleEntrances[index - 1].processArrival(arrival, time, events);

        // Generating a new arrival
        double nextArrival = getArrival();

        // Checking if the next arrival exceeds time limit
        if(nextArrival <= STOP){
            MsqEvent event = new MsqEvent(nextArrival, true, EventType.ARRIVAL_LUGGAGE_CHECK, 0);
            events.add(event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        } else {
            endOfArrivals = true;
        }

    }

    public void processCompletion(MsqEvent arrival, MsqTime time, List<MsqEvent> events){
        int index = arrival.centerID;
        if (index < 1 || index > 19) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        luggageChecksSingleEntrances[index - 1].processCompletion(arrival, time, events);
    }



    public double getArrival() {
        /* --------------------------------------------------------------
         * generate the next arrival time
         * --------------------------------------------------------------
         */
        rngs.selectStream(68);
        // 1/2 min inter-arrival time
        sarrival += exponential( 0.5, rngs);
        return (sarrival);
    }


    public long getNumberOfJobsInNode() {

        int numberOfJobsInNode = 0;

        for(int index=1; index<=3; index++){
            numberOfJobsInNode += luggageChecksSingleEntrances[index-1].numberOfJobsInNode;
        }

        return numberOfJobsInNode;
    }

    public void setArea(MsqTime time){

        for(int index=1; index<=3; index++){
            luggageChecksSingleEntrances[index-1].area += (time.next - time.current) * luggageChecksSingleEntrances[index-1].numberOfJobsInNode;
        }
    }


    public void saveStats() {
        for(int index=1; index<=3; index++){
            luggageChecksSingleEntrances[index-1].saveStats();
        }
    }

    public void writeStats(String simulationType){
        for(int index=1; index<=3; index++){
            luggageChecksSingleEntrances[index-1].writeStats(simulationType);
        }
    }

    private class LuggageChecksSingleEntrance {

        /*  STATISTICS OF INTEREST :
         *  * Response times
         *  * Service times
         *  * Queue times
         *  * Inter-arrival times
         *  * Population
         *  * Utilization
         *  * Queue population
         */

        Statistics statistics;

        //Constants and Variables
        public static long  arrivalsCounter = 0;        /* number of arrivals */
        long numberOfJobsInNode =0;                     /* number in the node */
        long numberOfJobsServed = 0;                    /* number of processed jobs */
        int CENTER_INDEX;                               /* index of center to select stream*/
        int centerID;
        double area   = 0.0;
        double service;
        double firstArrivalTime = Double.NEGATIVE_INFINITY;
        double lastArrivalTime = 0;
        double lastCompletionTime = 0;

        Rngs rngs;

        MsqSum sum = new MsqSum();

        public LuggageChecksSingleEntrance(int centerID) {
            this.centerID = centerID;
            this.statistics = new Statistics("LUGGAGE_CHECK_" + this.centerID);
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

            sum.served = 0;
            sum.service = 0;
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
                MsqEvent event = new MsqEvent(time.current + service, true, EventType.LUGGAGE_CHECK_DONE, 0, centerID);
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
                MsqEvent event = new MsqEvent(time.current, true, EventType.ARRIVAL_CHECK_IN_TARGET, 0);
                events.add(event);
                events.sort(Comparator.comparing(MsqEvent::getTime));
            }else{
                MsqEvent event = new MsqEvent(time.current, true, EventType.ARRIVAL_CHECK_IN_OTHERS, 0);
                events.add(event);
                events.sort(Comparator.comparing(MsqEvent::getTime));
            }

            //checking if there are jobs in queue, if so the server starts processing one
            if (numberOfJobsInNode > 0) {
                service = getService(CENTER_INDEX+1);
                sum.service += service;
                sum.served++;

                //generate a new completion event
                MsqEvent event = new MsqEvent(time.current + service, true, EventType.LUGGAGE_CHECK_DONE, 0, centerID);
                events.add(event);
                events.sort(Comparator.comparing(MsqEvent::getTime));
            }
        }

        public double getService(int streamIndex)
        {
            rngs.selectStream(streamIndex);

            // 5 min as mean service time
            return (exponential(5, rngs));
        }

        public void saveStats() {
            MsqSum[] sums = new MsqSum[1];
            sums[0] = this.sum;
            statistics.saveStats(1, numberOfJobsServed, area, sums, firstArrivalTime, lastArrivalTime, lastCompletionTime);
        }
        public void writeStats(String simulationType){
            statistics.writeStats(simulationType);
        }
    }
}
