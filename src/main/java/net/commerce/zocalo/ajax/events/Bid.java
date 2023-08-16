package net.commerce.zocalo.ajax.events;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import org.apache.log4j.Logger;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;

/** An Action representing the fact that a user entered a new offer to buy.  */
public class Bid extends PriceAction {
    /** @deprecated */
    public Bid(String owner, Price price, Quantity quantity, Position pos, Logger logger) {
        super(owner, price, quantity, pos, logger);
        log();
    }

    static public Bid newBid(String user, Price price, Quantity quantity, Position pos) {
        Bid bid = new Bid(user, price, quantity, pos);
        HibernateUtil.save(bid);
        return bid;
    }

    private Bid(String user, Price price, Quantity quantity, Position pos) {
        super(user, price, quantity, pos, PriceAction.getActionLogger());
        log();
    }

    /** @deprecated */
    public Bid() {
        super(PriceAction.getActionLogger());
    }

    protected String actionString() {
        return " added Buy ";
    }
}
