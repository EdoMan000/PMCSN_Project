package org.pmcsn.model;

import java.util.*;

public class Observations {
    public enum INDEX {
        RESPONSE_TIME;
    }
    private final String centerName;
    private final int n;
    private final List<List<Double>> observations;

    public Observations(String centerName, int runsNumber) {
        this.centerName = centerName;
        this.observations = new ArrayList<>(runsNumber);
        for (int i = 0; i < runsNumber; i++) {
            observations.add(new ArrayList<>());
        }
        this.n = runsNumber;
    }

    public String getCenterName() {
        return centerName;
    }

    public void saveObservation(int run, INDEX index, double point) {
        observations.get(run)
                .add(point);
    }

    public List<Double> welchPlot(INDEX index) {
        OptionalInt o = observations.stream().mapToInt(i -> {
            if (!i.isEmpty()) {
                return i.size();
            }
            return Integer.MAX_VALUE;
        }).min();
        if (o.isEmpty()) {
            throw new IllegalArgumentException("index '" + index + "' does not have any observation collected");
        }
        int m = o.getAsInt();
        List<Double> ensembleAverage = new ArrayList<>(m);

        // Calculate ensemble averages
        for (int i = 0; i < m; i++) {
            double sum = 0.0;
            int count = 0;
            for (int j = 0; j < n; j++) {
                if (!observations.get(j).isEmpty()) {
                    List<Double> Y_ji = observations.get(j);
                    sum += Y_ji.get(i);
                    count += 1;
                }
            }
            ensembleAverage.add(sum / count);
        }

        // Define window size (w)
        int w = Math.min(m / 4, 5); // Choose w as min(m/4, 5)

        // Calculate moving averages
        List<Double> movingAverage = new ArrayList<>(m);
        for (int i = 0; i < m; i++) {
            double sum = 0.0;
            int count = 0;

            if (i < w) {
                for (int s = -i; s <= i; s++) {
                    sum += ensembleAverage.get(i + s);
                    count++;
                }
                movingAverage.add(sum / count);
            } else if (i >= m - w) {
                for (int s = -(m - i - 1); s <= (m - i - 1); s++) {
                    sum += ensembleAverage.get(i + s);
                    count++;
                }
                movingAverage.add(sum / count);
            } else {
                for (int s = -w; s <= w; s++) {
                    sum += ensembleAverage.get(i + s);
                    count++;
                }
                movingAverage.add(sum / (2 * w + 1));
            }
        }

        return movingAverage;
    }
}
