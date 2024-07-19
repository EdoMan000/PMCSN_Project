package org.pmcsn.model;

import java.util.ArrayList;
import java.util.List;

public class Statistics {

    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Service times
     *  * Queue times
     *  * Interarrival times
     *  * Population
     *  * Utilizations
     *  * Queue population
     */

    public double meanResponseTime;
    public double meanServiceTime;
    public double meanQueueTime;
    public double meanInterarrivalTime;
    public int meanSystemPopulation;
    public double meanUtilization;
    public int meanQueuePopulation;

    public static void printStats(String centerName, int numberOfServers, long numberOfJobsServed, double area, MsqSum[] sum, MsqTime time, List<MsqEvent> centerSpecificEvents, int replicationIndex) {
        double utilizzazione=0;
        System.out.println(centerName + " STATISTICS \n\n");
        System.out.println("for " + numberOfJobsServed + " jobs the service node statistics are:\n\n");
        System.out.println("  avg interarrivals .. = " + centerSpecificEvents.getFirst().getTime() / numberOfJobsServed);
        double Ets = area / numberOfJobsServed;
        System.out.println("  avg wait ........... = " + Ets);
        double Ens = area / time.current;
        System.out.println("  avg # in node ...... = " + Ens);

        for(int i = 1; i <= numberOfServers; i++) {
            area -= sum[i].service;
        }
        double Etq = area / numberOfJobsServed;
        System.out.println("  avg delay .......... = " + Etq );
        double Enq = area / time.current;
        System.out.println("  avg # in queue ..... = " + Enq);
        System.out.println("\nthe server statistics are:\n\n");
        System.out.println("    server     utilization     avg service        share\n");
        for(int i = 1; i <= numberOfServers; i++) {
            System.out.println(i + "\t" + sum[i].service / time.current + "\t" + sum[i].service / sum[i].served + "\t" + ((double)sum[i].served / numberOfJobsServed));
            utilizzazione+=sum[i].service / (numberOfServers*time.current);
            System.out.println("\n");

        }
        //TODO ???????????????????????????????????????????????????????????????????????????????
//        DataExtractor.writeReplicationStat(replicationINSERT_CENTER_NAME_HERE,Ets, Ens, Etq, Enq);
//        replicationStatisticsINSERT_CENTER_NAME_HERE.setBatchTempoCoda(Etq, replicationIndex);
//        replicationStatisticsINSERT_CENTER_NAME_HERE.setBatchPopolazioneSistema(Ens, replicationIndex);
//        replicationStatisticsINSERT_CENTER_NAME_HERE.setBatchTempoSistema(Ets, replicationIndex);
//        replicationStatisticsINSERT_CENTER_NAME_HERE.setBatchPopolazioneCodaArray(Enq, replicationIndex);
//        replicationStatisticsINSERT_CENTER_NAME_HERE.setBatchUtilizzazione(utilizzazione, replicationIndex);
        System.out.println("\n");
    }

}
