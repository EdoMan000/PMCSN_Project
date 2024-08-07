package org.pmcsn.centers;

import org.pmcsn.model.EventQueue;
import org.pmcsn.model.EventType;
import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqTime;

import static org.pmcsn.utils.Distributions.exponential;
import static org.pmcsn.utils.Distributions.uniform;
import static org.pmcsn.utils.Probabilities.isPriority;

public class StampsCheck extends MultiServer{

    public StampsCheck(String centerName, double meanServiceTime, int serverNumber, int streamIndex, boolean approximateServiceAsExponential) {
        super(centerName, meanServiceTime, serverNumber, streamIndex, approximateServiceAsExponential);
    }


    public void spawnNextCenterEvent(MsqTime time, EventQueue queue){
        boolean isPriority = isPriority(rngs, streamIndex + 1);
        queue.addPriority(new MsqEvent(EventType.ARRIVAL_BOARDING, time.current, isPriority));
    }

    @Override
    void spawnCompletionEvent(MsqTime time, EventQueue queue, int serverId) {
        double service = getService(streamIndex);
        MsqEvent event = new MsqEvent(EventType.STAMP_CHECK_DONE, time.current + service, service, serverId);
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
