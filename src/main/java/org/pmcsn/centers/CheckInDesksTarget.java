package org.pmcsn.centers;

import org.pmcsn.model.EventQueue;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;

import static org.pmcsn.utils.Distributions.*;

public class CheckInDesksTarget extends MultiServer {
    public CheckInDesksTarget(String centerName, double meanServiceTime, int numOfServers, int streamIndex, boolean approximateServiceAsExponential) {
        super(centerName, meanServiceTime, numOfServers, streamIndex, approximateServiceAsExponential);
    }

    @Override
    public void spawnNextCenterEvent(MsqTime time, EventQueue queue) {
        /*
        MsqEvent event = new MsqEvent(EventType.ARRIVAL_BOARDING_PASS_SCANNERS, time.current);
        queue.add(event);

         */
    }

    @Override
    public void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId) {
        double service = getService(streamIndex);
        MsqEvent event = new MsqEvent(EventType.CHECK_IN_TARGET_DONE, time.current + service, service, serverId);
        queue.add(event);
    }


    @Override
    public double getService(int streamIndex)
    {
        rngs.selectStream(streamIndex);
        if (approximateServiceAsExponential) {
            return exponential(meanServiceTime, rngs);
        }
        return logNormal(meanServiceTime, meanServiceTime*0.2, rngs);
    }
}
