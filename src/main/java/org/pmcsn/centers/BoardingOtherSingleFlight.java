package org.pmcsn.centers;

import org.pmcsn.model.EventQueue;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Distributions.logNormal;

public class BoardingOtherSingleFlight extends MultiServer {
    private final int nodeId;

    public BoardingOtherSingleFlight(String centerName, int nodeId, double meanServiceTime, int serversNum, int centerIndex, boolean approximateServiceAsExponential) {
        super(centerName + nodeId, meanServiceTime, serversNum, centerIndex, approximateServiceAsExponential);
        this.nodeId = nodeId;
    }

    @Override
    void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId) {
        double service = getService(CENTER_INDEX+1);
        MsqEvent event = new MsqEvent(EventType.BOARDING_OTHERS_DONE, time.current + service, service, serverId, nodeId);
        queue.add(event);
    }

    @Override
    void spawnNextCenterEvent(MsqTime time, EventQueue events) {
    }

    @Override
    double getService(int streamIndex) {
        rngs.selectStream(streamIndex);
        if(approximateServiceAsExponential){
            return exponential(meanServiceTime, rngs);
        }
        return logNormal(meanServiceTime, meanServiceTime*0.2, rngs);
    }
}
