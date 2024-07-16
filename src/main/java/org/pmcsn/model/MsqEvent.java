package org.pmcsn.model;

public class MsqEvent {
    public double time;   //time
    public boolean active;  //status
    public final EventType type; //type
    public int server;  //server (if needed)
    public boolean hasPriority; //if the event has priority


    public MsqEvent(double time, boolean active, EventType type, int server) {
        this.time = time;
        this.active = active;
        this.type = type;
        this.server = server;
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
