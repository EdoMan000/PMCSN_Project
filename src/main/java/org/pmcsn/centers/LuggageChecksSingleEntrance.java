package org.pmcsn.centers;

import org.pmcsn.model.EventQueue;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;

import java.util.logging.*;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Probabilities.isTargetFlight;

public class LuggageChecksSingleEntrance extends SingleServer {
    int centerID;

    public LuggageChecksSingleEntrance(String centerName, int nodeId, int streamIndex, double meanServiceTime, boolean approximateServiceAsExponential) {
        super("%s_%d".formatted(centerName, nodeId), meanServiceTime, streamIndex, approximateServiceAsExponential);
        this.centerID = nodeId;
    }

    @Override
    public void spawnNextCenterEvent(MsqTime time, EventQueue queue) {
        EventType type = EventType.ARRIVAL_CHECK_IN_OTHERS;
        if(isTargetFlight(rngs, streamindex + 1)){
            type = EventType.ARRIVAL_CHECK_IN_TARGET;
        }
        MsqEvent event = new MsqEvent(type, time.current);
        queue.add(event);
    }

    @Override
    public void spawnCompletionEvent(MsqTime time, EventQueue queue) {
        double service = getService(streamindex);
        MsqEvent event = new MsqEvent(EventType.LUGGAGE_CHECK_DONE, time.current + service, service, 0, centerID);
        queue.add(event);
    }

    protected double getService(int streamIndex)
    {
        rngs.selectStream(streamIndex);
        return exponential(meanServiceTime, rngs);
    }
}