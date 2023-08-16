package net.commerce.zocalo.ajax.events;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import org.apache.log4j.Logger;

/** An Action representing the fact that a user entered a new offer to Sell.  */
public class Ask extends PriceAction {
    /** @deprecated */
    public Ask(String owner, Price price, Quantity quantity, Position pos, Logger logger) {
        super(owner, price, quantity, pos, logger);
        log();
    }

    private Ask(String owner, Price price, Quantity quantity, Position pos) {
        super(owner, price, quantity, pos, PriceAction.getActionLogger());
        log();
    }

    static public Ask newAsk(String owner, Price price, Quantity quantity, Position pos) {
        Ask ask = new Ask(owner, price, quantity, pos);
        HibernateUtil.save(ask);
        return ask;
    }

    Ask() {
        super(PriceAction.getActionLogger());
    }

    protected String actionString() {
        return " added Sell ";
    }
}
