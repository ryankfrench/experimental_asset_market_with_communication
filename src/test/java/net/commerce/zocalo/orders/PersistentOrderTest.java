package net.commerce.zocalo.orders;

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.user.SecureUser;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;

// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class PersistentOrderTest extends PersistentTestHelper {
    protected void setUp() throws Exception {
        super.setUp();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testPersistentOrders() throws Exception {
        manualSetUpForCreate("data/PersistentOrdersTest");
        CashBank rootBank = new CashBank("money");
        storeObject(rootBank);
        int initialCash = 1000;
        SecureUser someone = new SecureUser("someone", rootBank.makeFunds(initialCash), "secure", "someone@example.com");
        storeObject(someone);
        {
            BinaryClaim rain = BinaryClaim.makeClaim("rain", someone, "rain before Wednesday.");
            storeObject(rain);
            Position yes = rain.getYesPosition();
            Order order = new Order(yes, Price.dollarPrice(.3), q(20), someone);
            storeObject(order);
            assertQEquals(.3, order.price());
            HibernateTestUtil.currentSession().beginTransaction().commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            Session session = HibernateTestUtil.currentSession();
            Transaction transaction = HibernateTestUtil.currentSession().beginTransaction();
            Criteria orderCriteria = session.createCriteria(Order.class);
            Order orderAgain = (Order) orderCriteria.list().iterator().next();

            assertQEquals(20, orderAgain.quantity());
            transaction.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        manualTearDown();
    }
}
