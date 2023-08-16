package net.commerce.zocalo.currency;

import net.commerce.zocalo.hibernate.HibernateUtil;

// Copyright 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

/** The basic trading unit.  Funds (representing money) and Coupons
    (representing other assets) are derived from Currency.  */
public abstract class Currency {
    static final private double epsilon = 0.000001;
    private CurrencyToken token; // FINAL
    private Quantity balance;
    private Long id;
    final static public double CURRENCY_SCALE = 100.0;

////// FACTORY ///////////////////////////////////////////////////////////////

    /** @deprecated */
    Currency() {
        // for Hibernate
    }

    protected Currency(CurrencyToken toke, int bal) {
        token = toke;
        balance = new Quantity(bal);
    }

    protected Currency(CurrencyToken toke, Quantity bal) {
        token = toke;
        balance = bal;
    }

    protected Currency makeEmptyCurrency() {
        return makeEmpty(token());
    }

    abstract protected Currency makeEmpty(CurrencyToken token);

////// QUERIES ///////////////////////////////////////////////////////////////
    public String getTokenName() {
        return token().getName();
    }

    public boolean negligible() {
        return getBalance().isNegligible();
    }

    public boolean sameCurrency(Currency otherCurrency) {
        return token().equals(otherCurrency.token());
    }

    ////// TRANSFERRING VALUE ////////////////////////////////////////////////////
    Currency provideCurrency(Quantity amount) {
        if (! amount.isPositive()) {
            return makeEmpty(token());
        }
        Currency extracted = makeEmpty(token());
        transfer(amount, extracted);
        return extracted;
    }

    public Quantity transfer(Currency recipient) {
        return transfer(getBalance(), recipient);
    }

    public Quantity transfer(Quantity value, Currency recipient) {
        if (! value.isPositive()) {
            return Quantity.ZERO;
        }
        if (getBalance().compareTo(value) >= 0 && sameCurrency(recipient)) {
            decreaseBalance(value);
            recipient.increaseBalance(value);
            return value;
        }
        
        return Quantity.ZERO;
    }

    void increaseBalance(Quantity value) {
        balance(getBalance().plus(value));
    }

    void decreaseBalance(Quantity value) {
        balance(getBalance().minus(value));
    }

////// HIBERNATE /////////////////////////////////////////////////////////////

    /** @deprecated */
    private Long getId() {
        return id;
    }

    /** @deprecated */
    private void setId(Long id) {
        this.id = id;
    }

    /** @deprecated should only be used in token() and by Hibernate */
    CurrencyToken getToken() {
        return token;
    }

    private CurrencyToken token() {
        return getToken();
    }

    /** @deprecated */
    void setToken(CurrencyToken token) {
        this.token = token;
    }

    public Quantity getBalance() {
        return balance;
    }

    private void balance(Quantity b) {
        setBalance(b);
    }

    /** @deprecated */
    void setBalance(Quantity balance) {
        this.balance = balance;
    }
}
