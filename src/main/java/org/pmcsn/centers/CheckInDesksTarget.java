package org.pmcsn.centers;

import org.pmcsn.model.EventQueue;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Distributions.logNormal;

public class CheckInDesksTarget extends Multiserver {
    public CheckInDesksTarget(String name, double meanServiceTime, int numOfServers, int centerIndex, boolean approximateServiceAsExponential) {
        super(name, meanServiceTime, numOfServers, centerIndex, approximateServiceAsExponential);
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
        if(approximateServiceAsExponential){
            return exponential(meanServiceTime, rngs);
        }
        return (logNormal(meanServiceTime, meanServiceTime*0.2, rngs));
    }
}
