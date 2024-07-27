package org.pmcsn.centers;

import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Probabilities.*;

public class SecurityChecks extends Multiserver {
    public SecurityChecks(String name, double meanServiceTime, int serversNumber, int centerIndex) {
        super(name, meanServiceTime, serversNumber, centerIndex);
    }

    @Override
    double getService(int streamIndex) {
        rngs.selectStream(streamIndex);
        return exponential(meanServiceTime, rngs);
    }

    @Override
    public void spawnNextCenterEvent(MsqTime time, EventQueue queue) {
        if (isCitizen(rngs,CENTER_INDEX + 2)) {
            if (isTargetFlight(rngs, CENTER_INDEX + 3)) {
                boolean priority = isPriority(rngs, CENTER_INDEX + 4);
                MsqEvent event = new MsqEvent(EventType.ARRIVAL_BOARDING, time.current, priority);
                queue.addPriority(event);
            }
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
}
