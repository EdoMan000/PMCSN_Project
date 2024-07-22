package org.pmcsn.model;

public class MsqSum {                      /* accumulated sums of                */
    public double service = 0;                   /*   service times                    */
    public long served = 0;                    /*   number served                    */

    @Override
    public String toString() {
        return "MsqSum{service=" + service + ", served=" + served + "}";
    }
}
