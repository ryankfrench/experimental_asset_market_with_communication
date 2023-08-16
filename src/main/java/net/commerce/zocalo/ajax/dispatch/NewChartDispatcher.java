package net.commerce.zocalo.ajax.dispatch;

import org.mortbay.cometd.AbstractBayeux;

import java.net.URI;
import java.util.Map;

// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** marshalls events that announce the availability of
 {@link net.commerce.zocalo.ajax.events.NewChart new price charts}.  */
public class NewChartDispatcher extends Dispatcher {
    public static final String NEW_CHART_TOPIC_URI = "/newChart/";

    public NewChartDispatcher(AbstractBayeux bayeux, String marketName) {
        super(bayeux, marketName, NEW_CHART_TOPIC_URI);
    }

    public void publishTransition(Map<String, Object> message) {
        publishEvent(message);
    }
}
