package net.commerce.zocalo.ajax.dispatch;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.market.Book;
import net.commerce.zocalo.ajax.events.TimingUpdater;
import org.mortbay.cometd.AbstractBayeux;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/** marshal events that represent changes to the active bids displayed in the stripchart.  */
public class BidUpdateDispatcher extends Dispatcher {
    final static public String BID_UPTDATE_TOPIC_URI = "/liveUpdate";
    final private TimingUpdater updater;
    final private Book book;

    public BidUpdateDispatcher(AbstractBayeux bayeux, Book book, TimingUpdater timingUpdater) {
        super(bayeux, BID_UPTDATE_TOPIC_URI);
        this.book = book;
        updater = timingUpdater;
    }

    /** publish an update on current prices */
    public void publish() {
        Map<String, Object> refreshEvent = new HashMap<String, Object>();
        BinaryClaim claim = (BinaryClaim) book.getClaim();
        refreshEvent.put("sell", book.buildPriceList(claim.getNoPosition()));
        refreshEvent.put("buy", book.buildPriceList(claim.getYesPosition()));
        if (updater == null) {
            refreshEvent.put("round", "0");
        } else {
            updater.addTimingInfo(refreshEvent);
        }
        publishEvent(refreshEvent);
    }
}
