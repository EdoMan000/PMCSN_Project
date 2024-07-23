package org.pmcsn.utils;

import org.pmcsn.controller.Verification.Result;
import org.pmcsn.model.Statistics.MeanStatistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Comparison {

    public static class ComparisonResult {
        public String name;
        public double responseTimeDiff;
        public double queueTimeDiff;
        public double serviceTimeDiff;
        public double systemPopulationDiff;
        public double queuePopulationDiff;
        public double utilizationDiff;
        public double lambdaDiff;

        public ComparisonResult(String name, double responseTimeDiff, double queueTimeDiff, double serviceTimeDiff, double systemPopulationDiff, double queuePopulationDiff, double utilizationDiff, double lambdaDiff) {
            this.name = name;
            this.responseTimeDiff = responseTimeDiff;
            this.queueTimeDiff = queueTimeDiff;
            this.serviceTimeDiff = serviceTimeDiff;
            this.systemPopulationDiff = systemPopulationDiff;
            this.queuePopulationDiff = queuePopulationDiff;
            this.utilizationDiff = utilizationDiff;
            this.lambdaDiff = lambdaDiff;
        }
    }

    public static List<ComparisonResult> compareResults(String simulationType, List<Result> verificationResults, List<MeanStatistics> meanStatisticsList) {
        List<ComparisonResult> comparisonResults = new ArrayList<>();

        for (Result result : verificationResults) {
            for (MeanStatistics meanStatistics : meanStatisticsList) {
                if (result.name.equals(meanStatistics.centerName.toUpperCase())) {
                    double responseTimeDiff = Math.abs(result.Ets - meanStatistics.meanResponseTime);
                    double queueTimeDiff = Math.abs(result.Etq - meanStatistics.meanQueueTime);
                    double serviceTimeDiff = Math.abs(result.Es - meanStatistics.meanServiceTime);
                    double systemPopulationDiff = Math.abs(result.Ens - meanStatistics.meanSystemPopulation);
                    double queuePopulationDiff = Math.abs(result.Enq - meanStatistics.meanQueuePopulation);
                    double utilizationDiff = Math.abs(result.rho - meanStatistics.meanUtilization);
                    double lambdaDiff = Math.abs(result.lambda - meanStatistics.lambda);

                    ComparisonResult comparisonResult = new ComparisonResult(
                            result.name,
                            responseTimeDiff,
                            queueTimeDiff,
                            serviceTimeDiff,
                            systemPopulationDiff,
                            queuePopulationDiff,
                            utilizationDiff,
                            lambdaDiff
                    );
                    comparisonResults.add(comparisonResult);
                    printComparisonResult(comparisonResult);
                }
            }
        }
        writeResultsComparison(simulationType, comparisonResults);
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

            fileWriter.append("Center, E[Ts]_Diff, E[Tq]_Diff, E[s]_Diff, E[Ns]_Diff, E[Nq]_Diff, ρ_Diff, λ_Diff").append(DELIMITER);
            for (ComparisonResult result : comparisonResults) {
                fileWriter.append(result.name).append(COMMA)
                        .append(String.valueOf(result.responseTimeDiff)).append(COMMA)
                        .append(String.valueOf(result.queueTimeDiff)).append(COMMA)
                        .append(String.valueOf(result.serviceTimeDiff)).append(COMMA)
                        .append(String.valueOf(result.systemPopulationDiff)).append(COMMA)
                        .append(String.valueOf(result.queuePopulationDiff)).append(COMMA)
                        .append(String.valueOf(result.utilizationDiff)).append(COMMA)
                        .append(String.valueOf(result.lambdaDiff)).append(DELIMITER);
            }

            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static final String RESET = "\033[0m";
    public static final String YELLOW = "\033[0;33m";
    public static final String GREEN = "\033[0;32m";
    public static final String RED = "\033[0;31m";
    public static final String BLUE = "\033[0;34m";

    public static void printComparisonResult(ComparisonResult comparisonResult) {
        System.out.println(BLUE + "\n\n********************************************");
        System.out.println("Comparison results for " + comparisonResult.name.toUpperCase());
        System.out.println("********************************************" + RESET);

        // Print results with color based on the value
        System.out.println("E[Ts]_Diff: " + getColor(comparisonResult.responseTimeDiff) + comparisonResult.responseTimeDiff + RESET);
        System.out.println("E[Tq]_Diff: " + getColor(comparisonResult.queueTimeDiff) + comparisonResult.queueTimeDiff + RESET);
        System.out.println("E[s]_Diff: " + getColor(comparisonResult.serviceTimeDiff) + comparisonResult.serviceTimeDiff + RESET);
        System.out.println("E[Ns]_Diff: " + getColor(comparisonResult.systemPopulationDiff) + comparisonResult.systemPopulationDiff + RESET);
        System.out.println("E[Nq]_Diff: " + getColor(comparisonResult.queuePopulationDiff) + comparisonResult.queuePopulationDiff + RESET);
        System.out.println("rho_Diff: " + getColor(comparisonResult.utilizationDiff) + comparisonResult.utilizationDiff + RESET);
        System.out.println("lambda_Diff: " + getColor(comparisonResult.lambdaDiff) + comparisonResult.lambdaDiff + RESET);

        System.out.println(BLUE + "********************************************" + RESET);
    }

    private static String getColor(double value) {
        if (value < 0.5) {
            return GREEN;
        } else if (value < 1) {
            return YELLOW;
        } else {
            return RED;
        }
    }
}