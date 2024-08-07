package org.pmcsn.model;

import java.util.List;

public class BatchStatistics extends AbstractStatistics {
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
        list.add(value);
        if(list.size() >= batchesNumber) {
            done = true;
        }
    }

    public boolean isDone() {
        return done;
    }
}
