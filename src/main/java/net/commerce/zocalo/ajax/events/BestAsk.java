package net.commerce.zocalo.ajax.events;

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import org.apache.log4j.Logger;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** An Action representing the fact that there's a new best offer to sell.  */
public class BestAsk extends PriceAction {
    public BestAsk(String owner, Price price, Quantity quantity, Position pos, Logger logger) {
        super(owner, price, quantity, pos, logger);
        log();
    }

    private BestAsk(String owner, Price price, Quantity quantity, Position pos) {
        super(owner, price, quantity, pos, PriceAction.getActionLogger());
        log();
    }

    static public BestAsk newBest(String owner, Price price, Quantity quantity, Position pos) {
        BestAsk bestAsk = new BestAsk(owner, price, quantity, pos);
        HibernateUtil.save(bestAsk);
        return bestAsk;
    }

    /** @deprecated */
    public BestAsk() {
        super(PriceAction.getActionLogger());
    }

    protected String actionString() {
        return " added new best ask price ";
    }
}
