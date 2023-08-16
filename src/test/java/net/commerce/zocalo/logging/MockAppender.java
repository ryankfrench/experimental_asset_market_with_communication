package net.commerce.zocalo.logging;

// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.spi.LoggingEvent;

import java.util.Set;
import java.util.HashSet;

/** allow tests to examine what gets stored in the log.  */
public class MockAppender extends AppenderSkeleton {
    private final Set events = new HashSet();

    protected void append(LoggingEvent event) {
        events.add(event);
    }

    public void close() {

    }

    public boolean requiresLayout() {
        return false;
    }

    public int messageCount() {
        return events.size();
    }

    public Set getEvents() {
        return events;
    }
}
