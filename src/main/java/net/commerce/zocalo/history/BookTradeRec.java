package net.commerce.zocalo.history;

import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.currency.Price;
import org.jfree.data.time.TimePeriodValuesCollection;
// Copyright 2006, 2007 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** BookTradeRec represents a single trade between a new order and an existing
    BookOrder.  It may be combined with other BookTradeRecs and MarketTradeRecs
    into a single TransactionRec. */
public class BookTradeRec extends TradeRec {
    private double quantity;
    private Order order;

    public BookTradeRec(SecureUser taker, double quantity, Order order) {
        super(taker);
        this.quantity = quantity;
        this.order = order;
    }

    public void addTo(TimePeriodValuesCollection c) {
        // Do nothing; it all happens in the containing transaction.
        // use sum of quantities, first open, last close.
    }

    public double getQuantity() {
        return quantity;
    }

    public Price getOpen() {
        return order.naturalPrice();
    }

    public Price getClose() {
        return order.naturalPrice();
    }
}
