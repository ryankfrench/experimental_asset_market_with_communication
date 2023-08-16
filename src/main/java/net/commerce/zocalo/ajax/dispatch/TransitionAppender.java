package net.commerce.zocalo.ajax.dispatch;

// Copyright 2008-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.mortbay.cometd.AbstractBayeux;
import net.commerce.zocalo.ajax.events.Transition;

/** publishes actions representing Session state transitions to cometd. */
public class TransitionAppender extends AppenderBase {
    final private TransitionDispatcher transitionDispatcher;

    public TransitionAppender(AbstractBayeux bayeux) {
        transitionDispatcher = new TransitionDispatcher(bayeux);
    }

    protected void append(LoggingEvent event) {
        ((Transition) event).publishTo(transitionDispatcher);
    }

    static public void registerNewAppender(AbstractBayeux bayeux) {
        registerAppender(new TransitionAppender(bayeux));
    }


    public void close() {
        // NOOP
    }

    public boolean requiresLayout() {
        return false;
    }

    protected Logger getLogger() {
        return Transition.getActionLogger();
    }
}
