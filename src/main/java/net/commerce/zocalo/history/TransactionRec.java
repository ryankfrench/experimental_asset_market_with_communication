package net.commerce.zocalo.history;

import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.currency.Price;
import org.jfree.data.time.SimpleTimePeriod;
import org.jfree.data.time.TimePeriodValuesCollection;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;


// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** TransactionRec represents a complete transaction, which may include
    multiple book and market trades when a single new order consumes more
    than a single book order or both book orders and MarketMaker offers. */
public class TransactionRec extends PriceHistoryRecord {
    private Date date = new Date();
    private ArrayList actions = new ArrayList();

    public TransactionRec() {
    }

    /** @deprecated */
    public TransactionRec(Date testDate) {
        date = testDate;
    }

    public void addBookTrade(SecureUser taker, double quantity, Order order) {
        actions.add(new BookTradeRec(taker, quantity, order));
    }

    public void addMarketTrade(Position position, SecureUser taker, double open, double close, double quantity) {
        actions.add(new MarketTradeRec(taker, position, open, close, quantity, date));
    }

    public void addPriceChange(Position position, double bidAfter, double askAfter) {
        actions.add(new PriceChangeRec(position, bidAfter, askAfter, date));
    }

    public void addPriceChange(BinaryClaim claim, double bidAfter, double askAfter) {
        actions.add(new PriceChangeRec(claim, bidAfter, askAfter, date));
    }

    public void addBidPriceChange(Position position, double bidAfter) {
        actions.add(PriceChangeRec.makeBidPriceChange(position, bidAfter, date));
    }

    public void addBidPriceChange(BinaryClaim claim, double bidAfter) {
        actions.add(PriceChangeRec.makeBidPriceChange(claim, bidAfter, date));
    }

    public void addAskPriceChange(Position position, double askAfter) {
        actions.add(PriceChangeRec.makeAskPriceChange(position, askAfter, date));
    }

    public void addAskPriceChange(BinaryClaim claim, double askAfter) {
        actions.add(PriceChangeRec.makeAskPriceChange(claim, askAfter, date));
    }

    public void addTransactionsTo(TimePeriodValuesCollection c){
        double quantity = 0;
        Price open = null;
        Price close = null;
        for (int i = 0; i < actions.size(); i++) {
            PriceHistoryRecord xAction = (PriceHistoryRecord) actions.get(i);
            quantity += xAction.getQuantity();

            // use first open and last close
            if (open == null) {
                open = xAction.getOpen();
            }
            Price newClose = xAction.getClose();
            if (newClose != null) {
                close = newClose;
            }

            xAction.addTo(c);
        }
        if (quantity > 0) {
            c.getSeries(QUANTITY).add(time(), quantity);
        }
        if (open != null && open.isPositive()) {
            c.getSeries(OPEN).add(time(), open.asValue());
        }
        if (close != null && close.isPositive()) {
            c.getSeries(CLOSE).add(time(), close.asValue());
        }
    }

    public SimpleTimePeriod time() {
        return new SimpleTimePeriod(date, date);
    }

    public double getQuantity() {
        double quantity = 0.0;
        for (Iterator iterator = actions.iterator(); iterator.hasNext();) {
            PriceHistoryRecord rec = (PriceHistoryRecord) iterator.next();
            quantity += rec.getQuantity();
        }
        return quantity;
    }

    public void addTo(TimePeriodValuesCollection c) {
        // Do nothing.
    }
}
