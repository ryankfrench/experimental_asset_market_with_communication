package net.commerce.zocalo.ajax.events;

import org.apache.log4j.Logger;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.Probability;
import net.commerce.zocalo.ajax.dispatch.PriceChangeDispatcher;

import java.util.*;
// Copyright 2009 Chris Hibbert.  All rights reserved.
//
// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Event used to notify web pages of new prices in a market. */
public class PriceChange extends Action {
    private String marketName;
    private Dictionary<Position, Probability> prices;

    public PriceChange(String marketName, Dictionary<Position, Probability> probabilities) {
        super(getPriceChangeLogger());

        this.marketName = marketName;
        prices = probabilities;

        getLogger().callAppenders(this);
    }

    public static Logger getPriceChangeLogger() {
        return Logger.getLogger(PriceChange.class);
    }

    public String toLogString() {
        StringBuffer buff = new StringBuffer();
        buff.append("prices in market '" + marketName + "' changed.  ");
        Enumeration<Position> positions = prices.keys();
        while (positions.hasMoreElements()) {
            Position pos = positions.nextElement();
            Probability price = prices.get(pos);
            buff.append(pos).append(": ");
            buff.append(price.printAsIntegerPercent());
            if (positions.hasMoreElements()) {
                buff.append(", ");
            }
        }
        return buff.toString();
    }

    public void publishTo(PriceChangeDispatcher dispatcher) {
        Map<String,Object> message = new HashMap<String,Object>();
        message.put("priceChange", new java.util.Date());
        Enumeration<Position> positions = prices.keys();
        while (positions.hasMoreElements()) {
            Position position = positions.nextElement();
            Probability price = prices.get(position);
            message.put(position.getName(), price.printAsIntegerPercent());
        }
        dispatcher.publishTransition(message, marketName);
    }
}
