package net.commerce.zocalo.user;

// Copyright 2007, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.orders.Order;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.hibernate.HibernateTestUtil;

public class UserTest extends PersistentTestHelper {
    private final CashBank rootBank = new CashBank("cash");

    protected void setUp() throws Exception {
        HibernateTestUtil.resetSessionFactory();
    }

    public void testChangeAssets() {
        User user = rootBank.makeEndowedUser("chris", q(1000));
        User payee = rootBank.makeEndowedUser("payee", Quantity.ZERO);

        user.receiveCash(payee.getAccounts().provideCash(q(200)));
        assertQEquals(1000.0, user.cashOnHand());
        assertQEquals(0, payee.cashOnHand());

        payee.receiveCash(user.getAccounts().provideCash(q(200)));
        assertQEquals(800.0, user.cashOnHand());
        assertQEquals(200.0, payee.cashOnHand());
    }

    public void testOrders() {
        User user = rootBank.makeEndowedUser("chris", q(1000));
        BinaryClaim claim = BinaryClaim.makeClaim("someClaim", user, "a vanilla claim");
        Position yes = claim.getYesPosition();
        new Order(yes, Price.dollarPrice(0.3), q(50), user);
        assertEquals(1, user.getOrders().size());
        assertQEquals(.3, user.outstandingOrderCost(yes));
        assertQEquals(50, user.outstandingAskQuantity(yes));
    }

    public void testWarnings() {
        User user = rootBank.makeEndowedUser("chris", q(1000));
        final String htmlBreak = User.WARNING_SEPARATOR;
        final String closed = "market Closed.";
        final String better = "order must be better than pre-existing orders.";

        assertEquals("", user.getWarningsHTML());

        user.addWarning(closed);
        assertEquals(closed + htmlBreak, user.getWarningsHTML());
        assertEquals("", user.getWarningsHTML());

        user.addWarning(closed);
        user.addWarning(better);
        assertEquals(closed + htmlBreak + better + htmlBreak, user.getWarningsHTML());
        assertEquals("", user.getWarningsHTML());
    }

    public void testPersistentUsers() throws Exception {
        manualSetUpForCreate("data/PersistentAccountsTest");
        int initialCash = 1000;
        storeObject(rootBank);
        {
            SecureUser user = new SecureUser("someone", rootBank.makeFunds(initialCash), "foo", "someone@example.com");
            storeObject(user);
            assertQEquals(initialCash, user.cashOnHand());
            HibernateTestUtil.currentSession().beginTransaction().commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            User userAgain = getUser("someone");

            assertQEquals(initialCash, userAgain.cashOnHand());
            HibernateTestUtil.currentSession().beginTransaction().commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        manualTearDown();
    }

    public void testPersistentPassword() throws Exception {
        manualSetUpForCreate("data/PersistentAccountsTest");
        int initialCash = 1000;
        storeObject(rootBank);
        {
            SecureUser user = new SecureUser("someone", rootBank.makeFunds(initialCash), "bar", "someoneElse@example.com");
            storeObject(user);
            user.setPassword("jimbo");
            HibernateTestUtil.currentSession().beginTransaction().commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            SecureUser userAgain = (SecureUser) getUser("someone");

            assertTrue(userAgain.verifyPassword("jimbo"));
            assertFalse(userAgain.verifyPassword("jamSandwich"));
            HibernateTestUtil.currentSession().beginTransaction().commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        manualTearDown();
    }
}
