package org.pmcsn.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.pmcsn.MenaraAirportSimulator.RESET;
import static org.pmcsn.MenaraAirportSimulator.YELLOW;

public class Statistics {

    /*  STATISTICS OF INTEREST :
     *  * Response times
     *  * Service times
     *  * Queue times
     *  * Inter-arrival times
     *  * Population
     *  * Utilization
     *  * Queue population
     */

    public String centerName;
    public List<Double> meanResponseTimeList = new ArrayList<Double>();
    public List<Double> meanServiceTimeList = new ArrayList<Double>();
    public List<Double> meanQueueTimeList = new ArrayList<Double>();
    public List<Double> meanInterarrivalTimeList = new ArrayList<Double>();
    public List<Double> meanSystemPopulationList = new ArrayList<Double>();
    public List<Double> meanUtilizationList = new ArrayList<Double>();
    public List<Double> meanQueuePopulationList = new ArrayList<Double>();

    public Statistics(String centerName) {
        this.centerName = centerName.toLowerCase();
    }

    public void saveStats(int numberOfServers, long numberOfJobsServed, double area, MsqSum[] sum, double firstArrivalTime, double lastArrivalTime, double lastCompletionTime) {
        System.out.println(YELLOW + "\n\n***************************************");
        System.out.println("Saving stats for " + this.centerName.toUpperCase());
        System.out.println("***************************************" + RESET);
        // Print the parameters
        System.out.println("Number of Servers: " + numberOfServers);
        System.out.println("Number of Jobs Served: " + numberOfJobsServed);
        System.out.println("Area: " + area);
        System.out.println("Sum: " + Arrays.toString(sum));
        System.out.println("First Arrival Time: " + firstArrivalTime);
        System.out.println("Last Arrival Time: " + lastArrivalTime);
        System.out.println("Last Completion Time: " + lastCompletionTime);
        System.out.println(YELLOW + "***************************************" + RESET);

        double totalResponseTime = lastCompletionTime - firstArrivalTime;
        //double time = lastCompletionTime;
        double utilization = 0;
        double totalServiceTime = 0;

        for(int i = 0; i < numberOfServers; i++) {
            totalServiceTime += sum[i].service;
        }

        // inter-arrival
        meanInterarrivalTimeList.add((lastArrivalTime - firstArrivalTime) / numberOfJobsServed);

        // mean response time (E[Ts])
        double Ets = area / numberOfJobsServed;
        meanResponseTimeList.add(Ets);

        // mean population (E[Tn])
        double Ens = area / totalResponseTime;
        meanSystemPopulationList.add(Ens);

        // mean wait time (E[Tq])
        double totalQueueTime = totalResponseTime - totalServiceTime;
        double Etq = totalQueueTime / numberOfJobsServed;
        meanQueueTimeList.add(Etq);

        // mean queue population (E[Nq])
        double Enq = totalQueueTime / totalResponseTime;
        meanQueuePopulationList.add(Enq);

        for(int i = 0; i < numberOfServers; i++) {
            utilization += sum[i].service / totalResponseTime;
        }
        //utilization = totalServiceTime / (numberOfServers * totalResponseTime);

        // mean utilization (ρ)
        meanUtilizationList.add(utilization/numberOfServers);

        // mean service time (E[s])
        meanServiceTimeList.add(totalServiceTime / numberOfJobsServed);

    }

    public void writeStats(String simulationType) {
        File file = new File("csvFiles/results/" + simulationType +"/");
        if (!file.exists()) {
            file.mkdirs();
        }

        file = new File("csvFiles/results/" + simulationType +"/" + centerName+ ".csv");
        try(FileWriter fileWriter = new FileWriter(file)) {

            String DELIMITER = "\n";
            String COMMA = ",";
            int run;


            fileWriter.append("#Run, E[Ts], E[Tq], E[s], E[Ns], E[Nq], ρ, λ").append(DELIMITER);
            for (run = 0; run < 150; run++){

                fileWriter.append(String.valueOf(run)).append(COMMA)
                        .append(String.valueOf(meanResponseTimeList.get(run))).append(COMMA)
                        .append(String.valueOf(meanQueueTimeList.get(run))).append(COMMA)
                        .append(String.valueOf(meanServiceTimeList.get(run))).append(COMMA)
                        .append(String.valueOf(meanSystemPopulationList.get(run))).append(COMMA)
                        .append(String.valueOf(meanQueueTimeList.get(run))).append(COMMA)
                        .append(String.valueOf(meanUtilizationList.get(run))).append(COMMA)
                        .append(String.valueOf(meanInterarrivalTimeList.get(run))).append(DELIMITER);
            }

            fileWriter.flush();
        } catch (IOException e) {
            //ignore
        }
    }


}
