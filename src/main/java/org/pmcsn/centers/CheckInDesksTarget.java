package org.pmcsn.centers;

import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;

public class CheckInDesksTarget extends Multiserver {
    public CheckInDesksTarget(String name, double meanServiceTime, int numOfServers, int centerIndex) {
        super(name, meanServiceTime, numOfServers, centerIndex);
    }

    @Override
    public void spawnNextCenterEvent(MsqTime time, EventQueue queue) {
        MsqEvent event = new MsqEvent(EventType.ARRIVAL_BOARDING_PASS_SCANNERS, time.current);
        queue.add(event);
    }

    @Override
    public void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId) {
        double service = getService(CENTER_INDEX + 1);
        MsqEvent event = new MsqEvent(EventType.CHECK_IN_TARGET_DONE, time.current + service, service, serverId);
        queue.add(event);
    }


    @Override
    public double getService(int streamIndex)
    {
        rngs.selectStream(streamIndex);
        return exponential(meanServiceTime, rngs);
    }
}
