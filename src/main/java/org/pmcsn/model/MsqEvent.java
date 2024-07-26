package org.pmcsn.model;

public class MsqEvent {
    public double service;
    public double time;   //time
    public boolean active;  //status
    public final EventType type; //type
    public int server;  //server (if needed)
    public boolean hasPriority = false;     //if the event has priority
    public int centerID;

    public MsqEvent(EventType type, double service, double time, boolean active, int server, boolean hasPriority, int centerID) {
        this.type = type;
        this.service = service;
        this.time = time;
        this.active = active;
        this.server = server;
        this.hasPriority = hasPriority;
        this.centerID = centerID;
    }

    public MsqEvent(double time, boolean active, EventType type, int server) {
        this.time = time;
        this.active = active;
        this.type = type;
        this.server = server;
    }

    public MsqEvent(double time, boolean active, EventType type, int server, int centerID) {
        this.time = time;
        this.active = active;
        this.type = type;
        this.server = server;
        this.centerID = centerID;
    }

    public MsqEvent(double time, boolean active, EventType type) {
        this.time = time;
        this.active = active;
        this.type = type;
    }

    public MsqEvent(double time, boolean active, EventType type, int server, boolean hasPriority) {
        this.time = time;
        this.active = active;
        this.type = type;
        this.server = server;
        this.hasPriority = hasPriority;
    }

    public double getTime(){
        return time;
    }
}
