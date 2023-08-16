package net.commerce.zocalo.ajax.events;

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.ajax.dispatch.PriceActionAppender;
import org.apache.log4j.Logger;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Action representing the fact that a user traded with the order book.  */
public class LimitTrade extends Trade {
    /** @deprecated TEST ONLY */
    public LimitTrade(String trader, Price price, Quantity quantity, Position pos, Logger logger) {
        super(trader, price, quantity, pos, logger);
        log();
    }

    private LimitTrade(String trader, Price priceToBuyer, Quantity quantityExchanged, Position pos) {
        super(trader, priceToBuyer, quantityExchanged, pos, PriceAction.getActionLogger());
        log();
    }

    static public LimitTrade newLimitTrade(String trader, Price price, Quantity quantity, Position pos) {
        LimitTrade trade = new LimitTrade(trader, price, quantity, pos);
        HibernateUtil.save(trade);
        return trade;
    }

    /** @deprecated */
    public LimitTrade() {
        super();
    }

    protected String actionString() {
        return " had limit offer accepted ";
    }

    public boolean isBuy() {
        return getPos().isBuy(getQuantity().isPositive());
    }
}
