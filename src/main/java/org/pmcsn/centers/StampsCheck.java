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

    public StampsCheck(String centerName, double meanServiceTime, int centerIndex, boolean approximateServiceAsExponential) {
        super(centerName, meanServiceTime, centerIndex, approximateServiceAsExponential);
    }


    public void spawnNextCenterEvent(MsqTime time, EventQueue queue){
        if(isTargetFlight(rngs, CENTER_INDEX+2)) {
            MsqEvent event = new MsqEvent(EventType.ARRIVAL_BOARDING, time.current, isPriority(rngs, CENTER_INDEX + 3));
            queue.addPriority(event);
        }
    }

    public void spawnCompletionEvent(MsqTime time, EventQueue queue) {
        double service = getService(CENTER_INDEX);
        MsqEvent event = new MsqEvent(EventType.STAMP_CHECK_DONE, time.current + service, service);
        queue.add(event);
    }

    public double getService(int streamIndex)
    {
        rngs.selectStream(streamIndex);
        if(approximateServiceAsExponential){
            return exponential(meanServiceTime, rngs);
        }
        return (uniform(meanServiceTime-0.5 , meanServiceTime+0.5, rngs));
    }

}
