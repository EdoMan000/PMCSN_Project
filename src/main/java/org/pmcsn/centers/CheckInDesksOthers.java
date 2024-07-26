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
        return (int) checkInDesksSingleFlights[center].getCompletions();
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
        for(CheckInDesksSingleFlight singleFlight : checkInDesksSingleFlights){
            singleFlight.updateArea(time.next - time.current);
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
        int CENTER_INDEX;                               /* index of center to select stream*/
        int centerID;
        private final Area area;
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
            this.area = new Area();
        }

        public void reset(Rngs rngs) {
            this.rngs = rngs;

            // resetting variables
            this.numberOfJobsInNode =0;
            area.reset();
            this.firstArrivalTime = Double.NEGATIVE_INFINITY;
            this.lastArrivalTime = 0;
            this.lastCompletionTime = 0;

            for(int i=0; i<SERVERS ; i++){
                sum[i].reset();
                servers[i].reset();
            }
        }

        public void resetBatch() {
            // resetting variables
            area.reset();
            this.firstArrivalTime = Double.NEGATIVE_INFINITY;
            this.lastArrivalTime = 0;
            this.lastCompletionTime = 0;

            for(int i=0; i<SERVERS ; i++){
                sum[i].reset();
                servers[i].reset();
            }
        }

        public void updateArea(double width) {
            // TODO: questo controllo dovrebbe avvenire nel loop principale
            if (numberOfJobsInNode > 0) {
                area.incNodeArea(width * numberOfJobsInNode);
                area.incQueueArea(width * (numberOfJobsInNode - 1));
                area.incServiceArea(width);
            }
        }

        public long getCompletions() {
            long numberOfJobsServed = 0;
            for(int i=0; i<SERVERS ; i++){
                numberOfJobsServed += sum[i].served;
            };
            return numberOfJobsServed;
        }

        public long getNumberOfJobsInNode() {
                return numberOfJobsInNode;
            }


        public void processArrival(MsqEvent arrival, MsqTime time, List<MsqEvent> events){
            int serverId;

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
                spawnCompletionEvent(time, events);
            }
        }

        public void processCompletion(MsqEvent completion, MsqTime time, List<MsqEvent> events) {
            //updating counters
            numberOfJobsInNode--;

            int serverId = completion.serverId;
            sum[serverId].service += completion.time;
            sum[serverId].served++;
            lastCompletionTime = completion.time;

            //remove the event since I'm processing it
            events.remove(completion);

            // generating arrival for the next center
            spawnNextCenterEvent(time, events);

            //checking if there are jobs in queue, if so the server starts processing one
            if (numberOfJobsInNode >= SERVERS) {
                spawnCompletionEvent(time, events, serverId);
            } else {
                //if there are no jobs in queue the server returns idle and updates the last completion time
                servers[serverId].lastCompletionTime = completion.time;
                servers[serverId].running = false;
            }
        }



        private void spawnNextCenterEvent(MsqTime time, List<MsqEvent> events) {
            MsqEvent next_center_event = new MsqEvent(time.current, true, EventType.ARRIVAL_BOARDING_PASS_SCANNERS, 0);
            events.add(next_center_event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        }

        private void spawnCompletionEvent(MsqTime time, List<MsqEvent> events, int serverId) {

//            service = getService(CENTER_INDEX+1);
//            sum[serverId].service += service;
//            sum[serverId].served++;
//
//            //generate a new completion event
//            MsqEvent completion_event = new MsqEvent(time.current + service, true, EventType.CHECK_IN_OTHERS_DONE, serverId, centerID);
//            events.add(completion_event);
//            events.sort(Comparator.comparing(MsqEvent::getTime));

            double service = getService(CENTER_INDEX+1);

            //generate a new completion event
            MsqEvent event = new MsqEvent(time.current + service, true, EventType.CHECK_IN_OTHERS_DONE, serverId, centerID);
            // TODO: inizializzare in costruttore
            event.service = service;
            events.add(event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        }

        private void spawnCompletionEvent(MsqTime time, List<MsqEvent> events) {

//            //generating service time
//            service         = getService(CENTER_INDEX);
//            // finding one idle server and updating server status
//            serverId               = findOne();
//            servers[serverId].running = true;
//            //update statistics
//            sum[serverId].service += service;
//            sum[serverId].served++;
//            //generate a new completion event
//            MsqEvent event = new MsqEvent(time.current + service, true, EventType.CHECK_IN_OTHERS_DONE, serverId, centerID);
//            events.add(event);
//            events.sort(Comparator.comparing(MsqEvent::getTime));

            double service = getService(CENTER_INDEX);
            // finding one idle server and updating server status
            int serverId               = findOne();
            servers[serverId].running = true;
            MsqEvent event = new MsqEvent(time.current + service, true, EventType.CHECK_IN_OTHERS_DONE, serverId, centerID);
            // TODO: inizializzare in costruttore
            event.service = service;
            events.add(event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
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
            statistics.saveStats(area, sum, lastArrivalTime, lastCompletionTime);
        }
        public void writeStats(String simulationType){
            statistics.writeStats(simulationType);
        }

        public Statistics.MeanStatistics getMeanStatistics() {
                return statistics.getMeanStatistics();
            }

    }
}
