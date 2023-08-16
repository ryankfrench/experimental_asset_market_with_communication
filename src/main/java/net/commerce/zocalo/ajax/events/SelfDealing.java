package net.commerce.zocalo.ajax.events;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;

/** A user accepted his own offer.  Won't be represented as a trade, but will
    cause the standing offer to be removed from the book. */
public class SelfDealing extends PriceAction {
    private SelfDealing(String user, Price price, Quantity quantity, Position pos) {
        super(user, price, quantity, pos, PriceAction.getActionLogger());
        log();
    }

    static public SelfDealing newSelfDealing(String owner, Price price, Quantity quantity, Position pos) {
        SelfDealing selfDeal = new SelfDealing(owner, price, quantity, pos);
        HibernateUtil.save(selfDeal);
        return selfDeal;
    }

    /** @deprecated */
    public SelfDealing() {
        super(PriceAction.getActionLogger());
    }

    protected String actionString() {
        return " cancelled order ";
    }
}
