package net.commerce.zocalo.currency;

// Copyright 2007, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.JunitHelper;
import net.commerce.zocalo.logging.Log4JHelper;

public class FundsTest extends JunitHelper {
    private CurrencyToken cash;
    private Funds money;

    protected void setUp() throws Exception {
        cash = new CurrencyToken("cash");
        money = Funds.make(cash, q(1000));
        HibernateTestUtil.resetSessionFactory();
        Log4JHelper.getInstance();
    }

    public void testBasicFundsCreation() {
        assertQEquals(1000.0, money.getBalance());
    }

    public void testCurrenciesNotEqual() {
        Funds moolah = Funds.make(new CurrencyToken("cash"), q(1000));
        assertQEquals(1000.0, moolah.getBalance());
        assertNotSame(money, moolah);
        money.transfer(q(200), moolah);
        assertQEquals(1000, moolah.getBalance());
        assertQEquals(1000, money.getBalance());
    }

    public void testSimpleTransfer() {
        Funds money2 = Funds.make(cash, q(1000));
        assertTrue(money.sameCurrency(money2));
        money.transfer(q(200), money2);
        assertQEquals(1200.0, money2.getBalance());
        assertQEquals(800.0, money.getBalance());
    }

    public void testCurrenciesCantExchange() {
        Funds moolah = Funds.make(new CurrencyToken("cash"), q(1000));
        assertQEquals(1000.0, moolah.getBalance());
        assertNotSame(money, moolah);
        money.transfer(q(200), moolah);
        assertQEquals(1000.0, moolah.getBalance());
        assertQEquals(1000.0, money.getBalance());
    }

    public void testConserveFunds() {
        Funds insufficient = money.provide(q(1250));
        assertTrue(insufficient.getBalance().isZero());
        assertQEquals(1000, money.getBalance());

        Funds accumulating = money.provide(q(200));
        while (money.getBalance().compareTo(Quantity.Q100) >= 0) {
            money.transfer(q(200), accumulating);
            assertQEquals(q(1000), money.getBalance().plus(accumulating.getBalance()));
        }
        assertQEquals(0, money.getBalance());
        assertQEquals(1000, accumulating.getBalance());
    }

    public void testNoNegativeAmounts() {
        assertQEquals(0.0, money.provide(q(30).negate()).getBalance());
        assertQEquals(0.0, Funds.make(cash, q(30).negate()).getBalance());
        Funds empty = money.makeEmpty();
        money.transfer(q(30).negate(), empty);
        assertQEquals(0.0, empty.getBalance());
    }
}
