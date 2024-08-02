package org.pmcsn.centers;

import org.pmcsn.model.EventQueue;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Distributions.uniform;
import static org.pmcsn.utils.Probabilities.isPriority;
import static org.pmcsn.utils.Probabilities.isTargetFlight;

public class StampsCheck extends SingleServer{

    public StampsCheck(String centerName, double meanServiceTime, int streamIndex, boolean approximateServiceAsExponential) {
        super(centerName, meanServiceTime, streamIndex, approximateServiceAsExponential);
    }


    public void spawnNextCenterEvent(MsqTime time, EventQueue queue){
        boolean isPriority = isPriority(rngs, CENTER_INDEX + 3);
        EventType type = isTargetFlight(rngs, CENTER_INDEX+2) ? EventType.ARRIVAL_BOARDING_TARGET : EventType.ARRIVAL_BOARDING_OTHERS;
        queue.addPriority(new MsqEvent(type, time.current, isPriority));
    }

    public void spawnCompletionEvent(MsqTime time, EventQueue queue) {
        double service = getService(CENTER_INDEX);
        MsqEvent event = new MsqEvent(EventType.STAMP_CHECK_DONE, time.current + service, service);
        queue.add(event);
    }

    public double getService(int streamIndex)
    {
        rngs.selectStream(streamIndex);
        if (approximateServiceAsExponential) {
            return exponential(meanServiceTime, rngs);
        }
        return uniform(meanServiceTime-0.5 , meanServiceTime+0.5, rngs);
    }

}
