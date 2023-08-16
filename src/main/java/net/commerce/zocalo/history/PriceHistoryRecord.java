package net.commerce.zocalo.history;

import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.time.SimpleTimePeriod;

import java.util.Date;

import net.commerce.zocalo.currency.Price;
// Copyright 2006, 2007 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** PriceHistoryRecord is a start on a framework for tracking trading history.
 When complete, it will ensure that multiple trades that happen as a result
 of a single new order are recognized as a unit, which will make representation
 of price changes more correct.  */
public abstract class PriceHistoryRecord {
    static final public int OPEN = 0;
    static final public int CLOSE = 1;
    static final public int BEST_BID = 2;
    static final public int BEST_ASK = 3;
    static final public int QUANTITY = 4;

    protected abstract double getQuantity();

    public abstract void addTo(TimePeriodValuesCollection c);

    SimpleTimePeriod simpleTime(Date date) {
        return new SimpleTimePeriod(date, date);
    }

    public Price getOpen() {
        return null;
    }

    public Price getClose() {
        return null;
    }
}
