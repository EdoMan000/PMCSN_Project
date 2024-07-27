package org.pmcsn.centers;

import org.pmcsn.model.*;

import static org.pmcsn.utils.Distributions.exponential;

public class Boarding extends Multiserver {
    public Boarding(String name, double meanServiceTime, int serversNum, int centerIndex) {
        super(name, meanServiceTime, serversNum, centerIndex);
    }

    @Override
    void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId) {
        double service = getService(CENTER_INDEX+1);
        MsqEvent event = new MsqEvent(EventType.BOARDING_DONE, time.current + service, service, serverId);
        queue.add(event);
    }

    @Override
    void spawnNextCenterEvent(MsqTime time, EventQueue events) {

    }

    @Override
    double getService(int streamIndex) {
        rngs.selectStream(streamIndex);
        return exponential(meanServiceTime,  rngs);
    }
}
