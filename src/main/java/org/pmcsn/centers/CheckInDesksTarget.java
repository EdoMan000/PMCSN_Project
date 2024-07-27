package org.pmcsn.centers;

import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;

public class CheckInDesksTarget extends Multiserver{

    public CheckInDesksTarget(String name, double meanServiceTime, int numOfServers, int centerIndex) {
        super(name, meanServiceTime, numOfServers, centerIndex);
    }

    public void spawnNextCenterEvent(MsqTime time, List<MsqEvent> events) {
        MsqEvent event = new MsqEvent(time.current, true, EventType.ARRIVAL_BOARDING_PASS_SCANNERS, 0);
        events.add(event);
        events.sort(Comparator.comparing(MsqEvent::getTime));
    }

    public void spawnCompletionEvent(MsqTime time, List<MsqEvent> events, int serverId) {

        double service = getService(CENTER_INDEX+1);

        //generate a new completion event
        MsqEvent event = new MsqEvent(time.current + service, true, EventType.CHECK_IN_TARGET_DONE, serverId);
        // TODO: inizializzare in costruttore
        event.service = service;
        events.add(event);
        events.sort(Comparator.comparing(MsqEvent::getTime));
    }


    public double getService(int streamIndex)
        /* --------------------------------------------
         * generate the next service time with rate 2/3
         * --------------------------------------------
         */
    {
        rngs.selectStream(streamIndex);
        // mean time 10 min
        // std dev 2 min (20% since it has low variability)
        //return (logNormal(10, 2, rngs));
        return exponential(10, rngs);
    }

}
