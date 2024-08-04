package org.pmcsn.model;

import java.util.List;

import static org.pmcsn.utils.StatisticsUtils.computeConfidenceInterval;

public class ConfidenceIntervals {
    public double responseTimeCI;
    public double queueTimeCI;
    public double serviceTimeCI;
    public double systemPopulationCI;
    public double queuePopulationCI;
    public double utilizationCI;
    public double lambdaCI;

    public ConfidenceIntervals(List<Double> meanResponseTimeList, List<Double> meanQueueTimeList, List<Double> meanServiceTimeList,
                               List<Double> meanSystemPopulationList, List<Double> meanQueuePopulationList,
                               List<Double> meanUtilizationList, List<Double> lambdaList) {
        this.responseTimeCI = computeConfidenceInterval(meanResponseTimeList);
        this.queueTimeCI = computeConfidenceInterval(meanQueueTimeList);
        this.serviceTimeCI = computeConfidenceInterval(meanServiceTimeList);
        this.systemPopulationCI = computeConfidenceInterval(meanSystemPopulationList);
        this.queuePopulationCI = computeConfidenceInterval(meanQueuePopulationList);
        this.utilizationCI = computeConfidenceInterval(meanUtilizationList);
        this.lambdaCI = computeConfidenceInterval(lambdaList);
    }

    public double getResponseTimeCI() {
        return responseTimeCI;
    }

    public double getQueueTimeCI() {
        return queueTimeCI;
    }

    public double getServiceTimeCI() {
        return serviceTimeCI;
    }

    public double getSystemPopulationCI() {
        return systemPopulationCI;
    }

    public double getQueuePopulationCI() {
        return queuePopulationCI;
    }

    public double getUtilizationCI() {
        return utilizationCI;
    }

    public double getLambdaCI() {
        return lambdaCI;
    }
}