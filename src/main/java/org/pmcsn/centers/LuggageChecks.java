package org.pmcsn.centers;

import org.pmcsn.model.MsqEvent;
import org.pmcsn.model.MsqSum;

public class LuggageChecks {

    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Service times
     *  * Queue times
     *  * Interarrival times
     *  * Population
     *  * Utilizations
     *  * Queue population
     */

    //Constants and Variables
    public static long  arrivalsCounter = 0;        /*number of arrivals*/
    long numberOfJobsInNode =0;                     /*number in the node*/

    public void processArrival(MsqEvent arrival){

    }

    public void processCompletion(MsqEvent completion){


    }

}
