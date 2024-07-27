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
    public void spawnNextCenterEvent(MsqTime time, List<MsqEvent> events) {
        if(isCitizen(rngs,CENTER_INDEX+2)){
            if(isTargetFlight(rngs, CENTER_INDEX+3)){
                if(isPriority(rngs, CENTER_INDEX+4)){
                    /* generate an arrival at boarding with priority*/
                    MsqEvent event = new MsqEvent(time.current, true, EventType.ARRIVAL_BOARDING, 0, true);
                    events.add(event);
                    events.sort(Comparator.comparing(MsqEvent::getTime));
                } else {
                    /* generate an arrival at boarding without priority*/
                    MsqEvent event = new MsqEvent(time.current, true, EventType.ARRIVAL_BOARDING, 0);
                    events.add(event);
                    events.sort(Comparator.comparing(MsqEvent::getTime));
                }
            }
        }else{
            /* generate an arrival at passport check*/
            MsqEvent event = new MsqEvent(time.current, true, EventType.ARRIVAL_PASSPORT_CHECK, 0);
            events.add(event);
            events.sort(Comparator.comparing(MsqEvent::getTime));
        }
    }

    @Override
    public void spawnCompletionEvent(MsqTime time, List<MsqEvent> events, int serverId) {
        double service = getService(CENTER_INDEX+1);
        //generate a new completion event
        MsqEvent event = new MsqEvent(time.current + service, true, EventType.SECURITY_CHECK_DONE, serverId);
        // TODO: inizializzare in costruttore
        event.service = service;
        events.add(event);
        events.sort(Comparator.comparing(MsqEvent::getTime));
    }
}
