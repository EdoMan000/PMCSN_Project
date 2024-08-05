package org.pmcsn.centers;


import org.pmcsn.model.*;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Distributions.getWalkingTime;

public class PassportChecks extends MultiServer {
    public PassportChecks(String centerName, double meanServiceTime, int serversNumber, int streamIndex, boolean approximateServiceAsExponential) {
        super(centerName, meanServiceTime, serversNumber, streamIndex, approximateServiceAsExponential);
    }

    @Override
    public void spawnNextCenterEvent(MsqTime time, EventQueue queue) {
        MsqEvent next_center_event = new MsqEvent(EventType.ARRIVAL_STAMP_CHECK, time.current);
        queue.add(next_center_event);
    }

    @Override
    public void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId) {
        double service = getService(streamIndex);
        MsqEvent event = new MsqEvent(EventType.PASSPORT_CHECK_DONE, time.current + service, service, serverId);
        queue.add(event);
    }

    public double getService(int streamIndex)
    {
        rngs.selectStream(streamIndex);
        return exponential(meanServiceTime,  rngs);
    }
}
