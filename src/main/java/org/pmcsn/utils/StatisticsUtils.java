package org.pmcsn.utils;

import org.pmcsn.libraries.Rvms;

import java.util.List;

public class StatisticsUtils {


    public static double computeMean(List<Double> values) {
        return values.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
    }

    private static double getStandardDeviation(List<Double> values) {
        double mean = computeMean(values);
        double variance = values.stream().mapToDouble(v -> Math.pow(v - mean, 2)).sum() / (values.size() - 1);
        return Math.sqrt(variance);
    }

    public static double computeConfidenceInterval(List<Double> values) {
        int K = values.size();
        // Compute confidence intervals
        Rvms rvms = new Rvms();
        // for 95% confidence interval
        double alpha = 0.05;
        // t* in the formula
        double criticalValue = rvms.idfStudent(K - 1, 1 - alpha / 2);

        return criticalValue * getStandardDeviation(values) / Math.sqrt(K - 1);
    }

}
