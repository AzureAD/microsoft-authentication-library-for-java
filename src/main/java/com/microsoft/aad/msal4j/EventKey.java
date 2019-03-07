package com.microsoft.aad.msal4j;

class EventKey {
    private String requestId;
    private String eventName;

    EventKey(String requestId, Event event){
        this.requestId = requestId;
        this.eventName = event.get(Event.EVENT_NAME_KEY);
    }

    public String getRequestId() {
        return requestId;
    }

    public String getEventName() {
        return eventName;
    }

    @Override
    public boolean equals(Object obj) {
        if(obj == null) return false;
        if(!(obj instanceof EventKey)) return false;
        if(obj == this) return true;

        return requestId.equalsIgnoreCase(((EventKey) obj).requestId) &&
                eventName.equalsIgnoreCase(((EventKey) obj).eventName);
    }
}
