// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.

package com.microsoft.aad.msal4j;

import java.util.Objects;

class EventKey {
    private String requestId;
    private String eventName;

    EventKey(String requestId, Event event) {
        this.requestId = requestId;
        this.eventName = event.get(Event.EVENT_NAME_KEY);
    }

    /**
     * Gets the request id.
     * 
     * @return the request id
     */
    public String getRequestId() {
        return requestId;
    }

    /**
     * Gets the event name.
     * 
     * @return the event name
     */
    public String getEventName() {
        return eventName;
    }

    @Override
    /**
     * TODO: Add description
     */
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof EventKey)) return false;
        if (obj == this) return true;

        EventKey eventKey = (EventKey) obj;
        return Objects.equals(requestId, eventKey.getRequestId()) &&
                Objects.equals(eventName, eventKey.getEventName());
    }

    @Override
    /**
     * Checks if has h code.
     * 
     * @return true if has h code, false otherwise
     */
    public int hashCode() {
        return Objects.hash(requestId, eventName);
    }

}
