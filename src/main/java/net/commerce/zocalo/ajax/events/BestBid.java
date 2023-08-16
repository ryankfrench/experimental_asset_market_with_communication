package net.commerce.zocalo.ajax.events;

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import org.apache.log4j.Logger;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** An Action representing the fact that there's a new best bid price in the market */
public class BestBid extends PriceAction {
    protected BestBid(Logger logger) {
        super(logger);
        log();
    }

    protected BestBid(String owner, Price price, Quantity quantity, Position pos, Logger logger) {
        super(owner, price, quantity, pos, logger);
        log();
    }

    static public BestBid newBest(String user, Price price, Quantity quantity, Position pos) {
        BestBid bestBid = new BestBid(user, price, quantity, pos, Logger.getLogger(PriceAction.class));
        HibernateUtil.save(bestBid);
        return bestBid;
    }

    protected String actionString() {
        return " added new best bid price ";
    }

    /** @deprecated */
    public BestBid() {
        super(PriceAction.getActionLogger());
    }
}
