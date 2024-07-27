package org.pmcsn.model;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public class EventQueue {
    private static final Comparator<MsqEvent> CMP = new Comparator<MsqEvent>() {
        @Override
        public int compare(MsqEvent o1, MsqEvent o2) {
            return Double.compare(o1.time, o2.time);
        }
    };

    private final List<PriorityQueue<MsqEvent>> priority = List.of(new PriorityQueue<>(CMP), new PriorityQueue<>(CMP));
    private final PriorityQueue<MsqEvent> noPriority = new PriorityQueue<>(CMP);

    public void add(MsqEvent event) {
        noPriority.add(event);
    }

    public void addPriority(MsqEvent event) {
        if (event.hasPriority) {
            priority.getFirst().add(event);
        } else {
            priority.getLast().add(event);
        }
    }

    public MsqEvent pop() throws Exception {
        MsqEvent e1 = noPriority.peek();
        MsqEvent e2 = peek(priority);
        if (e1 == null && e2 == null) {
            throw new Exception("No events in queue");
        }
        if (e1 == null) {
            return poll(priority);
        } else if (e2 == null) {
            return noPriority.poll();
        } else if (e1.time <= e2.time) {
            return noPriority.poll();
        } else {
            return poll(priority);
        }
    }

    private MsqEvent peek(List<PriorityQueue<MsqEvent>> priorityQueues) {
        for (PriorityQueue<MsqEvent> queue : priorityQueues) {
            if (!queue.isEmpty()) {
                return queue.peek();
            }
        }
        return null;
    }

    private MsqEvent poll(List<PriorityQueue<MsqEvent>> priorityQueues) {
        for (PriorityQueue<MsqEvent> queue : priorityQueues) {
            if (!queue.isEmpty()) {
                return queue.poll();
            }
        }
        return null;
    }
}
