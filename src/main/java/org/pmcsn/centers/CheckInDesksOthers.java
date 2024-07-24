package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.libraries.Rvgs;
import org.pmcsn.model.*;

import java.util.*;

import static org.pmcsn.model.Statistics.computeMean;
import static org.pmcsn.utils.Distributions.*;
import static org.pmcsn.utils.Probabilities.getCheckInDesks;

public class CheckInDesksOthers {

    Rngs rngs;
    Rvgs rvgs;
    CheckInDesksSingleFlight[] checkInDesksSingleFlights;
    public int numberOfCenters;

    public CheckInDesksOthers() {
        this.checkInDesksSingleFlights = new CheckInDesksSingleFlight[19];
        for (int i = 0; i < checkInDesksSingleFlights.length; i++) {
            checkInDesksSingleFlights[i] = new CheckInDesksSingleFlight(i + 1);
            checkInDesksSingleFlights[i].CENTER_INDEX = 12 + (2 * i);
        }
        this.numberOfCenters = checkInDesksSingleFlights.length;
    }

    public void reset(Rngs rngs) {
        this.rngs = rngs;
        for (int i = 0; i < checkInDesksSingleFlights.length; i++) {
            checkInDesksSingleFlights[i].reset(rngs);
        }
    }

    public void processArrival(MsqEvent arrival, MsqTime time, List<MsqEvent> events) {
        int index = getCheckInDesks(rngs, 11); //TODO sameStreamIndex here???
        if (index < 1 || index > checkInDesksSingleFlights.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        checkInDesksSingleFlights[index - 1].processArrival(arrival, time, events);
    }


    public void processCompletion(MsqEvent completion, MsqTime time, List<MsqEvent> events) {
        int index = completion.centerID;
        if (index < 1 || index > checkInDesksSingleFlights.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        checkInDesksSingleFlights[index - 1].processCompletion(completion, time, events);
    }

    public long getNumberOfJobsInNode() {

        int numberOfJobsInNode = 0;

        for(int index=1; index<=checkInDesksSingleFlights.length; index++){
            numberOfJobsInNode += checkInDesksSingleFlights[index-1].numberOfJobsInNode;
        }

        return numberOfJobsInNode;
    }

    public void resetBatch(int center ) {
        checkInDesksSingleFlights[center].resetBatch();

    }

    public int getJobsServed(int center){

        int numberOfJobsServed = (int) checkInDesksSingleFlights[center].numberOfJobsServed;

        return numberOfJobsServed;
    }

    public int getBatchIndex(int center){
        return checkInDesksSingleFlights[center].batchIndex;
    }

    public int getMinBatchIndex() {
        // Assume there's at least one center in the array
        if (checkInDesksSingleFlights.length == 0) {
            throw new IllegalStateException("No centers available");
        }

        int minBatchIndex = checkInDesksSingleFlights[0].batchIndex;
        for (int i = 1; i < checkInDesksSingleFlights.length; i++) {
            if (checkInDesksSingleFlights[i].batchIndex < minBatchIndex) {
                minBatchIndex = checkInDesksSingleFlights[i].batchIndex;
            }
        }
        return minBatchIndex;
    }

    public void setArea(MsqTime time){

        for(int index=1; index<=checkInDesksSingleFlights.length; index++){
            checkInDesksSingleFlights[index-1].area += (time.next - time.current) * checkInDesksSingleFlights[index-1].numberOfJobsInNode;
        }
    }

    public void saveStats() {
        for(int index=1; index<=checkInDesksSingleFlights.length; index++){
            checkInDesksSingleFlights[index-1].saveStats();
        }
    }

    public void saveStats(int center) {
        checkInDesksSingleFlights[center].saveStats();
    }

    public void writeStats(String simulationType){
        for(int index=1; index<=checkInDesksSingleFlights.length; index++){
            checkInDesksSingleFlights[index-1].writeStats(simulationType);
        }
    }


    public Statistics.MeanStatistics getMeanStatistics(){
        List<Double> meanResponseTimeList = new ArrayList<>();
        List<Double> meanServiceTimeList = new ArrayList<Double>();
        List<Double> meanQueueTimeList = new ArrayList<Double>();
        List<Double> lambdaList = new ArrayList<Double>();
        List<Double> meanSystemPopulationList = new ArrayList<Double>();
        List<Double> meanUtilizationList = new ArrayList<Double>();
        List<Double> meanQueuePopulationList = new ArrayList<Double>();
        Statistics.MeanStatistics ms;

        // obtaining the mean for all centers
        for(int index=1; index<=checkInDesksSingleFlights.length; index++){
            ms = checkInDesksSingleFlights[index-1].getMeanStatistics();
            meanResponseTimeList.add(ms.meanResponseTime);
            meanServiceTimeList.add(ms.meanServiceTime);
            meanQueueTimeList.add(ms.meanQueueTime);
            lambdaList.add(ms.lambda);
            meanSystemPopulationList.add(ms.meanSystemPopulation);
            meanUtilizationList.add(ms.meanUtilization);
            meanQueuePopulationList.add(ms.meanQueuePopulation);
        }

        double meanResponseTime = computeMean(meanResponseTimeList);
        double meanServiceTime = computeMean(meanServiceTimeList);
        double meanQueueTime = computeMean(meanQueueTimeList);
        double lambda = computeMean(lambdaList);
        double meanSystemPopulation = computeMean(meanSystemPopulationList);
        double meanUtilization = computeMean(meanUtilizationList);
        double meanQueuePopulation = computeMean(meanQueuePopulationList);

        return new Statistics.MeanStatistics("CHECK-IN OTHERS", meanResponseTime, meanServiceTime, meanQueueTime, lambda, meanSystemPopulation, meanUtilization, meanQueuePopulation);

    }


        private class CheckInDesksSingleFlight {

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
        public static long  arrivalsCounter = 0;        /*number of arrivals*/
        long numberOfJobsInNode =0;                     /*number in the node*/
        static int    SERVERS = 3;                      /* number of servers*/
        long numberOfJobsServed = 0;                    /* number of processed jobs*/
        int CENTER_INDEX;                               /* index of center to select stream*/
        int centerID;
        double area   = 0.0;
        double service;
        double firstArrivalTime = Double.NEGATIVE_INFINITY;
        double lastArrivalTime = 0;
        double lastCompletionTime = 0;
            public int batchIndex = 0;

        Rngs rngs;

        MsqSum[] sum = new MsqSum[SERVERS];
        MsqServer[] servers = new MsqServer[SERVERS];

        public CheckInDesksSingleFlight(int centerID) {

            this.centerID = centerID;
            this.statistics = new Statistics("CHECK_IN_OTHERS_" + this.centerID);

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

        public long getNumberOfJobsInNode() {
                return numberOfJobsInNode;
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
                MsqEvent event = new MsqEvent(time.current + service, true, EventType.CHECK_IN_OTHERS_DONE, s, centerID);
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
                MsqEvent completion_event = new MsqEvent(time.current + service, true, EventType.CHECK_IN_OTHERS_DONE, s, centerID);
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
}
