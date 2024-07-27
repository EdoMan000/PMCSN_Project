package org.pmcsn.centers;


import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;

public class PassportChecks extends Multiserver{
    public PassportChecks(String name, double meanServiceTime, int numOfServers, int centerIndex) {
        super(name, meanServiceTime, numOfServers, centerIndex);
    }


    public void spawnNextCenterEvent(MsqTime time, List<MsqEvent> events) {
        MsqEvent next_center_event = new MsqEvent(time.current, true, EventType.ARRIVAL_STAMP_CHECK, 0);
        events.add(next_center_event);
        events.sort(Comparator.comparing(MsqEvent::getTime));
    }

    public void spawnCompletionEvent(MsqTime time, List<MsqEvent> events, int serverId) {

        double service = getService(CENTER_INDEX+1);

        //generate a new completion event
        MsqEvent event = new MsqEvent(time.current + service, true, EventType.PASSPORT_CHECK_DONE, serverId);
        // TODO: inizializzare in costruttore
        event.service = service;
        events.add(event);
        events.sort(Comparator.comparing(MsqEvent::getTime));
    }

    public double getService(int streamIndex)
    {
        rngs.selectStream(streamIndex);
        // 5 min as mean time
        return (exponential(5,  rngs));
    }

}
