package net.commerce.zocalo.history;

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.MultiClaim;

import java.util.*;

import org.jfree.data.time.TimePeriodValuesCollection;
import org.jfree.data.time.TimePeriod;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValues;
// Copyright 2006, 2007 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** PriceHistory records trades for the new Transaction history mechanism.  It collects information about individual
    trades, and can produce Series of trades in the format required by <a href="http://jfreechart.org">JFreeChart</a>.  */
public class PriceHistory {
    private Map posValues;

    public PriceHistory(BinaryClaim claim) {
        posValues = new HashMap();
        posValues.put(claim.getYesPosition(), makeCollection());
    }

    public PriceHistory(MultiClaim claim) {
        posValues = new HashMap();
        for (Iterator iterator = claim.getPositions().iterator(); iterator.hasNext();) {
            posValues.put((Position) iterator.next(), makeCollection());
        }
    }

    private TimePeriodValuesCollection makeCollection() {
        TimePeriodValuesCollection values = new TimePeriodValuesCollection();
        values.addSeries(new TimePeriodValues("Open"));
        values.addSeries(new TimePeriodValues("Close"));
        values.addSeries(new TimePeriodValues("Best Bid"));
        values.addSeries(new TimePeriodValues("Best Ask"));
        return values;
    }

    public void add(Date date, double open, double close, double bestBidAfter, double bestAskAfter) {
        if (posValues.keySet().size() != 1) {
            return;
        }
        Position pos = (Position) posValues.keySet().iterator().next();
        TimePeriodValuesCollection values = getCollection(pos);
        TimePeriod time = new SimpleTimePeriod(date, date);
        values.getSeries(PriceHistoryRecord.OPEN).add(time, open);
        values.getSeries(PriceHistoryRecord.CLOSE).add(time, close);
        values.getSeries(PriceHistoryRecord.BEST_BID).add(time, bestBidAfter);
        values.getSeries(PriceHistoryRecord.BEST_ASK).add(time, bestAskAfter);
    }

    public void add(Date date, Position pos, double open, double close, double bestBidAfter, double bestAskAfter) {
        TimePeriodValuesCollection v = getCollection(pos);
        TimePeriod time = new SimpleTimePeriod(date, date);
        v.getSeries(PriceHistoryRecord.OPEN).add(time, open);
        v.getSeries(PriceHistoryRecord.CLOSE).add(time, close);
        v.getSeries(PriceHistoryRecord.BEST_BID).add(time, bestBidAfter);
        v.getSeries(PriceHistoryRecord.BEST_ASK).add(time, bestAskAfter);
    }

    public TimePeriodValuesCollection getCollection(Position pos) {
        return (TimePeriodValuesCollection) posValues.get(pos);
    }

    public void add(Date date, double open, double close) {
        if (posValues.keySet().size() != 1) {
            return;
        }
        Position pos = (Position) posValues.keySet().iterator().next();
        TimePeriodValuesCollection values = getCollection(pos);
        TimePeriod time = new SimpleTimePeriod(date, date);
        values.getSeries(PriceHistoryRecord.OPEN).add(time, open);
        values.getSeries(PriceHistoryRecord.CLOSE).add(time, close);
    }

    public void add(Date date, Position pos, double open, double close) {
        TimePeriodValuesCollection v = getCollection(pos);
        TimePeriod time = new SimpleTimePeriod(date, date);
        v.getSeries(PriceHistoryRecord.OPEN).add(time, open);
        v.getSeries(PriceHistoryRecord.CLOSE).add(time, close);
    }

    static public void addToSeries(TimePeriodValuesCollection vals, int series, TimePeriod time, double val) {
        vals.getSeries(series).add(time, val);
    }
}
