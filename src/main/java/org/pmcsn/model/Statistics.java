package org.pmcsn.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
    public List<Double> lambdaList = new ArrayList<Double>();
    public List<Double> meanSystemPopulationList = new ArrayList<Double>();
    public List<Double> meanUtilizationList = new ArrayList<Double>();
    public List<Double> meanQueuePopulationList = new ArrayList<Double>();

    public static class MeanStatistics {
        public String centerName;
        public double meanResponseTime;
        public double meanServiceTime;
        public double meanQueueTime;
        public double lambda;
        public double meanSystemPopulation;
        public double meanUtilization;
        public double meanQueuePopulation;

        public MeanStatistics(Statistics stats) {
            this.centerName = stats.centerName;
            this.meanResponseTime = computeMean(stats.meanResponseTimeList);
            this.meanServiceTime = computeMean(stats.meanServiceTimeList);
            this.meanQueueTime = computeMean(stats.meanQueueTimeList);
            this.lambda = computeMean(stats.lambdaList);
            this.meanSystemPopulation = computeMean(stats.meanSystemPopulationList);
            this.meanUtilization = computeMean(stats.meanUtilizationList);
            this.meanQueuePopulation = computeMean(stats.meanQueuePopulationList);
        }

        public MeanStatistics(String centerName, double  meanResponseTime, double meanServiceTime, double meanQueueTime
        , double lambda, double meanSystemPopulation, double meanUtilization, double meanQueuePopulation) {
            this.centerName = centerName;
            this.meanResponseTime = meanResponseTime;
            this.meanServiceTime = meanServiceTime;
            this.meanQueueTime = meanQueueTime;
            this.lambda = lambda;
            this.meanSystemPopulation = meanSystemPopulation;
            this.meanUtilization = meanUtilization;
            this.meanQueuePopulation = meanQueuePopulation;
        }


        public void setCenterName(String centerName) {
            this.centerName = centerName;
        }
    }

    public MeanStatistics getMeanStatistics() {
        return new MeanStatistics(this);
    }

    public static double computeMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    public Statistics(String centerName) {
        this.centerName = centerName.toLowerCase();
    }

    public static final String RESET = "\033[0m";
    public static final String YELLOW = "\033[0;33m";

    private void printStats(int numberOfServers, long numberOfJobsServed, double area, MsqSum[] sum, double firstArrivalTime, double lastArrivalTime, double lastCompletionTime) {
        System.out.println(YELLOW + "\n\n*************************************************");
        System.out.println("Saving stats for " + this.centerName.toUpperCase());
        System.out.println("*************************************************" + RESET);
        // Print the parameters
        System.out.println("Number of Servers: " + numberOfServers);
        System.out.println("Number of Jobs Served: " + numberOfJobsServed);
        System.out.println("Area: " + area);
        System.out.println("First Arrival Time: " + firstArrivalTime);
        System.out.println("Last Arrival Time: " + lastArrivalTime);
        System.out.println("Last Completion Time: " + lastCompletionTime);
        System.out.println("Sum:");
        for (MsqSum s : sum) {
            System.out.println(s);
        }
        System.out.println(YELLOW + "*************************************************" + RESET);
    }

    public void saveStats(Area area, MsqSum[] sum, double lastArrivalTime, double lastCompletionTime) {
        long numberOfJobsServed = 0;
        for (MsqSum msqSum : sum) {
            numberOfJobsServed += msqSum.served;
        }

        // inter-arrival
        double lambda = numberOfJobsServed / lastArrivalTime;
        lambdaList.add(lambda);
        // mean system population (E[Ns])
        double meanNodePopulation = area.getNodeArea() / lastCompletionTime;
        meanSystemPopulationList.add(meanNodePopulation);
        // mean response time (E[Ts])
        double meanResponseTime = meanNodePopulation / lambda;
        meanResponseTimeList.add(meanResponseTime);
        // mean queue population (E[Nq])
        double meanQueuePopulation = area.getQueueArea() / lastCompletionTime;
        meanQueuePopulationList.add(meanQueuePopulation);
        // mean wait time (E[Tq])
        double meanQueueTime = meanQueuePopulation / lambda;
        meanQueueTimeList.add(meanQueueTime);
        // mean utilization (ρ)
        double utilization = area.getServiceArea() / lastCompletionTime;
        meanUtilizationList.add(utilization);
        // mean service time (E[s])
        meanServiceTimeList.add(area.getServiceArea() / numberOfJobsServed);
    }


    public void writeStats(String simulationType) {
        File file = new File("csvFiles/"+simulationType+"/results/" );
        if (!file.exists()) {
            file.mkdirs();
        }

        file = new File("csvFiles/"+simulationType+"/results/" + centerName+ ".csv");
        try(FileWriter fileWriter = new FileWriter(file)) {

            String DELIMITER = "\n";
            String COMMA = ",";
            int run;


            fileWriter.append("#Run, E[Ts], E[Tq], E[s], E[Ns], E[Nq], ρ, λ").append(DELIMITER);
            for (run = 0; run < meanResponseTimeList.size(); run++){

                fileWriter.append(String.valueOf(run+1)).append(COMMA)
                        .append(String.valueOf(meanResponseTimeList.get(run))).append(COMMA)
                        .append(String.valueOf(meanQueueTimeList.get(run))).append(COMMA)
                        .append(String.valueOf(meanServiceTimeList.get(run))).append(COMMA)
                        .append(String.valueOf(meanSystemPopulationList.get(run))).append(COMMA)
                        .append(String.valueOf(meanQueueTimeList.get(run))).append(COMMA)
                        .append(String.valueOf(meanUtilizationList.get(run))).append(COMMA)
                        .append(String.valueOf(lambdaList.get(run))).append(DELIMITER);
            }

            fileWriter.flush();
        } catch (IOException e) {
            //ignore
        }
    }
}
