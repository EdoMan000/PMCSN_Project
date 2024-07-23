package org.pmcsn.utils;

import org.pmcsn.model.MsqEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class EventUtils {

    public static MsqEvent getNextEvent(List<MsqEvent> events) {

        if (events == null || events.isEmpty()) {
            return null; // or throw an exception depending on your use case
        }
        MsqEvent minEvent = events.getFirst();

        List<MsqEvent> eventsWithPriority = new ArrayList<>(events);
        eventsWithPriority.removeIf(event -> !event.hasPriority);
        eventsWithPriority.sort(Comparator.comparing(MsqEvent::getTime));
        if (!eventsWithPriority.isEmpty()) {
            MsqEvent minEventPrio = eventsWithPriority.getFirst();

            List<MsqEvent> eventsWithoutPriority = new ArrayList<>(events);
            eventsWithoutPriority.removeIf(event -> event.hasPriority);
            eventsWithoutPriority.sort(Comparator.comparing(MsqEvent::getTime));
            minEvent = eventsWithoutPriority.getFirst();

            // minEventPrio has the lowest time of all events
            if (minEventPrio.time < minEvent.time) {
                minEvent = minEventPrio;

                // minEventPrio has the same time of other events (but they have equal or following type)
            } else if (minEventPrio.time == minEvent.time && minEventPrio.type.ordinal() <= minEvent.type.ordinal()) {
                minEvent = minEventPrio;
            }
            // minEventPrio has the same time of other events but they have prior type so they need to be processed before
            // (minEvent does not need to be changed)
        }


        // Now I can check as before
        for (MsqEvent event : events) {
            if (event.getTime() > minEvent.getTime()) {
                break; // Since the list is sorted by time, no need to check further

                // note that minEvent is changed only if the type is prior the current type (does not interfere with priority)
            } else if (event.getTime() == minEvent.getTime() && event.type.ordinal() < minEvent.type.ordinal()) {
                minEvent = event;
            }
        }

        return minEvent;

    }
}
