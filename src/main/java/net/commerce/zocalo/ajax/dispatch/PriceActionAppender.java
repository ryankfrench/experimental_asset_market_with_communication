package net.commerce.zocalo.ajax.dispatch;

// Copyright 2008-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.market.Book;
import net.commerce.zocalo.ajax.events.Action;
import net.commerce.zocalo.ajax.events.PriceAction;
import net.commerce.zocalo.ajax.events.TimingUpdater;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;
import org.mortbay.cometd.AbstractBayeux;

/** publishes actions describing bidding behavior to cometd */
public class PriceActionAppender extends AppenderBase {
    final private BidUpdateDispatcher bidDispatcher;
    TradeEventDispatcher tradeDispatcher;

    private PriceActionAppender(AbstractBayeux bayeux, Book book, TimingUpdater updater) {
        bidDispatcher = new BidUpdateDispatcher(bayeux, book, updater);
        tradeDispatcher = new TradeEventDispatcher(bayeux, updater);
    }

    private PriceActionAppender(AbstractBayeux bayeux) {
        bidDispatcher = null;
        tradeDispatcher = new TradeEventDispatcher(bayeux, null);
    }

    static public PriceActionAppender make(AbstractBayeux bayeux, Book book, TimingUpdater updater) {
        return new PriceActionAppender(bayeux, book, updater);
    }

    static public void registerNewAppender(AbstractBayeux bayeux, Book book, TimingUpdater updater) {
        registerAppender(new PriceActionAppender(bayeux, book, updater));
    }

    static public void registerNewAppender(AbstractBayeux bayeux) {
        registerAppender(new PriceActionAppender(bayeux));
    }

    protected Logger getLogger() {
        return PriceAction.getActionLogger();
    }

    public static PriceActionAppender make(AbstractBayeux bayeux) {
        return new PriceActionAppender(bayeux);
    }

    public void processAction(Action action) {
        action.callBackPublish(this);
    }

    public void publishBidUpdate(Action action) {
        if (bidDispatcher != null) {
            bidDispatcher.publish();
        }
    }

    public void publishTradeEvent(PriceAction action) {
        tradeDispatcher.publish(action);
    }

    protected void append(LoggingEvent event) {
        processAction((Action) event);
    }

    public void close() {
        // NOOP
    }

    public boolean requiresLayout() {
        return false;
    }
}
