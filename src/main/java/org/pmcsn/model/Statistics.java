package org.pmcsn.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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


    public List<Double> meanResponseTimeListBatch = new ArrayList<Double>();
    public List<Double> meanServiceTimeListBatch = new ArrayList<Double>();
    public List<Double> meanQueueTimeListBatch = new ArrayList<Double>();
    public List<Double> lambdaListBatch = new ArrayList<Double>();
    public List<Double> meanSystemPopulationListBatch = new ArrayList<Double>();
    public List<Double> meanUtilizationListBatch = new ArrayList<Double>();
    public List<Double> meanQueuePopulationListBatch = new ArrayList<Double>();

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

    public void saveOneBatchStats(){

        // obtaining the mean of the current batch
        MeanStatistics ms =  getMeanStatistics();

        lambdaListBatch.add(ms.lambda);
        meanSystemPopulationListBatch.add(ms.meanSystemPopulation);
        meanResponseTimeListBatch.add(ms.meanResponseTime);
        meanQueuePopulationListBatch.add(ms.meanQueuePopulation);
        meanQueueTimeListBatch.add(ms.meanQueueTime);
        meanServiceTimeListBatch.add(ms.meanServiceTime);
        meanUtilizationListBatch.add(ms.meanUtilization);

        //clearing the lists so in the next batch they will be empty
        reset();

    }

    private void reset(){
        meanResponseTimeList.clear();
        meanServiceTimeList.clear();
        meanQueueTimeList.clear();
        lambdaList.clear();
        meanSystemPopulationList.clear();
        meanUtilizationList.clear();
        meanQueuePopulationList.clear();
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

            if(simulationType.contains("BATCH")) {
                fileWriter.append("#Batch, E[Ts], E[Tq], E[s], E[Ns], E[Nq], ρ, λ").append(DELIMITER);

                for (run = 0; run < meanResponseTimeListBatch.size(); run++) {
                    fileWriter.append(String.valueOf(run + 1)).append(COMMA)
                            .append(String.valueOf(meanResponseTimeListBatch.get(run))).append(COMMA)
                            .append(String.valueOf(meanQueueTimeListBatch.get(run))).append(COMMA)
                            .append(String.valueOf(meanServiceTimeListBatch.get(run))).append(COMMA)
                            .append(String.valueOf(meanSystemPopulationListBatch.get(run))).append(COMMA)
                            .append(String.valueOf(meanQueueTimeListBatch.get(run))).append(COMMA)
                            .append(String.valueOf(meanUtilizationListBatch.get(run))).append(COMMA)
                            .append(String.valueOf(lambdaListBatch.get(run))).append(DELIMITER);
                }


            } else {

                fileWriter.append("#Run, E[Ts], E[Tq], E[s], E[Ns], E[Nq], ρ, λ").append(DELIMITER);

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
}
