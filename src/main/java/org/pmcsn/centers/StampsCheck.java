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

    // index of center to select stream
    private static final int CENTER_INDEX = 59;

    private final Statistics statistics = new Statistics("STAMP_CHECK");
    private Area area;
    private final double meanServiceTime;
    // node population
    private long numberOfJobsInNode = 0;
    // number of completions
    private long numberOfJobsServed = 0;
    private double firstArrivalTime = Double.NEGATIVE_INFINITY;
    private double lastArrivalTime = 0;
    private double lastCompletionTime = 0;
    public int batchIndex = 0;

    private Rngs rngs;

    private final MsqSum sum = new MsqSum();

    public StampsCheck(double meanServiceTime) {
        this.meanServiceTime = meanServiceTime;
    }

    public void reset(Rngs rngs) {
        this.rngs = rngs;
        this.area = new Area();
        // resetting variables
        this.numberOfJobsInNode = 0;
        this.numberOfJobsServed = 0;
        this.firstArrivalTime = Double.NEGATIVE_INFINITY;
        this.lastArrivalTime = 0;
        this.lastCompletionTime = 0;
        sum.served = 0;
        sum.service = 0;
    }

    public void resetBatch() {
        // resetting variables
        this.numberOfJobsServed = 0;
        this.firstArrivalTime = Double.NEGATIVE_INFINITY;
        this.lastArrivalTime = 0;
        this.lastCompletionTime = 0;
        sum.reset();
        area.reset();
    }

    public double getBusyTime() {
        return sum.service;
    }

    public long getCompletions(){
        return sum.served;
    }

    public long getNumberOfJobsInNode() {
        return numberOfJobsInNode;
    }

    public void setArea(MsqTime time) {
        // TODO: il controllo per l'aggiornamento di area dovrebbe avvenire nel loop principale
        if (numberOfJobsInNode > 0) {
            double width = time.next - time.current;
            area.incNodeArea(width * numberOfJobsInNode);
            area.incQueueArea(width * (numberOfJobsInNode - 1));
            area.incServiceArea(width);
        }
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
            spawnCompletionEvent(time, events);
        }
    }

    public void processCompletion(MsqEvent completion, MsqTime time, List<MsqEvent> events) {
        numberOfJobsInNode--;
        sum.served++;
        sum.service += completion.service;
        lastCompletionTime = completion.time;
        events.remove(completion);
        if(isTargetFlight(rngs, CENTER_INDEX+2)) {
            MsqEvent event = new MsqEvent(time.current, true, EventType.ARRIVAL_BOARDING, 0, isPriority(rngs, CENTER_INDEX + 3));
            events.add(event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        }
        //checking if there are jobs in queue, if so the server starts processing one
        if (numberOfJobsInNode > 0) {
            spawnCompletionEvent(time, events);
        }
    }

    private void spawnCompletionEvent(MsqTime time, List<MsqEvent> events) {
        double service = getService(CENTER_INDEX);
        MsqEvent event = new MsqEvent(time.current + service, true, EventType.STAMP_CHECK_DONE);
        // TODO: inizializzare in costruttore
        event.service = service;
        events.add(event);
        events.sort(Comparator.comparing(MsqEvent::getTime));
    }

    public double getService(int streamIndex)
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

    public Statistics.MeanStatistics getMeanStatistics() {
        return statistics.getMeanStatistics();
    }
}
