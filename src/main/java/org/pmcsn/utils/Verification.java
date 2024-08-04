package org.pmcsn.utils;

import org.pmcsn.model.ConfidenceIntervals;
import org.pmcsn.model.MeanStatistics;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Verification {

    public static class VerificationResult {
        public String name;
        public String metric;
        public double diffValue;
        public double confidenceInterval;
        public boolean withinInterval;

        public VerificationResult(String name, String metric, double diffValue, double confidenceInterval, boolean withinInterval) {
            this.name = name;
            this.metric = metric;
            this.diffValue = diffValue;
            this.confidenceInterval = confidenceInterval;
            this.withinInterval = withinInterval;
        }
    }

    public static List<VerificationResult> verifyConfidenceIntervals(String simulationType, List<Comparison.ComparisonResult> comparisonResultList, List<ConfidenceIntervals> confidenceIntervalsList) {
        List<VerificationResult> verificationResults = new ArrayList<>();

        if (comparisonResultList.size() != confidenceIntervalsList.size()) {
            System.out.println("Mismatch in the size of comparison results and confidence intervals lists");
            return verificationResults;
        }

        for (int i = 0; i < comparisonResultList.size(); i++) {
            Comparison.ComparisonResult comparisonResult = comparisonResultList.get(i);
            ConfidenceIntervals confidenceIntervals = confidenceIntervalsList.get(i);

            verificationResults.add(createVerificationResult(comparisonResult.name, "Response Time (Ets)", comparisonResult.responseTimeDiff, confidenceIntervals.getResponseTimeCI()));
            verificationResults.add(createVerificationResult(comparisonResult.name, "Queue Time (Etq)", comparisonResult.queueTimeDiff, confidenceIntervals.getQueueTimeCI()));
            verificationResults.add(createVerificationResult(comparisonResult.name, "Service Time (Es)", comparisonResult.serviceTimeDiff, confidenceIntervals.getServiceTimeCI()));
            verificationResults.add(createVerificationResult(comparisonResult.name, "System Population (Ens)", comparisonResult.systemPopulationDiff, confidenceIntervals.getSystemPopulationCI()));
            verificationResults.add(createVerificationResult(comparisonResult.name, "Queue Population (Enq)", comparisonResult.queuePopulationDiff, confidenceIntervals.getQueuePopulationCI()));
            verificationResults.add(createVerificationResult(comparisonResult.name, "Utilization (rho)", comparisonResult.utilizationDiff, confidenceIntervals.getUtilizationCI()));
            verificationResults.add(createVerificationResult(comparisonResult.name, "Lambda (lambda)", comparisonResult.lambdaDiff, confidenceIntervals.getLambdaCI()));
        }

        writeVerificationResults(simulationType, verificationResults);
        return verificationResults;
    }

    public static VerificationResult createVerificationResult(String name, String metric, double diffValue, double confidenceInterval) {
        boolean withinInterval = diffValue <= confidenceInterval;
        return new VerificationResult(name, metric, diffValue, confidenceInterval, withinInterval);
    }

    public static void writeVerificationResults(String modelName, List<VerificationResult> verificationResults) {
        File file = new File("csvFiles/" + modelName + "/verification/");
        if (!file.exists()) {
            file.mkdirs();
        }

        file = new File("csvFiles/" + modelName + "/verification/verification.csv");
        try (FileWriter fileWriter = new FileWriter(file)) {
            String DELIMITER = "\n";
            String COMMA = ",";

            fileWriter.append("Center, Metric, Diff Value, Confidence Interval, Within Interval").append(DELIMITER);
            for (VerificationResult verificationResult : verificationResults) {
                fileWriter.append(verificationResult.name).append(COMMA)
                        .append(verificationResult.metric).append(COMMA)
                        .append(String.valueOf(verificationResult.diffValue)).append(COMMA)
                        .append(String.valueOf(verificationResult.confidenceInterval)).append(COMMA)
                        .append(String.valueOf(verificationResult.withinInterval)).append(DELIMITER);
            }

            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
