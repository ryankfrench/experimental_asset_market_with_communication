package net.commerce.zocalo.history;

import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.claim.Position;
import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.time.SimpleTimePeriod;

import java.util.Date;
// Copyright 2006, 2007 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** MarketTradeRec represents a single step in a trade between a new order and
    a MarketMaker.  It may be combined with BookTradeRecs and MarketTradeRecs 
    into a single TransactionRec. */
public class MarketTradeRec extends TradeRec {
    private Position position;
    private Date date;
    private double open;
    private double close;
    private double quantity;

    public MarketTradeRec(SecureUser taker, Position position, double open, double close, double quantity, Date date) {
        super(taker);
        this.position = position;
        this.open = open;
        this.close = close;
        this.quantity = quantity;
        this.date = date;
    }

    public double getQuantity() {
        return quantity;
    }

    public void addTo(TimePeriodValuesCollection c) {
        SimpleTimePeriod time = simpleTime(date);
        c.getSeries(OPEN).add(time, open);
        c.getSeries(CLOSE).add(time, close);
    }
}
