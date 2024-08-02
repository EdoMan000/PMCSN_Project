package org.pmcsn.centers;

import org.pmcsn.model.*;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Distributions.logNormal;

public class BoardingTarget extends MultiServer {
    public BoardingTarget(String name, double meanServiceTime, int serversNum, int streamIndex, boolean approximateServiceAsExponential) {
        super(name, meanServiceTime, serversNum, streamIndex, approximateServiceAsExponential);
    }

    @Override
    void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId) {
        double service = getService(streamIndex);
        MsqEvent event = new MsqEvent(EventType.BOARDING_TARGET_DONE, time.current + service, service, serverId);
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
