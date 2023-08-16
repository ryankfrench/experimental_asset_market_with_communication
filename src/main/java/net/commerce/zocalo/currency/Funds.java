package net.commerce.zocalo.currency;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** Currency subclass representing money. */
public class Funds extends Currency {
    /** @deprecated */
    Funds() {
        // for Hibernate
    }

    public String toString() {
        return getBalance().printAsDollars();
    }

    ////// FACTORY ///////////////////////////////////////////////////////////////
    private Funds(CurrencyToken token, Quantity initialBalance) {
        super(token, Quantity.ZERO.max(initialBalance));
    }

    static public Funds make(CurrencyToken token) {
        return new Funds(token, Quantity.ZERO);
    }

    static public Funds make(CurrencyToken token, Quantity amount) {
        return new Funds(token, amount);
    }

    public Funds makeEmpty() {
        return (Funds) makeEmptyCurrency();
    }

    protected Currency makeEmpty(CurrencyToken token) {
        return make(token, Quantity.ZERO);
    }

////// TRANSFERRING VALUE ////////////////////////////////////////////////////
    public Funds provide(Quantity amount) {
        return (Funds)provideCurrency(amount);
    }
}
