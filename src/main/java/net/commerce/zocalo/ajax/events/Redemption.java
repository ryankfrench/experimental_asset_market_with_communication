package net.commerce.zocalo.ajax.events;

import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Action representing the fact that coupons were redeemed when the claim was settled.  */
public class Redemption extends PriceAction {
    public Redemption(String name, Price price, Quantity quantity, Position pos) {
        super(name, price, quantity, pos, PriceAction.getActionLogger());
        log();
    }

    static public Redemption newRedemption(String owner, Price price, Quantity quantity, Position pos) {
        Redemption redemption = new Redemption(owner, price, quantity, pos);
        HibernateUtil.save(redemption);
        return redemption;
    }

    /** @deprecated */
    public Redemption() {
        super(PriceAction.getActionLogger());
    }

    protected String actionString() {
        return " redeemed coupons ";
    }

    public String toLogString() {
        Position position = getPos();
        return getGID() + getOwner()
                + " redeemed " + getQuantity().printAsQuantity() + " coupons "
                + " for " + getPosition(position)
                + " at " + getPrice().toString();
    }
}
