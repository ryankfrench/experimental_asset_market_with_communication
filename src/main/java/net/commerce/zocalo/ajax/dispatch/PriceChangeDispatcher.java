package net.commerce.zocalo.ajax.dispatch;

import org.mortbay.cometd.AbstractBayeux;

import java.net.URI;
import java.util.Map;
// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** marshalls events that represent {@link net.commerce.zocalo.ajax.events.PriceChange new price levels}.  */
public class PriceChangeDispatcher extends Dispatcher {
    public static final String PRICE_CHANGE_TOPIC_URI = "/priceChange/";

    public PriceChangeDispatcher(AbstractBayeux bayeux, String marketName) {
        super(bayeux, marketName, PRICE_CHANGE_TOPIC_URI);
    }

    public void publishTransition(Map<String, Object> message, String marketName) {
        if (matchesTopic(marketName)) {
            publishEvent(message);
        }
    }
}
