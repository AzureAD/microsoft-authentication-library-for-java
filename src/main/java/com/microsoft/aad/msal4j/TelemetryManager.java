// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.aad.msal4j;

import com.google.common.base.Strings;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

class TelemetryManager implements ITelemetryManager, ITelemetry{

    private final ConcurrentHashMap<String, List<Event>> completedEvents = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<EventKey, Event> eventsInProgress = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<String, Integer>> eventCount =
            new ConcurrentHashMap<>();

    private boolean onlySendFailureTelemetry;
    private Consumer<List<HashMap<String,String>>>  telemetryConsumer;

    public TelemetryManager(Consumer<List<HashMap<String,String>>> telemetryConsumer,
                            boolean onlySendFailureTelemetry){
        this.telemetryConsumer= telemetryConsumer;
        this.onlySendFailureTelemetry = onlySendFailureTelemetry;
    }

    public TelemetryHelper createTelemetryHelper(String requestId,
                                                 String clientId,
                                                 Event eventToStart,
                                                 Boolean shouldFlush){
        return new TelemetryHelper(this, requestId, clientId, eventToStart, shouldFlush);
    }

    public String generateRequestId(){
        return UUID.randomUUID().toString();
    }

    @Override
    public void startEvent(String requestId, Event eventToStart) {
        if(hasConsumer() && !Strings.isNullOrEmpty(requestId)){
            eventsInProgress.put(new EventKey(requestId, eventToStart), eventToStart);
        }
    }

    @Override
    public void stopEvent(String requestId, Event eventToStop){
        if(!hasConsumer() || Strings.isNullOrEmpty(requestId)) return;

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
        if(!hasConsumer()){
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
                ((ApiEvent) event).getWasSuccessful();
        if(onlySendFailureTelemetry && eventsToFlush.stream().anyMatch(isSuccessfulPredicate)){
            eventsToFlush.clear();
        }
        if(eventsToFlush.size() <= 0){
            return;
        }
        eventsToFlush.add(0, new DefaultEvent(clientId, eventCountToFlush));

        telemetryConsumer.accept(Collections.unmodifiableList(eventsToFlush));
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

        eventNameCount.put(eventName, eventNameCount.getOrDefault(eventName, 0) + 1);
        eventCount.put(requestId,eventNameCount);
    }

    private boolean hasConsumer(){
        return telemetryConsumer != null;
    }
}
