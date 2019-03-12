package com.microsoft.aad.msal4j;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Predicate;

class TelemetryManager implements ITelemetryManager, ITelemetry{

    final ConcurrentHashMap<String, List<Event>> completedEvents = new ConcurrentHashMap<>();
    final ConcurrentHashMap<EventKey, Event> eventsInProgress = new ConcurrentHashMap<>();
    final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> eventCount =
            new ConcurrentHashMap<>();
    private boolean onlySendFailureTelemetry;

    ITelemetryCallback callback;


    public TelemetryManager(ITelemetryCallback callback, boolean onlySendFailureTelemetry){
        this.callback = callback;
        this.onlySendFailureTelemetry = onlySendFailureTelemetry;
    }

    public TelemetryHelper createTelemetryHelper(String requestId, String clientId,
                                                 Event eventToStart, boolean shouldFlush){
        return new TelemetryHelper(this, requestId, clientId, eventToStart, shouldFlush);
    }

    public String generateRequestId(){
        return UUID.randomUUID().toString();
    }

    private boolean hasCallback(){
        return callback != null;
    }

    @Override
    public void startEvent(String requestId, Event eventToStart) {
        if(hasCallback() && !Strings.isNullOrEmpty(requestId)){
            eventsInProgress.put(new EventKey(requestId, eventToStart), eventToStart);
        }
    }

    @Override
    public void stopEvent(String requestId, Event eventToStop){
        if(!hasCallback() || Strings.isNullOrEmpty(requestId)) return;

        EventKey eventKey = new EventKey(requestId, eventToStop);

        Event eventStarted = eventsInProgress.getOrDefault(eventKey, null);
        if(eventStarted == null){
            return;
        }

        eventToStop.stop();
        incrementEventCount(requestId, eventToStop);

        if(!completedEvents.containsKey(requestId)){
            List<Event> eventList = new ArrayList<>(Arrays.asList(eventToStop));
            completedEvents.put(requestId, eventList);
        } else {
            List<Event> eventList =  completedEvents.get(requestId);
            eventList.add(eventToStop);
        }

        eventsInProgress.remove(eventKey);
    }

    @Override
    public void flush(String requestId, String clientId){
        if(!hasCallback()){
            return;
        }

        if(!completedEvents.containsKey(requestId)){
            return;
        }
        completedEvents.get(requestId).addAll(collateOrphanedEvents(requestId));

        List<Event> eventsToFlush = completedEvents.remove(requestId);
        Map<String, Integer> eventCountToFlush = eventCount.remove(requestId);
        eventCountToFlush = !(eventCountToFlush == null) ?
                eventCountToFlush :
                new ConcurrentHashMap<>();

        Predicate<Event> isSuccessfulPredicate = event -> event instanceof ApiEvent &&
                ((ApiEvent) event).getWasSuccesful();
        if(onlySendFailureTelemetry && eventsToFlush.stream().anyMatch(isSuccessfulPredicate)){
            eventsToFlush.clear();
        }
        if(eventsToFlush.size() <= 0){
            return;
        }

        eventsToFlush.add(0, new DefaultEvent(clientId, eventCountToFlush));
        callback.onTelemetryCallback(eventsToFlush);
    }

    private Collection<Event> collateOrphanedEvents(String requestId){
        List<Event> orphanedEvents = new ArrayList<>();
        for(EventKey key: eventsInProgress.keySet()){
            if(key.getRequestId().equalsIgnoreCase(requestId)){
                orphanedEvents.add(eventsInProgress.remove(key));
            }
        }
        return orphanedEvents;
    }

    private void incrementEventCount(String requestId, Event eventToIncrement){
        String eventName = eventToIncrement.get(Event.EVENT_NAME_KEY);
        ConcurrentHashMap<String, Integer> eventNameCount = eventCount.getOrDefault(
                requestId, new ConcurrentHashMap<String, Integer>(){
                    { put(eventName, 0); }
                });
        eventNameCount.put(eventName, eventNameCount.get(eventName) + 1);
        eventCount.put(requestId,eventNameCount);
    }
}
