package org.pmcsn.model;

public class MsqServer {
    public double lastCompletionTime;
    public boolean running;

    public MsqServer() {
        lastCompletionTime = 0;
        running = false;
    }
}
