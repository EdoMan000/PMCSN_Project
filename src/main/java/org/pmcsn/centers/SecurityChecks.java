package org.pmcsn.centers;

import org.pmcsn.model.EventQueue;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Distributions.logNormal;
import static org.pmcsn.utils.Probabilities.*;

public class SecurityChecks extends MultiServer {
    public SecurityChecks(String centerName, double meanServiceTime, int serversNumber, int centerIndex, boolean approximateServiceAsExponential) {
        super(centerName, meanServiceTime, serversNumber, centerIndex, approximateServiceAsExponential);
    }

    @Override
    public void spawnNextCenterEvent(MsqTime time, EventQueue queue) {
        if (isCitizen(rngs,CENTER_INDEX + 2)) {
            boolean priority = isPriority(rngs, CENTER_INDEX + 4);
            EventType type = isTargetFlight(rngs, CENTER_INDEX + 3) ? EventType.ARRIVAL_BOARDING_TARGET : EventType.ARRIVAL_BOARDING_OTHERS;
            queue.addPriority(new MsqEvent(type, time.current, priority));
        } else {
            MsqEvent event = new MsqEvent(EventType.ARRIVAL_PASSPORT_CHECK, time.current);
            queue.add(event);
        }
    }

    @Override
    public void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId) {
        double service = getService(CENTER_INDEX+1);
        //generate a new completion event
        MsqEvent event = new MsqEvent(EventType.SECURITY_CHECK_DONE, time.current + service, service, serverId);
        queue.add(event);
    }

    @Override
    double getService(int streamIndex) {
        rngs.selectStream(streamIndex);
        if(approximateServiceAsExponential){
            return exponential(meanServiceTime, rngs);
        }
        return (logNormal(meanServiceTime, meanServiceTime*0.2, rngs));
    }
}
