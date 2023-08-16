package net.commerce.zocalo.currency;

// Copyright 2008, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.claim.Position;

/** Tradable assets representing a particular Position on a Claim.  */
public class Coupons extends Currency {
    private Position position;

    /** @deprecated */
    Coupons() {
        // for Hibernate
    }

    ////// FACTORY ///////////////////////////////////////////////////////////////
    private Coupons(CurrencyToken currency, Quantity amount, Position position) {
        super(currency, Quantity.ZERO.max(amount));
        this.position = position;
    }

    static public Coupons newPosition(CurrencyToken currency, Quantity balance, Position position) {
        return new Coupons(currency, balance, position);
    }

    public Coupons makeEmpty() {
        return (Coupons) makeEmptyCurrency();
    }

    protected Currency makeEmpty(CurrencyToken token) {
        return new Coupons(token, Quantity.ZERO, position);
    }

////// QUERIES ///////////////////////////////////////////////////////////////
    public Claim getClaim() {
        return position.getClaim();
    }

    public Position getPosition() {
        return position;
    }

    public String toString() {
        return position.getClaim().getName() + "(" + getTokenName() + "): " + getBalance().printAsDetailedQuantity();
    }

////// TRANSFERRING VALUE ////////////////////////////////////////////////////
    public Coupons provide(Quantity amount) {
        return (Coupons)provideCurrency(amount);
    }

    /** @deprecated */
    private void setPosition(Position position) {
        this.position = position;
    }
}
