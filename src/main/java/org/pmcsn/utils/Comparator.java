package org.pmcsn.utils;

import org.pmcsn.controller.Verification.Result;
import org.pmcsn.model.Statistics.MeanStatistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Comparator {

    public static class ComparisonResult {
        public String name;
        public double responseTimeDiff;
        public double serviceTimeDiff;
        public double queueTimeDiff;
        public double lambdaDiff;
        public double systemPopulationDiff;
        public double utilizationDiff;
        public double queuePopulationDiff;

        public ComparisonResult(String name, double responseTimeDiff, double serviceTimeDiff, double queueTimeDiff, double lambdaDiff, double systemPopulationDiff, double utilizationDiff, double queuePopulationDiff) {
            this.name = name;
            this.responseTimeDiff = responseTimeDiff;
            this.serviceTimeDiff = serviceTimeDiff;
            this.queueTimeDiff = queueTimeDiff;
            this.lambdaDiff = lambdaDiff;
            this.systemPopulationDiff = systemPopulationDiff;
            this.utilizationDiff = utilizationDiff;
            this.queuePopulationDiff = queuePopulationDiff;
        }

        @Override
        public String toString() {
            return "ComparatorResult{" +
                    "name='" + name + '\'' +
                    ", responseTimeDiff=" + responseTimeDiff +
                    ", serviceTimeDiff=" + serviceTimeDiff +
                    ", queueTimeDiff=" + queueTimeDiff +
                    ", lambdaDiff=" + lambdaDiff +
                    ", systemPopulationDiff=" + systemPopulationDiff +
                    ", utilizationDiff=" + utilizationDiff +
                    ", queuePopulationDiff=" + queuePopulationDiff +
                    '}';
        }
    }

    public static List<ComparisonResult> compareResults(List<Result> verificationResults, List<MeanStatistics> meanStatisticsList) {
        List<ComparisonResult> comparisonResults = new ArrayList<>();

        for (Result result : verificationResults) {
            for (MeanStatistics meanStatistics : meanStatisticsList) {
                if (result.name.equals(meanStatistics.centerName)) {
                    double responseTimeDiff = Math.abs(result.Ets - meanStatistics.meanResponseTime);
                    double serviceTimeDiff = Math.abs(result.Es - meanStatistics.meanServiceTime);
                    double queueTimeDiff = Math.abs(result.Etq - meanStatistics.meanQueueTime);
                    double lambdaDiff = Math.abs(result.lambda - meanStatistics.lambda);
                    double systemPopulationDiff = Math.abs(result.Ens - meanStatistics.meanSystemPopulation);
                    double utilizationDiff = Math.abs(result.rho - meanStatistics.meanUtilization);
                    double queuePopulationDiff = Math.abs(result.Enq - meanStatistics.meanQueuePopulation);

                    ComparisonResult comparisonResult = new ComparisonResult(
                            result.name,
                            responseTimeDiff,
                            serviceTimeDiff,
                            queueTimeDiff,
                            lambdaDiff,
                            systemPopulationDiff,
                            utilizationDiff,
                            queuePopulationDiff
                    );
                    comparisonResults.add(comparisonResult);
                }
            }
        }
        writeResultsComparison("BASIC_MODEL", comparisonResults);
        return comparisonResults;
    }

    public static void writeResultsComparison(String modelName, List<ComparisonResult> comparisonResults) {
        File file = new File("csvFiles/" + modelName + "/comparison/");
        if (!file.exists()) {
            file.mkdirs();
        }

        file = new File("csvFiles/" + modelName + "/comparison/comparison.csv");
        try (FileWriter fileWriter = new FileWriter(file)) {
            String DELIMITER = "\n";
            String COMMA = ",";

            fileWriter.append("Center, ResponseTimeDiff, ServiceTimeDiff, QueueTimeDiff, LambdaDiff, SystemPopulationDiff, UtilizationDiff, QueuePopulationDiff").append(DELIMITER);
            for (ComparisonResult result : comparisonResults) {
                fileWriter.append(result.name).append(COMMA)
                        .append(String.valueOf(result.responseTimeDiff)).append(COMMA)
                        .append(String.valueOf(result.serviceTimeDiff)).append(COMMA)
                        .append(String.valueOf(result.queueTimeDiff)).append(COMMA)
                        .append(String.valueOf(result.lambdaDiff)).append(COMMA)
                        .append(String.valueOf(result.systemPopulationDiff)).append(COMMA)
                        .append(String.valueOf(result.utilizationDiff)).append(COMMA)
                        .append(String.valueOf(result.queuePopulationDiff)).append(DELIMITER);
            }

            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
