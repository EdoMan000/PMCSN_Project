package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;
import org.pmcsn.model.Statistics.MeanStatistics;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import static org.pmcsn.model.Statistics.computeMean;
import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Probabilities.getEntrance;
import static org.pmcsn.utils.Probabilities.isTargetFlight;

public class LuggageChecks {
    Rngs rngs;
    LuggageChecksSingleEntrance[] luggageChecksSingleEntrances;
    private final double interArrivalTime;
    double sarrival;
    // all the times are measured in min
    int STOP = 1440;
    boolean endOfArrivals = false;

    public LuggageChecks(int numberOfCenters, double interArrivalTime, double meanServiceTime) {
        this.interArrivalTime = interArrivalTime;
        this.luggageChecksSingleEntrances = new LuggageChecksSingleEntrance[numberOfCenters]; //TODO risistemare CENTER_INDEX
        for (int i = 0; i < luggageChecksSingleEntrances.length; i++) {
            luggageChecksSingleEntrances[i] = new LuggageChecksSingleEntrance(i + 1, 1 + 3 * i, meanServiceTime);
        }
    }

    public void reset(Rngs rngs, double sarrival) {
        this.rngs = rngs;
        this.sarrival = sarrival;
        this.endOfArrivals = false;
        for (LuggageChecksSingleEntrance center : luggageChecksSingleEntrances) {
            center.reset(rngs);
        }
    }

    public void resetBatch(int center ) {
        luggageChecksSingleEntrances[center].resetBatch();
    }

    public void setSTOP(int STOP) {
        this.STOP = STOP;
    }

    public boolean isEndOfArrivals(){
        return endOfArrivals;
    }

