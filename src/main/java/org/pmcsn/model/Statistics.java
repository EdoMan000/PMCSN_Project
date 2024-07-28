package org.pmcsn.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.pmcsn.utils.PrintUtils.formatList;
import static org.pmcsn.utils.StatisticsUtils.computeMean;
import static org.pmcsn.utils.StatisticsUtils.computeConfidenceInterval;

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

    }

    public MeanStatistics getMeanStatistics() {
        return new MeanStatistics(this);
    }

    public Statistics(String centerName) {
        this.centerName = centerName.toLowerCase();
    }


    public void saveStats(Area area, MsqSum[] sum, double lastArrivalTime, double lastCompletionTime, boolean isMultiserver) {
        long numberOfJobsServed = Arrays.stream(sum).mapToLong(s -> s.served).sum();
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

        if(isMultiserver) {
            double meanServiceTime = 0.0;
            int usedServers = 0;
            for (MsqSum s : sum) {
                if (s.served > 0) {
                    meanServiceTime += s.service / s.served;
                    usedServers++;
                }
            }
            // mean service time (E[s])
            meanServiceTime /= usedServers;
            meanServiceTimeList.add(meanServiceTime);
            // mean utilization (ρ)
            double utilization = (lambda * meanServiceTime)/sum.length;
            meanUtilizationList.add(utilization);
        } else {
            // mean utilization (ρ)
            double utilization = area.getServiceArea() / lastCompletionTime;
            meanUtilizationList.add(utilization);
            // mean service time (E[s])
            meanServiceTimeList.add(area.getServiceArea() / sum[0].served);
        }

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
            String name = simulationType.contains("BATCH")? "#Batch": "#Run";

            fileWriter.append(name).append(", E[Ts], E[Tq], E[s], E[Ns], E[Nq], ρ, λ").append(DELIMITER);

            for (run = 0; run < meanResponseTimeList.size(); run++) {

                fileWriter.append(String.valueOf(run + 1)).append(COMMA)
                        .append(String.valueOf(meanResponseTimeList.get(run))).append(COMMA)
                        .append(String.valueOf(meanQueueTimeList.get(run))).append(COMMA)
                        .append(String.valueOf(meanServiceTimeList.get(run))).append(COMMA)
                        .append(String.valueOf(meanSystemPopulationList.get(run))).append(COMMA)
                        .append(String.valueOf(meanQueueTimeList.get(run))).append(COMMA)
                        .append(String.valueOf(meanUtilizationList.get(run))).append(COMMA)
                        .append(String.valueOf(lambdaList.get(run))).append(DELIMITER);
            }


            // Compute mean values
            MeanStatistics meanStats = getMeanStatistics();

            // Write mean values row
            fileWriter.append("MEAN_VALUES").append(COMMA)
                    .append(String.valueOf(meanStats.meanResponseTime)).append(COMMA)
                    .append(String.valueOf(meanStats.meanQueueTime)).append(COMMA)
                    .append(String.valueOf(meanStats.meanServiceTime)).append(COMMA)
                    .append(String.valueOf(meanStats.meanSystemPopulation)).append(COMMA)
                    .append(String.valueOf(meanStats.meanQueuePopulation)).append(COMMA)
                    .append(String.valueOf(meanStats.meanUtilization)).append(COMMA)
                    .append(String.valueOf(meanStats.lambda)).append(DELIMITER);

            // Compute confidence interval
            double responseTimeCI = computeConfidenceInterval(meanResponseTimeList);
            double queueTimeCI = computeConfidenceInterval(meanQueueTimeList);
            double serviceTimeCI = computeConfidenceInterval(meanServiceTimeList);
            double systemPopulationCI = computeConfidenceInterval(meanSystemPopulationList);
            double queuePopulationCI = computeConfidenceInterval(meanQueuePopulationList);
            double utilizationCI = computeConfidenceInterval(meanUtilizationList);
            double lambdaCI = computeConfidenceInterval(lambdaList);

            String PREAMBLE = "± ";
            // Write confidence intervals row
            fileWriter.append("CONFIDENCE_INTERVALS").append(COMMA) //TODO capire se i valori risultanti sono accettabili o meno
                    .append(PREAMBLE).append(String.valueOf(responseTimeCI)).append(COMMA)
                    .append(PREAMBLE).append(String.valueOf(queueTimeCI)).append(COMMA)
                    .append(PREAMBLE).append(String.valueOf(serviceTimeCI)).append(COMMA)
                    .append(PREAMBLE).append(String.valueOf(systemPopulationCI)).append(COMMA)
                    .append(PREAMBLE).append(String.valueOf(queuePopulationCI)).append(COMMA)
                    .append(PREAMBLE).append(String.valueOf(utilizationCI)).append(COMMA)
                    .append(PREAMBLE).append(String.valueOf(lambdaCI)).append(DELIMITER);

            fileWriter.flush();
        } catch (IOException e) {
            //ignore
        }
    }

    public void printLists() {
        System.out.println("Mean Response Time List: " + formatList(meanResponseTimeList));
        System.out.println("Mean Service Time List: " + formatList(meanServiceTimeList));
        System.out.println("Mean Queue Time List: " + formatList(meanQueueTimeList));
        System.out.println("Lambda List: " + formatList(lambdaList));
        System.out.println("Mean System Population List: " + formatList(meanSystemPopulationList));
        System.out.println("Mean Utilization List: " + formatList(meanUtilizationList));
        System.out.println("Mean Queue Population List: " + formatList(meanQueuePopulationList));
    }
}
