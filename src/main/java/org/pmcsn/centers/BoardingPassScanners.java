package org.pmcsn.centers;

import org.pmcsn.model.*;

import static org.pmcsn.utils.Distributions.exponential;

public class BoardingPassScanners extends MultiServer {
    public BoardingPassScanners(String centerName, double meanServiceTime, int serversNumber, int centerIndex, boolean approximateServiceAsExponential) {
        super(centerName, meanServiceTime, serversNumber, centerIndex, approximateServiceAsExponential);
    }

    @Override
    void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId) {
        double service = getService(streamIndex);
        MsqEvent event = new MsqEvent(EventType.BOARDING_PASS_SCANNERS_DONE, time.current + service, service, serverId);
        queue.add(event);
    }

    @Override
    public void spawnNextCenterEvent(MsqTime time, EventQueue queue) {
        MsqEvent event = new MsqEvent(EventType.ARRIVAL_SECURITY_CHECK, time.current);
        queue.add(event);
    }

    @Override
    double getService(int streamIndex) {
        rngs.selectStream(streamIndex);
        return exponential(meanServiceTime, rngs);
    }
}
