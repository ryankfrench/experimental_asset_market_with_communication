package net.commerce.zocalo.ajax.dispatch;

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.mortbay.cometd.AbstractBayeux;
import net.commerce.zocalo.ajax.events.PriceChange;

// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** publishes actions announcing {@link net.commerce.zocalo.ajax.events.PriceChange
new MarketMaker prices} to cometd. */
public class PriceChangeAppender extends AppenderBase {
    private PriceChangeDispatcher dispatcher;

    private PriceChangeAppender(AbstractBayeux bayeux, String marketName) {
        dispatcher = new PriceChangeDispatcher(bayeux, marketName);
    }

    static public void registerNewAppender(AbstractBayeux bayeux, String marketName) {
        AppenderBase base = new PriceChangeAppender(bayeux, marketName);
        registerAppender(base);
    }

    protected Logger getLogger() {
        return PriceChange.getPriceChangeLogger();
    }

    protected void append(LoggingEvent event) {
        ((PriceChange)event).publishTo(dispatcher);
    }

    public boolean requiresLayout() {
        return false;
    }

    public void close() {
        // Do nothing
    }
}