    public void processArrival(MsqEvent arrival, MsqTime time, EventQueue queue) {
        int index = getEntrance(rngs, 67);
        if (index < 1 || index > luggageChecksSingleEntrances.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        luggageChecksSingleEntrances[index - 1].processArrival(arrival, time, queue);

        // Generating a new arrival
        double nextArrival = getArrival();

        // Checking if the next arrival exceeds time limit
        if(nextArrival < STOP){
            MsqEvent event = new MsqEvent(EventType.ARRIVAL_LUGGAGE_CHECK, nextArrival);
            queue.add(event);
        } else {
            endOfArrivals = true;
        }

    }

    public void processCompletion(MsqEvent arrival, MsqTime time, EventQueue queue){
        int index = arrival.nodeId;
        if (index < 1 || index > luggageChecksSingleEntrances.length) {
            throw new IllegalArgumentException("Invalid centerID: " + index);
        }
        luggageChecksSingleEntrances[index - 1].processCompletion(arrival, time, queue);
    }

    public double getArrival() {
        rngs.selectStream(68);
        sarrival += exponential(interArrivalTime, rngs);
        return (sarrival);
    }

    public long getNumberOfJobsInNode() {
        return Arrays.stream(luggageChecksSingleEntrances).mapToLong(c -> c.numberOfJobsInNode).sum();
    }

    public long getTotalNumberOfJobsServed() {
        return Arrays.stream(luggageChecksSingleEntrances).mapToLong(LuggageChecksSingleEntrance::getCompletions).sum();
    }

    public int getJobsServed(int center){
        return (int) luggageChecksSingleEntrances[center].getCompletions();
    }

    public void setArea(MsqTime time){
        for (LuggageChecksSingleEntrance singleEntrance : luggageChecksSingleEntrances) {
            singleEntrance.updateArea(time.next - time.current);
        }
    }

    public int getBatchIndex(int center){
        return luggageChecksSingleEntrances[center].batchIndex;
    }

    public int getMinBatchIndex() {
        // Assume there's at least one center in the array
        if (luggageChecksSingleEntrances.length == 0) {
            throw new IllegalStateException("No centers available");
        }

        int minBatchIndex = luggageChecksSingleEntrances[0].batchIndex;
        for (int i = 1; i < luggageChecksSingleEntrances.length; i++) {
            if (luggageChecksSingleEntrances[i].batchIndex < minBatchIndex) {
                minBatchIndex = luggageChecksSingleEntrances[i].batchIndex;
            }
        }
        return minBatchIndex;
    }

    public void saveStats() {
        for (LuggageChecksSingleEntrance center : luggageChecksSingleEntrances) {
            center.saveStats();
        }
    }

    public void saveStats(int center) {
        luggageChecksSingleEntrances[center].saveStats();
    }

    public void writeStats(String simulationType){
        for (LuggageChecksSingleEntrance center : luggageChecksSingleEntrances) {
            center.writeStats(simulationType);
        }
    }

    public MeanStatistics getMeanStatistics(){
        List<Double> meanResponseTimeList = new ArrayList<>();
        List<Double> meanServiceTimeList = new ArrayList<Double>();
        List<Double> meanQueueTimeList = new ArrayList<Double>();
        List<Double> lambdaList = new ArrayList<Double>();
        List<Double> meanSystemPopulationList = new ArrayList<Double>();
        List<Double> meanUtilizationList = new ArrayList<Double>();
        List<Double> meanQueuePopulationList = new ArrayList<Double>();
        MeanStatistics ms;

        // obtaining the mean for all centers
        for(LuggageChecksSingleEntrance c : luggageChecksSingleEntrances){
            ms = c.getMeanStatistics();
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

        return new MeanStatistics("LUGGAGE CHECK", meanResponseTime, meanServiceTime, meanQueueTime, lambda, meanSystemPopulation, meanUtilization, meanQueuePopulation);
    }

    private static class LuggageChecksSingleEntrance {

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

        long numberOfJobsInNode = 0;                     /* number in the node */
        int centerIndex;                               /* index of center to select stream*/
        int centerID;
        private final double meanServiceTime;
        private final Area area;
        double firstArrivalTime = Double.NEGATIVE_INFINITY;
        double lastArrivalTime = 0;
        double lastCompletionTime = 0;
        public int batchIndex = 0;

        Rngs rngs;

        MsqSum sum = new MsqSum();

        public LuggageChecksSingleEntrance(int centerID, int centerIndex, double meanServiceTime) {
            this.centerID = centerID;
            this.centerIndex = centerIndex;
            this.statistics = new Statistics("LUGGAGE_CHECK_" + this.centerID);
            this.meanServiceTime = meanServiceTime;
            this.area = new Area();
        }


        public void reset(Rngs rngs) {
            this.rngs = rngs;
            // resetting variables
            this.numberOfJobsInNode = 0;
            this.firstArrivalTime = Double.NEGATIVE_INFINITY;
            this.lastArrivalTime = 0;
            this.lastCompletionTime = 0;
            area.reset();
            sum.reset();
        }

        public void resetBatch() {
            // resetting variables
            this.firstArrivalTime = Double.NEGATIVE_INFINITY;
            this.lastArrivalTime = 0;
            this.lastCompletionTime = 0;
            area.reset();
            sum.reset();
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
            return sum.served;
        }


        public void processArrival(MsqEvent arrival, MsqTime time, EventQueue queue){
            numberOfJobsInNode++;
            if(firstArrivalTime == Double.NEGATIVE_INFINITY){
                firstArrivalTime = arrival.time;
            }
            lastArrivalTime = arrival.time;
            if (numberOfJobsInNode == 1) {
                spawnCompletionEvent(time, queue);
            }
        }

        public void processCompletion(MsqEvent completion, MsqTime time, EventQueue queue) {
            numberOfJobsInNode--;
            sum.service += completion.service;
            sum.served++;
            lastCompletionTime = completion.time;
            spawnNextCenterEvent(time, queue);
            if (numberOfJobsInNode > 0) {
                spawnCompletionEvent(time, queue);
            }
        }

        private void spawnNextCenterEvent(MsqTime time, EventQueue queue) {
            EventType type = EventType.ARRIVAL_CHECK_IN_OTHERS;
            if(isTargetFlight(rngs, centerIndex + 2)){
                type = EventType.ARRIVAL_CHECK_IN_TARGET;
            }
            MsqEvent event = new MsqEvent(type, time.current);
            queue.add(event);
        }

        private void spawnCompletionEvent(MsqTime time, EventQueue queue) {
            double service = getService(centerIndex);
            MsqEvent event = new MsqEvent(EventType.LUGGAGE_CHECK_DONE, time.current + service, service, 0, centerID);
            queue.add(event);
        }

        private double getService(int streamIndex)
        {
            rngs.selectStream(streamIndex);
            return exponential(meanServiceTime, rngs);
        }

        public void saveStats() {
            batchIndex++;
            statistics.saveStats(area, sum, lastArrivalTime, lastCompletionTime);
        }
        public void writeStats(String simulationType){
            statistics.writeStats(simulationType);
        }

        public MeanStatistics getMeanStatistics() {
            return statistics.getMeanStatistics();
        }
    }
}
