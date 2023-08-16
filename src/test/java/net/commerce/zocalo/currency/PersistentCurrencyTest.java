package net.commerce.zocalo.currency;

import junitx.framework.Assert;
import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.user.SecureUser;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.Transaction;

import java.util.Iterator;
import java.util.List;

// Copyright 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class PersistentCurrencyTest extends PersistentTestHelper {
    protected void tearDown() throws Exception {
        super.tearDown();
        HibernateTestUtil.resetSessionFactory();
    }

    protected void setUp() throws Exception {
        super.setUp();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testBasicPersistence() throws Exception {
        manualSetUpForCreate("data/PersistentCurrencyTest");

        CurrencyToken token = new CurrencyToken("ignore");
        storeObject(token);
        SecureUser owner = new SecureUser("owner", Funds.make(token), "secure", "someone@example.com");
        storeObject(owner);
        BinaryClaim claim = BinaryClaim.makeClaim("claim", owner, "random claim");
        storeObject(claim);
        assertNotNull(HibernateTestUtil.currentSession());
        manualTearDown();
    }

    public void testPersistentCoupon() throws Exception {
        manualSetUpForCreate("data/PersistentCurrencyTest");
        {
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            CurrencyToken token = new CurrencyToken("ignore");
            storeObject(token);
            SecureUser owner = new SecureUser("owner", Funds.make(token), "secure", "someone@example.com");
            storeObject(owner);
            BinaryClaim claim = BinaryClaim.makeClaim("claim", owner, "random claim");
            storeObject(claim);
            Coupons rain = Coupons.newPosition(token, Quantity.Q100, claim.getYesPosition());
            storeObject(rain);
            List allCoupons = allCoupons("rain");
            assertEquals(1, allCoupons.size());

            CurrencyToken otherToken = new CurrencyToken("other");
            storeObject(otherToken);
            Coupons imposter = Coupons.newPosition(otherToken, q(50), claim.getYesPosition());

            assertEquals(imposter.getPosition(), rain.getPosition());
            storeObject(imposter);
            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            Session session = HibernateTestUtil.currentSession();
            Criteria couponCriteria = session.createCriteria(Coupons.class);

            List couponsList = couponCriteria.list();
            Coupons[] cList = new Coupons[2];
            Iterator iterator = couponsList.iterator();
            cList[0] = (Coupons)iterator.next();
            cList[1] = (Coupons)iterator.next();

            assertEquals(cList[0].getClaim(), cList[1].getClaim());
        }
        manualTearDown();
    }

    List allCoupons(String name) {
        Session session = HibernateTestUtil.currentSession();
        Criteria c = session.createCriteria(Coupons.class);
        return c.list();
    }

    List allCurrencyTokens(String name) {
        Session session = HibernateTestUtil.currentSession();
        Criteria c = session.createCriteria(CurrencyToken.class);
        return c.list();
    }

    public void testCurrencyToken() throws Exception {
        manualSetUpForCreate("data/PersistentCurrencyTest");
        Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

        {
            CurrencyToken toke = new CurrencyToken("one");
            storeObject(toke);
            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        Session session = HibernateTestUtil.currentSession();
        assertEquals(1, allCurrencyTokens("foo").size());
        assertEquals("one", ((CurrencyToken) session.get(CurrencyToken.class, new Long(1))).getName());

        manualTearDown();
    }

    public void testPersistentCashBanks() throws Exception {
        manualSetUpForCreate("data/PersistentCurrencyTest");

        {
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            CashBank money = new CashBank("money");
            storeObject(money);
            User joe = money.makeEndowedUser("joe", q(10));
            assertQEquals(10, joe.cashOnHand());
            Funds five = joe.getAccounts().provideCash(q(5));
            storeObject(five);
            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            CashBank bankAgain = getBank();

            Session session = HibernateTestUtil.currentSession();
            Criteria fundsCriteria = session.createCriteria(Funds.class);
            Funds fiveAgain = (Funds) fundsCriteria.list().iterator().next();
            assertQEquals(5, fiveAgain.getBalance());

            User jerry = bankAgain.makeEndowedUser("jerry", q(11));
            jerry.receiveCash(fiveAgain);
            assertQEquals(16, jerry.cashOnHand());
            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            Session session = HibernateTestUtil.currentSession();
            Criteria fundsCriteria = session.createCriteria(Funds.class);
            Funds emptyNow = (Funds) fundsCriteria.list().iterator().next();
            assertQEquals(0, emptyNow.getBalance());
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }
        manualTearDown();
    }

    public void testPersistentCouponBank() throws Exception {
        manualSetUpForCreate("data/PersistentCurrencyTest");
        {
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            CashBank bank = new CashBank("cash");
            storeObject(bank);
            SecureUser owner = new SecureUser("owner", bank.makeFunds(1000), "secure", "someone@example.com");

            storeObject(owner);
            tx.commit();
            Claim truth = BinaryClaim.makeClaim("truth", owner, "veritas eterna");
            storeObject(truth);
            tx.commit();
            CouponBank couponSource = CouponBank.makeBank(truth, bank.noFunds());
            storeObject(couponSource);
            tx.commit();

            assertQEquals(0, couponSource.getSetsMinted());
            Coupons[] couponSets = couponSource.printNewCouponSets(q(3), owner.getAccounts().provideCash(q(300)), Quantity.Q100);
            for (int i = 0; i < couponSets.length; i++) {
                Coupons coupons = couponSets[i];
                storeObject(coupons);
                tx.commit();
            }
            assertQEquals(3, couponSource.getSetsMinted());
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        { /* can I retrieve the same CouponBank? */
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            Session session = HibernateTestUtil.currentSession();
            Criteria coupBankCriteria = session.createCriteria(CouponBank.class);
            CouponBank coupBankAgain = (CouponBank) coupBankCriteria.list().iterator().next();
            assertQEquals(3, coupBankAgain.getSetsMinted());

            Criteria claimCriteria = session.createCriteria(BinaryClaim.class);
            Claim truthAgain = (Claim) claimCriteria.list().iterator().next();
            Criteria cashCriteria = session.createCriteria(Funds.class);
            Funds cashAgain = (Funds) cashCriteria.list().iterator().next();

            coupBankAgain.printNewCouponSets(q(2), cashAgain.provide(q(500)), Quantity.Q100);
            assertQEquals(5, coupBankAgain.getSetsMinted());

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        { // Can I distinguish Positions in different banks?
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            Session session = HibernateTestUtil.currentSession();
            Criteria bankCriteria = session.createCriteria(CouponBank.class);
            CouponBank bankAgain = (CouponBank) bankCriteria.list().iterator().next();

            User ownerAgain = getUser("owner");

            Claim peace = BinaryClaim.makeClaim("peace", ownerAgain, "war soon?");
            storeObject(peace);
            CouponBank peaceCouponSource = CouponBank.makeBank(peace, ownerAgain.getAccounts().provideCash(Quantity.ZERO));
            storeObject(peaceCouponSource);

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            Session session = HibernateTestUtil.currentSession();
            Criteria bankCriteria = session.createCriteria(CouponBank.class);
            CouponBank bankAgain = (CouponBank) bankCriteria.list().iterator().next();
            assertQEquals(5, bankAgain.getSetsMinted());

            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }
        manualTearDown();
    }

    public void testComparePersistentCouponBanks() throws Exception {
        manualSetUpForCreate("data/PersistentCurrencyTest");
        {
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            CashBank bank = new CashBank("cash");
            storeObject(bank);
            SecureUser owner = new SecureUser("owner", bank.makeFunds(1000), "secure", "someone@example.com");
            storeObject(owner);
            tx.commit();
            Claim truth = BinaryClaim.makeClaim("truth", owner, "veritas eterna");
            storeObject(truth);
            tx.commit();
            Funds empty = bank.noFunds();
            storeObject(empty);
            tx.commit();
            CouponBank couponSource = CouponBank.makeBank(truth, empty);
            storeObject(couponSource);
            tx.commit();

            Claim peace = BinaryClaim.makeClaim("peace", owner, "war soon?");
            storeObject(peace);
            tx.commit();
            CouponBank peaceCouponSource = CouponBank.makeBank(peace, owner.getAccounts().provideCash(Quantity.ZERO));
            storeObject(peaceCouponSource);

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        { // Can I distinguish Positions in different banks?
            Session session = HibernateTestUtil.currentSession();
            Criteria bankCriteria = session.createCriteria(CouponBank.class);
            List list = bankCriteria.list();
            Coupons[] coupons = new Coupons[2];

            Criteria cashCriteria = session.createCriteria(Funds.class);
            Funds cashAgain = (Funds) cashCriteria.list().iterator().next();

            int i = 0;
            for (Iterator iterator = list.iterator() ; iterator.hasNext(); i++) {
                CouponBank bank = (CouponBank) iterator.next();

                coupons[i]= bank.printNewCouponSets(q(3), cashAgain.provide(q(300)), Quantity.Q100)[i];
                storeObject(coupons[i]);
            }

            Assert.assertNotEquals(coupons[0].getClaim(), coupons[1].getClaim());
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }
        manualTearDown();
    }
}
