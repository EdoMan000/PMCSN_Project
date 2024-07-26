package org.pmcsn.model;

public class MsqEvent {
    public double service;
    public double time;   //time
    public boolean active;  //status
    public final EventType type; //type
    public int serverId;  //server (if needed)
    public boolean hasPriority = false;     //if the event has priority
    public int centerID;

    public MsqEvent(EventType type, double service, double time, boolean active, int serverId, boolean hasPriority, int centerID) {
        this.type = type;
        this.service = service;
        this.time = time;
        this.active = active;
        this.serverId = serverId;
        this.hasPriority = hasPriority;
        this.centerID = centerID;
    }

    public MsqEvent(double time, boolean active, EventType type, int serverId) {
        this.time = time;
        this.active = active;
        this.type = type;
        this.serverId = serverId;
    }

    public MsqEvent(double time, boolean active, EventType type, int serverId, int centerID) {
        this.time = time;
        this.active = active;
        this.type = type;
        this.serverId = serverId;
        this.centerID = centerID;
    }

    public MsqEvent(double time, boolean active, EventType type) {
        this.time = time;
        this.active = active;
        this.type = type;
    }

    public MsqEvent(double time, boolean active, EventType type, int serverId, boolean hasPriority) {
        this.time = time;
        this.active = active;
        this.type = type;
        this.serverId = serverId;
        this.hasPriority = hasPriority;
    }

    public double getTime(){
        return time;
    }
}
