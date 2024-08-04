package org.pmcsn.model;

import java.util.ArrayList;
import java.util.List;

public class BatchStatistics extends AbstractStatistics {
    private final List<Double> meanResponseTimeBatch = new ArrayList<>();
    private final List<Double> meanServiceTimeBatch = new ArrayList<>();
    private final List<Double> meanQueueTimeBatch = new ArrayList<>();
    private final List<Double> lambdaBatch = new ArrayList<>();
    private final List<Double> meanSystemPopulationBatch = new ArrayList<>();
    private final List<Double> meanUtilizationBatch = new ArrayList<>();
    private final List<Double> meanQueuePopulationBatch = new ArrayList<>();
    private boolean done = false;
    private final int batchSize;
    private final int batchesNumber;

    public BatchStatistics(String centerName, int batchSize, int batchesNumber) {
        super(centerName);
        this.batchSize = batchSize;
        this.batchesNumber = batchesNumber;
    }

    @Override
    void add(Index index, List<Double> list, double value) {
        switch (index) {
            case ServiceTime -> addPoint(meanServiceTimeList, meanServiceTimeBatch, value);
            case QueueTime -> addPoint(meanQueueTimeList, meanQueueTimeBatch, value);
            case Lambda -> addPoint(lambdaList, lambdaBatch, value);
            case SystemPopulation -> addPoint(meanSystemPopulationList, meanSystemPopulationBatch, value);
            case Utilization -> addPoint(meanUtilizationList, meanUtilizationBatch, value);
            case QueuePopulation -> addPoint(meanQueuePopulationList, meanQueuePopulationBatch, value);
            case ResponseTime -> addPoint(meanResponseTimeList, meanResponseTimeBatch, value);
        }

    }

    private void addPoint(List<Double> list, List<Double> points, double value) {
        points.add(value);
        if (points.size() == batchSize) {
            double average = points.stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            points.clear();
            appendToBatchWindow(list, average);
        }
    }

    private void appendToBatchWindow(List<Double> meanStatsList, double value) {
        if (meanStatsList.size() < batchesNumber) {
            meanStatsList.add(value);
        } else {
            if (!done) {
                System.out.printf("%s has collected %d batches%n%n", getCenterName(), batchesNumber);
            }
            done = true;
        }
    }

    public boolean isDone() {
        return done;
    }
}
