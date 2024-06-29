package org.pmcsn.model;

public class MsqEvent {
    private double t;   //time
    private int x;  //status
    private EventType type; //type
    private int s;  //server (if needed)


    public MsqEvent(double t, int x, EventType type, int s) {
        this.t = t;
        this.x = x;
        this.type = type;
        this.s = s;
    }

    public double getT() {
        return t;
    }

    public int getX() {
        return x;
    }

    public EventType getType() {
        return type;
    }

    public int getS() {
        return s;
    }
}
