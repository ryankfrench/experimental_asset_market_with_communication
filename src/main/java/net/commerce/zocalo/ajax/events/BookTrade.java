package net.commerce.zocalo.ajax.events;

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.hibernate.HibernateUtil;
import org.apache.log4j.Logger;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Action representing the fact that a user traded with the order book.  */
public class BookTrade extends Trade {
    /** @deprecated TEST ONLY */
    public BookTrade(String trader, Price price, Quantity quantity, Position pos, Logger logger) {
        super(trader, price, quantity, pos, logger);
        log();
    }

    private BookTrade(String trader, Price priceToBuyer, Quantity quantityExchanged, Position pos) {
        super(trader, priceToBuyer, quantityExchanged, pos, PriceAction.getActionLogger());
        log();
    }

    static public BookTrade newBookTrade(String trader, Price price, Quantity quantity, Position pos) {
        BookTrade trade = new BookTrade(trader, price, quantity, pos);
        HibernateUtil.save(trade);
        return trade;
    }

    /** @deprecated */
    public BookTrade() {
        super();
    }

    protected String actionString() {
        return " accepted offer ";
    }

    public boolean isBuy() {
        return getPos().isBuy(getQuantity().isPositive());
    }
}
