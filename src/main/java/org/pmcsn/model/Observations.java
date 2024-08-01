package org.pmcsn.model;

import java.util.*;

public class Observations {
    public enum INDEX {
        RESPONSE_TIME;
    }
    private final String centerName;
    private final int n;
    private final List<Double> observations;

    public Observations(String centerName, int runsNumber) {
        this.centerName = centerName;
        this.observations = new ArrayList<>();
        this.n = runsNumber;
    }

    public String getCenterName() {
        return centerName;
    }

    public void saveObservation(int run, INDEX index, double point) {
        observations.add(point);
    }

    public List<Double> getPoints() {
        return observations;
    }

    public void reset() {
        observations.clear();
    }
}
