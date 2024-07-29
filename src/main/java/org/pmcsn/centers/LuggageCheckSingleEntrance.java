package org.pmcsn.centers;

import org.pmcsn.model.EventQueue;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Probabilities.isTargetFlight;

class LuggageChecksSingleEntrance extends SingleServer {
    int centerID;

    public LuggageChecksSingleEntrance(String centerName, int nodeId, int centerIndex, double meanServiceTime, boolean approximateServiceAsExponential) {
        super("%s_%d".formatted(centerName, nodeId), meanServiceTime, centerIndex, approximateServiceAsExponential);
        this.centerID = nodeId;

    }

    @Override
    public void spawnNextCenterEvent(MsqTime time, EventQueue queue) {
        EventType type = EventType.ARRIVAL_CHECK_IN_OTHERS;
        if(isTargetFlight(rngs, CENTER_INDEX + 2)){
            type = EventType.ARRIVAL_CHECK_IN_TARGET;
        }
        MsqEvent event = new MsqEvent(type, time.current);
        queue.add(event);
    }

    @Override
    public void spawnCompletionEvent(MsqTime time, EventQueue queue) {
        double service = getService(CENTER_INDEX);
        MsqEvent event = new MsqEvent(EventType.LUGGAGE_CHECK_DONE, time.current + service, service, 0, centerID);
        queue.add(event);
    }

    protected double getService(int streamIndex)
    {
        rngs.selectStream(streamIndex);
        return exponential(meanServiceTime, rngs);
    }
}