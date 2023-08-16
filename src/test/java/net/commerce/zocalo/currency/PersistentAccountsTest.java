package net.commerce.zocalo.currency;

import junitx.framework.ArrayAssert;
import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.user.User;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.criterion.Expression;

import java.util.Iterator;
import java.util.List;

// Copyright 2007, 2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class PersistentAccountsTest extends PersistentTestHelper {
    CouponBank issuer;
    private Quantity initialCash = new Quantity("3000.0");

    protected void setUp() throws Exception {
        super.setUp();

        manualSetUpForCreate("data/PersistentAccountsTest");
        Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
        CashBank rootBank = new CashBank("cash");
        HibernateTestUtil.save(rootBank);
        SecureUser mgr = new SecureUser("manager", rootBank.makeFunds(initialCash), "secure", "someone@example.com");
        HibernateTestUtil.save(mgr);
        BinaryClaim claim = BinaryClaim.makeClaim("rain", mgr, "when will it rain?");
        HibernateTestUtil.save(claim);

        SecureUser trader = new SecureUser("trader", rootBank.makeFunds(initialCash), "secure", "someone@example.com");
        HibernateTestUtil.save(trader);

        issuer = CouponBank.makeBank(claim, rootBank.noFunds());
        HibernateTestUtil.save(issuer);

        SecureUser seller = new SecureUser("seller", rootBank.makeFunds(initialCash), "secure", "someone@example.com");
        HibernateTestUtil.save(seller);
        seller.endow(issuer.issueUnpairedCoupons(new Quantity("200"), claim.getYesPosition()));

        tx.commit();
        HibernateTestUtil.currentSession().flush();
        HibernateTestUtil.closeSession();
    }

    public void testPersistentAccounts() throws Exception {
        Quantity fifty = new Quantity("50");
        manualSetUpForUpdate("data/PersistentAccountsTest");

        {
            User trader = getUser("trader");
            User seller = getUser("seller");
            assertEquals(0, initialCash.compareTo(trader.cashOnHand()));

            BinaryClaim rain = getClaim("rain");
            Accounts traderAccount = trader.getAccounts();
            traderAccount.addCoupons(seller.getAccounts().provideCoupons(rain.getYesPosition(), fifty));

            assertEquals(0, fifty.compareTo(traderAccount.couponCount(rain.getYesPosition())));
            HibernateTestUtil.currentSession().beginTransaction().commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            Session session = HibernateTestUtil.currentSession();
            Criteria accountCriteria = session.createCriteria(Accounts.class);
            List accountList = accountCriteria.list();
            Iterator iterator = accountList.iterator();

            Criteria claimsCriteria = session.createCriteria(BinaryClaim.class);
            Claim claimAgain = (Claim) claimsCriteria.list().iterator().next();
            String[] counts = new String[accountList.size()];
            int i = 0;
            while (iterator.hasNext()) {
                Accounts accounts = (Accounts) iterator.next();
                assertEquals(0, initialCash.compareTo(accounts.cashValue()));
                counts[i] = accounts.couponCount(claimAgain).toString();
                i++;
            }
            String[] expected = {"0", "50", "150"};
            ArrayAssert.assertEquals(expected, counts);

            HibernateTestUtil.currentSession().beginTransaction().commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        manualTearDown();
    }

    private BinaryClaim getClaim(String name) {
        Session session = HibernateTestUtil.currentSession();
        Criteria claimCriterion = session.createCriteria(BinaryClaim.class);
        claimCriterion.add(Expression.eq("name", name));
        return (BinaryClaim) claimCriterion.list().iterator().next();
    }
}
