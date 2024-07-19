package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.libraries.Rvgs;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqSum;
import org.pmcsn.model.MsqTime;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Distributions.uniform;
import static org.pmcsn.utils.Probabilities.getEntrance;
import static org.pmcsn.utils.Probabilities.isTargetFlight;

public class LuggageChecks {

    Rngs rngs;
    Rvgs rvgs;
    LuggageChecksSingleEntrance[] entrances;
    double sarrival;
    int STOP = 86400;
    boolean endOfArrivals = false;

    public LuggageChecks(Rngs rngs, double sarrival) {
        this.rngs = rngs;
        this.rvgs = new Rvgs(rngs);
        this.entrances = new LuggageChecksSingleEntrance[3];
        this.sarrival = sarrival;
    }

    public double getSarrival(){
        return sarrival;
    }

    public boolean isEndOfArrivals(){
        return endOfArrivals;
    }

    public void processArrival(MsqEvent arrival, MsqTime time, List<MsqEvent> events) {

        int centerID = getEntrance(rngs, 11);
        if (centerID < 1 || centerID > 3) {
            throw new IllegalArgumentException("Invalid centerID: " + centerID);
        }
        entrances[centerID - 1].processArrival(arrival, time, events);

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
        int centerID = arrival.centerID;
        if (centerID < 1 || centerID > 19) {
            throw new IllegalArgumentException("Invalid centerID: " + centerID);
        }
        entrances[centerID - 1].processCompletion(arrival, time, events);
    }



    public double getArrival() {
        /* --------------------------------------------------------------
         * generate the next arrival time, with rate 1/2
         * --------------------------------------------------------------
         */
        rngs.selectStream(0);
        sarrival += exponential(2.0, rngs);
        return (sarrival);
    }


    public long getNumberOfJobsInNode() {

        int numberOfJobsInNode = 0;
        int centerID = 0;

        for(centerID=0; centerID<3; centerID++){
            numberOfJobsInNode += entrances[centerID - 1].numberOfJobsInNode;
        }

        return numberOfJobsInNode;
    }

    private class LuggageChecksSingleEntrance {

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
        long processedJobs = 0;                         /* number of processed jobs */
        static int CENTER_INDEX;//TODO                    /* index of center to select stream*/
        int centerID;
        double area   = 0.0;
        double service;

        Rngs rngs;
        Rvgs rvgs;

        MsqSum sum = new MsqSum();

        public LuggageChecksSingleEntrance(Rngs rngs, int centerID) {
            this.rngs = rngs;
            this.centerID = centerID;
            this.rvgs = new Rvgs(rngs);
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
                MsqEvent event = new MsqEvent(time.current + service, true, EventType.LUGGAGE_CHECK_DONE, 0, centerID);
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

            //TODO parametri? erlang con k=10
            return (uniform(0, 10, rngs));
        }

    }
}
