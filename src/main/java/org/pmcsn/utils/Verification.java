package org.pmcsn.utils;

import org.pmcsn.model.ConfidenceIntervals;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Verification {

    public static class VerificationResult {
        public String name;
        public double responseTimeDiff;
        public double queueTimeDiff;
        public double serviceTimeDiff;
        public double systemPopulationDiff;
        public double queuePopulationDiff;
        public double utilizationDiff;
        public double lambdaDiff;
        public double responseTimeCI;
        public double queueTimeCI;
        public double serviceTimeCI;
        public double systemPopulationCI;
        public double queuePopulationCI;
        public double utilizationCI;
        public double lambdaCI;
        public boolean responseTimeWithinInterval;
        public boolean queueTimeWithinInterval;
        public boolean serviceTimeWithinInterval;
        public boolean systemPopulationWithinInterval;
        public boolean queuePopulationWithinInterval;
        public boolean utilizationWithinInterval;
        public boolean lambdaWithinInterval;

        public VerificationResult(String name,
                                  double responseTimeDiff, double responseTimeCI, boolean responseTimeWithinInterval,
                                  double queueTimeDiff, double queueTimeCI, boolean queueTimeWithinInterval,
                                  double serviceTimeDiff, double serviceTimeCI, boolean serviceTimeWithinInterval,
                                  double systemPopulationDiff, double systemPopulationCI, boolean systemPopulationWithinInterval,
                                  double queuePopulationDiff, double queuePopulationCI, boolean queuePopulationWithinInterval,
                                  double utilizationDiff, double utilizationCI, boolean utilizationWithinInterval,
                                  double lambdaDiff, double lambdaCI, boolean lambdaWithinInterval) {
            this.name = name;
            this.responseTimeDiff = responseTimeDiff;
            this.responseTimeCI = responseTimeCI;
            this.responseTimeWithinInterval = responseTimeWithinInterval;
            this.queueTimeDiff = queueTimeDiff;
            this.queueTimeCI = queueTimeCI;
            this.queueTimeWithinInterval = queueTimeWithinInterval;
            this.serviceTimeDiff = serviceTimeDiff;
            this.serviceTimeCI = serviceTimeCI;
            this.serviceTimeWithinInterval = serviceTimeWithinInterval;
            this.systemPopulationDiff = systemPopulationDiff;
            this.systemPopulationCI = systemPopulationCI;
            this.systemPopulationWithinInterval = systemPopulationWithinInterval;
            this.queuePopulationDiff = queuePopulationDiff;
            this.queuePopulationCI = queuePopulationCI;
            this.queuePopulationWithinInterval = queuePopulationWithinInterval;
            this.utilizationDiff = utilizationDiff;
            this.utilizationCI = utilizationCI;
            this.utilizationWithinInterval = utilizationWithinInterval;
            this.lambdaDiff = lambdaDiff;
            this.lambdaCI = lambdaCI;
            this.lambdaWithinInterval = lambdaWithinInterval;
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

            verificationResults.add(new VerificationResult(
                    comparisonResult.name,
                    comparisonResult.responseTimeDiff, confidenceIntervals.getResponseTimeCI(), comparisonResult.responseTimeDiff <= confidenceIntervals.getResponseTimeCI(),
                    comparisonResult.queueTimeDiff, confidenceIntervals.getQueueTimeCI(), comparisonResult.queueTimeDiff <= confidenceIntervals.getQueueTimeCI(),
                    comparisonResult.serviceTimeDiff, confidenceIntervals.getServiceTimeCI(), comparisonResult.serviceTimeDiff <= confidenceIntervals.getServiceTimeCI(),
                    comparisonResult.systemPopulationDiff, confidenceIntervals.getSystemPopulationCI(), comparisonResult.systemPopulationDiff <= confidenceIntervals.getSystemPopulationCI(),
                    comparisonResult.queuePopulationDiff, confidenceIntervals.getQueuePopulationCI(), comparisonResult.queuePopulationDiff <= confidenceIntervals.getQueuePopulationCI(),
                    comparisonResult.utilizationDiff, confidenceIntervals.getUtilizationCI(), comparisonResult.utilizationDiff <= confidenceIntervals.getUtilizationCI(),
                    comparisonResult.lambdaDiff, confidenceIntervals.getLambdaCI(), comparisonResult.lambdaDiff <= confidenceIntervals.getLambdaCI()
            ));
        }

        writeVerificationResults(simulationType, verificationResults);
        return verificationResults;
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

            fileWriter.append("Center, E[Ts]_Diff, E[Ts]_CI, E[Ts]_Within, E[Tq]_Diff, E[Tq]_CI, E[Tq]_Within, E[s]_Diff, E[s]_CI, E[s]_Within, E[Ns]_Diff, E[Ns]_CI, E[Ns]_Within, E[Nq]_Diff, E[Nq]_CI, E[Nq]_Within, ρ_Diff, ρ_CI, ρ_Within, λ_Diff, λ_CI, λ_Within").append(DELIMITER);
            for (VerificationResult verificationResult : verificationResults) {
                fileWriter.append(verificationResult.name).append(COMMA)
                        .append(String.valueOf(verificationResult.responseTimeDiff)).append(COMMA)
                        .append(String.valueOf(verificationResult.responseTimeCI)).append(COMMA)
                        .append(String.valueOf(verificationResult.responseTimeWithinInterval)).append(COMMA)
                        .append(String.valueOf(verificationResult.queueTimeDiff)).append(COMMA)
                        .append(String.valueOf(verificationResult.queueTimeCI)).append(COMMA)
                        .append(String.valueOf(verificationResult.queueTimeWithinInterval)).append(COMMA)
                        .append(String.valueOf(verificationResult.serviceTimeDiff)).append(COMMA)
                        .append(String.valueOf(verificationResult.serviceTimeCI)).append(COMMA)
                        .append(String.valueOf(verificationResult.serviceTimeWithinInterval)).append(COMMA)
                        .append(String.valueOf(verificationResult.systemPopulationDiff)).append(COMMA)
                        .append(String.valueOf(verificationResult.systemPopulationCI)).append(COMMA)
                        .append(String.valueOf(verificationResult.systemPopulationWithinInterval)).append(COMMA)
                        .append(String.valueOf(verificationResult.queuePopulationDiff)).append(COMMA)
                        .append(String.valueOf(verificationResult.queuePopulationCI)).append(COMMA)
                        .append(String.valueOf(verificationResult.queuePopulationWithinInterval)).append(COMMA)
                        .append(String.valueOf(verificationResult.utilizationDiff)).append(COMMA)
                        .append(String.valueOf(verificationResult.utilizationCI)).append(COMMA)
                        .append(String.valueOf(verificationResult.utilizationWithinInterval)).append(COMMA)
                        .append(String.valueOf(verificationResult.lambdaDiff)).append(COMMA)
                        .append(String.valueOf(verificationResult.lambdaCI)).append(COMMA)
                        .append(String.valueOf(verificationResult.lambdaWithinInterval)).append(DELIMITER);
            }

            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
