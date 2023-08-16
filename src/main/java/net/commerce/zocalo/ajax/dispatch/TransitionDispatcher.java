package net.commerce.zocalo.ajax.dispatch;

import java.util.Map;
import java.net.URI;

import org.mortbay.cometd.AbstractBayeux;

// Copyright 2007-2009 Chris Hibbert.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** marshalls events that represent state transitions in the Session.  They can
    effect the browser display in various ways.  */
public class TransitionDispatcher extends Dispatcher {
    final static public String TRANSITION_TOPIC_URI = "/transition";

    public TransitionDispatcher(AbstractBayeux bayeux) {
        super(bayeux, TRANSITION_TOPIC_URI);
    }

    public void publishTransition(Map<String, Object> message) {
        publishEvent(message);
    }
}
