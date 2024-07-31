package org.pmcsn.centers;

import org.pmcsn.model.EventQueue;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;

import java.util.Arrays;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Distributions.logNormal;

class CheckInDesksOtherSingleFlight extends MultiServer {
    private final int nodeId;

    public CheckInDesksOtherSingleFlight(String centerName, int nodeId, double meanServiceTime, int numOfServers, int centerIndex, boolean approximateServiceAsExponential) {
        super(centerName + nodeId, meanServiceTime, numOfServers, centerIndex, approximateServiceAsExponential);
        this.nodeId = nodeId;
    }

    public long getCompletions() {
        return Arrays.stream(sum).mapToLong(s -> s.served).sum();
    }

    @Override
    public void spawnNextCenterEvent(MsqTime time, EventQueue queue) {
        MsqEvent event = new MsqEvent(EventType.ARRIVAL_BOARDING_PASS_SCANNERS, time.current);
        queue.add(event);
    }

    public void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId) {
        double service = getService(CENTER_INDEX+1);
        MsqEvent event = new MsqEvent(EventType.CHECK_IN_OTHERS_DONE, time.current + service, service, serverId, nodeId);
        queue.add(event);
    }

    public double getService(int streamIndex) {
        rngs.selectStream(streamIndex);
        if(approximateServiceAsExponential){
            return exponential(meanServiceTime, rngs);
        }
        return (logNormal(meanServiceTime, meanServiceTime*0.2, rngs));
    }
}
