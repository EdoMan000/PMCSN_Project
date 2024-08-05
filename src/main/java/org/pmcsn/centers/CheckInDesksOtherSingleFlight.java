package org.pmcsn.centers;

import org.pmcsn.model.EventQueue;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;

import java.util.Arrays;

import static org.pmcsn.utils.Distributions.*;

class CheckInDesksOtherSingleFlight extends MultiServer {
    private final int nodeId;

    public CheckInDesksOtherSingleFlight(String centerName, int nodeId, double meanServiceTime, int numOfServers, int streamIndex, boolean approximateServiceAsExponential) {
        super("%s_%d".formatted(centerName, nodeId), meanServiceTime, numOfServers, streamIndex, approximateServiceAsExponential);
        this.nodeId = nodeId;
    }

    public long getCompletions() {
        return Arrays.stream(sum).mapToLong(s -> s.served).sum();
    }

    @Override
    public void spawnNextCenterEvent(MsqTime time, EventQueue queue) {
        double walkingTime = getWalkingTime(rngs);
        MsqEvent event = new MsqEvent(EventType.ARRIVAL_BOARDING_PASS_SCANNERS, time.current + walkingTime);
        queue.add(event);
    }

    public void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId) {
        double service = getService(streamIndex);
        MsqEvent event = new MsqEvent(EventType.CHECK_IN_OTHERS_DONE, time.current + service, service, serverId, nodeId);
        queue.add(event);
    }

    public double getService(int streamIndex) {
        rngs.selectStream(streamIndex);
        if(approximateServiceAsExponential){
            return exponential(meanServiceTime, rngs);
        }
        return logNormal(meanServiceTime, meanServiceTime*0.2, rngs);
    }
}
