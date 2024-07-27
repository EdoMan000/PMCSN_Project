package org.pmcsn.model;

import java.util.Comparator;
import java.util.PriorityQueue;

public class EventQueue {
    private static final Comparator<MsqEvent> NO_PRIORITY_CMP = new Comparator<MsqEvent>() {
        @Override
        public int compare(MsqEvent o1, MsqEvent o2) {
            return Double.compare(o1.time, o2.time);
        }
    };

    private static final Comparator<MsqEvent> PRIORITY_CMP = new Comparator<MsqEvent>() {
        @Override
        public int compare(MsqEvent o1, MsqEvent o2) {
            return Boolean.compare(o1.hasPriority, o2.hasPriority) & Double.compare(o1.time, o2.time);
        }
    };

    private final PriorityQueue<MsqEvent> noPriority = new PriorityQueue<>(NO_PRIORITY_CMP);
    private final PriorityQueue<MsqEvent> priority = new PriorityQueue<>(PRIORITY_CMP);

    public void add(MsqEvent event) {
        noPriority.add(event);
    }



    public MsqEvent pop() throws Exception {
        MsqEvent e1 = priority.peek();
        MsqEvent e2 = noPriority.peek();
        if (e1 == null && e2 == null) {
            throw new Exception("queue is empty");
        } else {
            if (e1 == null) {
                return noPriority.poll();
            } else if (e2 == null) {
                priority.poll();
            } else if (e1.time < e2.time) {
                return priority.poll();
            }
            return noPriority.poll();
        }
    }
}
