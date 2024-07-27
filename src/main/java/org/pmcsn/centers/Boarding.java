package org.pmcsn.centers;

import org.pmcsn.libraries.Rngs;
import org.pmcsn.model.*;

import java.util.Comparator;
import java.util.List;

import static org.pmcsn.utils.Distributions.exponential;

public class Boarding extends Multiserver { //TODO gestione priorità per calcolo delle statistiche
    public Boarding(String name, double meanServiceTime, int serversNum, int centerIndex) {
        super(name, meanServiceTime, serversNum, centerIndex);
    }

    @Override
    void spawnCompletionEvent(MsqTime time, List<MsqEvent> events, int serverId) {
        double service = getService(CENTER_INDEX+1);

        //generate a new completion event
        MsqEvent event = new MsqEvent(time.current + service, true, EventType.BOARDING_DONE, serverId);
        // TODO: inizializzare in costruttore
        event.service = service;
        events.add(event);
        events.sort(Comparator.comparing(MsqEvent::getTime));
    }

    @Override
    void spawnNextCenterEvent(MsqTime time, List<MsqEvent> events) {

    }

    @Override
    double getService(int streamIndex) {
        rngs.selectStream(streamIndex);
        return (exponential(meanServiceTime,  rngs));
    }
}
