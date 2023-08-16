package net.commerce.zocalo.ajax.dispatch;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import java.util.Map;
import java.util.HashMap;
import org.mortbay.cometd.AbstractBayeux;
import net.commerce.zocalo.ajax.events.TimingUpdater;
import net.commerce.zocalo.ajax.events.PriceAction;
import net.commerce.zocalo.currency.Price;

/** marshalls events that represent trades that have taken place.  They will be used
    to update the historical section of the stripchart.  */
public class TradeEventDispatcher extends Dispatcher {
    final static public String TRADE_EVENT_TOPIC_SUFFIX = "/historicalUpdate";
    final private TimingUpdater updater;

    public TradeEventDispatcher(AbstractBayeux bayeux, TimingUpdater timingUpdater) {
        super(bayeux, TRADE_EVENT_TOPIC_SUFFIX);
        updater = timingUpdater;
    }

    private void addTimingInfo(Map<String, Object> e) {
        if (updater == null) {
            e.put("round", "0");
            return;
        }
        updater.addTimingInfo(e);
    }

    public void publish(PriceAction action) {
        Map<String, Object> e = new HashMap<String, Object>();
        Price price = action.getNaturalPrice();
        if (! price.isZero()) {
            e.put("traded", price);
        }
        addTimingInfo(e);
        e.put("traderName", action.getOwner());
        e.put("tradeType", action.getClass().getName());
        e.put("quantity", action.getQuantity());
        publishEvent(e);
    }
}
