package net.commerce.zocalo.currency;

// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.user.User;

/** CashBank is the exclusive minter of cash-like currencies.  Anyone with
 access to a CashBank object can endow new accounts with money.  Thus,
 access to it should be treated as a closely-held capability.  */
public class CashBank {
    private long id;
    private CurrencyToken token;

    public CashBank(String name) {
        this.token = new CurrencyToken(name);
    }

    /** @deprecated */
    CashBank() {
    }

    public User makeEndowedUser(String name, Quantity balance) {
        Funds initialBalance = Funds.make(token(), balance);
        return new User(name, initialBalance);
    }

    public Funds makeFunds(Quantity value) {
        return Funds.make(token(), value);
    }

    public Funds makeFunds(int value) {
        return Funds.make(token(), new Quantity(value));
    }

    public Funds noFunds() {
        return Funds.make(token());
    }

    private CurrencyToken token() {
        return getToken();
    }

    /** @deprecated */
    long getId() {
        return id;
    }

    /** @deprecated */
    void setId(long id) {
        this.id = id;
    }

    /** @deprecated */
    CurrencyToken getToken() {
        return token;
    }

    /** @deprecated */
    void setToken(CurrencyToken token) {
        this.token = token;
    }
}
