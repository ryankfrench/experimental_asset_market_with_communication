package net.commerce.zocalo.history;

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import org.jfree.data.time.TimePeriodValuesCollection;

import java.util.Date;
// Copyright 2006, 2007 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** PriceChangeRec represents a price change not associated with a trade as a result of the
 addition or withdrawal of a book order. */
public class PriceChangeRec extends PriceHistoryRecord {
    private Position position;
    private Date date;
    private double bidAfter;
    private double askAfter;

    public PriceChangeRec(Position position, double bidAfter, double askAfter, Date date) {
        this.position = position;
        this.bidAfter = bidAfter;
        this.askAfter = askAfter;
        this.date = date;
    }

    public PriceChangeRec(BinaryClaim claim, double bidAfter, double askAfter, Date date) {
        position = claim.getYesPosition();
        this.bidAfter = bidAfter;
        this.askAfter = askAfter;
        this.date = date;
    }

    public static PriceChangeRec makeBidPriceChange(Position position, double bidAfter, Date date) {
        return new PriceChangeRec(position, bidAfter, -1, date);
    }

    public static PriceChangeRec makeBidPriceChange(BinaryClaim claim, double bidAfter, Date date) {
        return new PriceChangeRec(claim, bidAfter, -1, date);
    }

    public static PriceChangeRec makeAskPriceChange(Position position, double askAfter, Date date) {
        return new PriceChangeRec(position, -1, askAfter, date);
    }

    public static PriceChangeRec makeAskPriceChange(BinaryClaim claim, double askAfter, Date date) {
        return new PriceChangeRec(claim, -1, askAfter, date);
    }

    public double getQuantity() {
        return 0;
    }

    public void addTo(TimePeriodValuesCollection c) {
        if (-1 != bidAfter) {
            c.getSeries(BEST_BID).add(simpleTime(date), bidAfter);
        }
        if (-1 != askAfter) {
            c.getSeries(BEST_ASK).add(simpleTime(date), askAfter);
        }
    }
}
