package net.commerce.zocalo.ajax.dispatch;

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.mortbay.cometd.AbstractBayeux;
import net.commerce.zocalo.ajax.events.IndividualTimingEvent;
// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Uses log4j to dispatch events to individual endpoints
 rather than broadcasting them. */
public class PrivateEventAppender extends AppenderBase {
    private PrivateEventDispatcher dispatcher;

    private PrivateEventAppender(AbstractBayeux bayeux) {
        dispatcher = new PrivateEventDispatcher(bayeux);
    }

    static public void registerNewAppender(AbstractBayeux bayeux) {
        registerAppender(new PrivateEventAppender(bayeux));
    }

    protected Logger getLogger() {
        return IndividualTimingEvent.getActionLogger();
    }

    protected void append(LoggingEvent event) {
        ((IndividualTimingEvent)event).sendTo(dispatcher);
    }

    public void close() {
        // No op
    }

    public boolean requiresLayout() {
        return false;
    }
}
