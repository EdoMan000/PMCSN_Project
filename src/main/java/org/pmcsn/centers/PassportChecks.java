package org.pmcsn.centers;


import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;

public class PassportChecks extends Multiserver {
    public PassportChecks(String name, double meanServiceTime, int numOfServers, int centerIndex) {
        super(name, meanServiceTime, numOfServers, centerIndex);
    }

    @Override
    public void spawnNextCenterEvent(MsqTime time, EventQueue queue) {
        MsqEvent next_center_event = new MsqEvent(EventType.ARRIVAL_STAMP_CHECK, time.current);
        queue.add(next_center_event);
    }

    @Override
    public void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId) {
        double service = getService(CENTER_INDEX+1);
        MsqEvent event = new MsqEvent(EventType.PASSPORT_CHECK_DONE, time.current + service, service, serverId);
        queue.add(event);
    }

    public double getService(int streamIndex)
    {
        rngs.selectStream(streamIndex);
        return (exponential(meanServiceTime,  rngs));
    }
}
