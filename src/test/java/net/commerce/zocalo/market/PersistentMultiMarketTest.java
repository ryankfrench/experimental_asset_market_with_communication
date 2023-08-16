package net.commerce.zocalo.market;

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.claim.MultiClaim;
import net.commerce.zocalo.claim.Claim;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.currency.Probability;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import org.hibernate.Transaction;

import java.util.Dictionary;

// Copyright 2006, 2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class PersistentMultiMarketTest extends PersistentTestHelper {
    private String weatherName = "weather";
    private String rainName = "rain";
    private String snowName = "snow";
    private String sunName = "sun";
    private String bankName;
    private static final Price MAX_PRICE = Price.ONE_DOLLAR;
    private Quantity HundredThousand = new Quantity(100000);

    protected void setUp() throws Exception {
        super.setUp();
        manualSetUpForCreate("data/PersistentMultiMarketTest");
        Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
        bankName = "money";
        CashBank rootBank = new CashBank(bankName);
        storeObject(rootBank);
        SecureUser owner = new SecureUser("joe", rootBank.makeFunds(100000), "secure", "someone@example.com");
        storeObject(owner);
        String[] positions = {rainName, snowName, sunName};
        MultiClaim mClaim = MultiClaim.makeClaim(weatherName, owner, "tomorrow's weather", positions);
        storeObject(mClaim);
        MultiMarket m = MultiMarket.make(owner, mClaim, rootBank.noFunds());
        storeObject(m);
        MultiMarketMaker mm = m.makeMarketMaker(new Quantity(30000), owner);
        storeObject(mm);

        tx.commit();
        HibernateTestUtil.currentSession().flush();
        HibernateTestUtil.closeSession();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testClosingMarket() throws Exception {
        manualSetUpForUpdate("data/PersistentMultiMarketTest");
        HibernateTestUtil.currentSession().beginTransaction();
        MultiMarket m = HibernateTestUtil.getMultiMarketByName(weatherName);
        Claim weatherClaim = HibernateTestUtil.getClaimByName(weatherName);
        Position rain = weatherClaim.lookupPosition(rainName);
        Position sun = weatherClaim.lookupPosition(sunName);

        SecureUser ace = makeUser("ace", 100000);
        SecureUser nick = makeUser("nick", 100000);
        buy(m, sun, ace, "60", 20);
        buy(m, rain, nick, "60", 20);
        Quantity aceCashBefore = ace.getAccounts().cashValue();
        Quantity nickCashBefore = nick.getAccounts().cashValue();

        assertQEquals(20, ace.getAccounts().couponCount(sun));
        assertQEquals(20, nick.getAccounts().couponCount(rain));
        assertTrue(HundredThousand.compareTo(nickCashBefore) > 0);
        assertTrue(HundredThousand.compareTo(aceCashBefore) > 0);
        Dictionary<Position,Probability> finalProbs = m.getMaker().currentProbabilities(rain);
        assertTrue(m.isOpen());
        m.decideClaimAndRecord(rain);

        assertFalse(m.isOpen());
        assertTrue(nickCashBefore.compareTo(nick.getAccounts().cashValue()) < 0);
        assertTrue(ace.getAccounts().couponCount(sun).isZero());
        assertTrue(nick.getAccounts().couponCount(rain).isZero());
        assertTrue(m.getMaker().cashInAccount().isZero());
        assertBalancedTrades(nick, m, 100000);
        assertBalancedTrades(ace, m, 100000);
        assertEquals("rain", m.describeOutcome());
        assertQEquals(finalProbs.get(rain), m.currentProbability(rain));
        assertQEquals(finalProbs.get(sun), m.currentProbability(sun));
    }

    private void buy(MultiMarket m, Position sun, SecureUser ace, String price, double q) throws DuplicateOrderException {
        limitOrder(m, sun, price, q, ace);
    }

    private SecureUser makeUser(String name, double endowment) {
        SecureUser user = new SecureUser(name, getBank().makeFunds(q(endowment)), "secure", name + "@example.com");
        storeObject(user);
        return user;
    }

    public void testRedemptions() throws Exception {
        manualSetUpForUpdate("data/PersistentMultiMarketTest");
        HibernateTestUtil.currentSession().beginTransaction();
        SecureUser owner = HibernateTestUtil.getUserByName("joe");
        MultiMarket m = HibernateTestUtil.getMultiMarketByName(weatherName);
        MarketMaker mm = m.getMaker();
        Claim weatherClaim = HibernateTestUtil.getClaimByName(weatherName);
        Position rain = weatherClaim.lookupPosition(rainName);
        Position snow = weatherClaim.lookupPosition(snowName);
        Position sun = weatherClaim.lookupPosition(sunName);


        int buyerStart = 100000;
        SecureUser buyer = makeUser("buyer", buyerStart);
        buyUpToQuantity(mm, buyer, rain, "0.45", 1000);
        assertTrue(q(.45).compareTo(mm.currentProbability(rain)) >= 0);

        buyUpToQuantity(mm, buyer, rain, "0.45", 10000);
        assertQEquals(0.45, mm.currentProbability(rain));

        SecureUser ace = makeUser("ace", 500000);
        buyUpToQuantity(mm, ace, rain, "0.50", 30000);
        assertQEquals(0.5, mm.currentProbability(rain));
        buyUpToQuantity(mm, ace, sun, "0.50", 30000);

        Quantity aceRain = ace.couponCount(rain);
        Quantity aceSun = ace.couponCount(sun);
        Quantity aceSnow = ace.couponCount(snow);

        Quantity buyerRain = buyer.couponCount(rain);
        Quantity buyerSun =  buyer.couponCount(sun);
        Quantity buyerSnow = buyer.couponCount(snow);

        Quantity mmRain = mm.accounts().couponCount(rain);
        Quantity mmSun = mm.accounts().couponCount(sun);
        Quantity mmSnow = mm.accounts().couponCount(snow);

        assertQEquals(aceRain.plus(buyerRain).plus(mmRain), aceSun.plus(buyerSun.plus(mmSun)));
        assertQEquals(aceRain.plus(buyerRain.plus(mmRain)), aceSnow.plus(buyerSnow.plus(mmSnow)));
        assertTrue(buyerSun.isZero());
        assertTrue(buyerSnow.isZero());
        assertTrue(aceSnow.isZero());
        assertQEquals(70000, owner.cashOnHand());

        m.decideClaimAndRecord(sun);

        assertTrue(owner.cashOnHand().isPositive());

        Quantity ownerLoss = owner.cashOnHand().minus(HundredThousand);
        Quantity buyerLoss = HundredThousand.minus(buyer.cashOnHand());
        Quantity aceGain = ace.cashOnHand().minus(new Quantity(500000));
        assertQApproaches(ownerLoss, buyerLoss.minus(aceGain));
        assertBalancedTrades(ace, m, 500000);
        assertBalancedTrades(buyer, m, buyerStart);
    }

    public void testThinVsThickMarkets() throws Exception {
        Quantity samQuantity;

        {
            manualSetUpForUpdate("data/PersistentMultiMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
            CashBank rootBank = HibernateTestUtil.getOrMakePersistentRootBank(bankName);
            SecureUser transientOwner = new SecureUser("jerry", rootBank.makeFunds(500000), "insecure", "jerry@example.com");
            storeObject(transientOwner);

            String[] tOutcomes = {"quick", "fast", "young"};
            MultiClaim transientClaim = MultiClaim.makeClaim("transient", transientOwner, "Transient", tOutcomes);
            storeObject(transientClaim);
            MultiMarket tMarket = MultiMarket.make(transientOwner, transientClaim, rootBank.noFunds());
            storeObject(tMarket);
            MarketMaker tMaker = tMarket.makeMarketMaker(new Quantity(10000), transientOwner);
            storeObject(tMaker);
            User sam = makeUser("sam", 100000);
            storeObject(sam);

            Position quick = transientClaim.lookupPosition("quick");
            buyUpToQuantity(tMaker, sam, quick, ".99", 1000);
            samQuantity = sam.couponCount(quick);

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        String sharksString = "Sharks";
        String[] teams = {sharksString, "Detroit", "Other"};
        String hockeyString = "hockey";

        {
            manualSetUpForUpdate("data/PersistentMultiMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            CashBank rootBank = HibernateTestUtil.getOrMakePersistentRootBank(bankName);
            SecureUser newOwner = new SecureUser("jesse", rootBank.makeFunds(500000), "insecure", "jesse@example.com");
            storeObject(newOwner);
            MultiClaim hockeyClaim = MultiClaim.makeClaim(hockeyString, newOwner, "Division Champ", teams);
            storeObject(hockeyClaim);
            MultiMarket hockey = MultiMarket.make(newOwner, hockeyClaim, rootBank.noFunds());
            storeObject(hockey);
            MarketMaker hMaker = hockey.makeMarketMaker(new Quantity(300000), newOwner); // 10 times weather's maker
            storeObject(hMaker);

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }

        {
            manualSetUpForUpdate("data/PersistentMultiMarketTest");
            Transaction tx = HibernateTestUtil.currentSession().beginTransaction();

            Position sharks = HibernateTestUtil.getClaimByName(hockeyString).lookupPosition(sharksString);
            MultiMarket hockeyMarket = HibernateTestUtil.getMultiMarketByName(hockeyString);
            MarketMaker hockeyMaker = hockeyMarket.getMaker();
            User bill = makeUser("bill", 100000);
            buyUpToQuantity(hockeyMaker, bill, sharks, "0.99", 1000);
            Quantity billQuantity = bill.couponCount(sharks);

            MultiMarket weather = HibernateTestUtil.getMultiMarketByName(weatherName);
            MarketMaker weatherMaker = weather.getMaker();
            Position rain = HibernateTestUtil.getClaimByName(weatherName).lookupPosition(rainName);
            User bob = makeUser("bob", 100000);

            buyUpToQuantity(weatherMaker, bob, rain, ".99", 1000);
            Quantity bobQuantity = bob.couponCount(rain);
            assertTrue(bobQuantity.isPositive());
            assertTrue(billQuantity.isPositive());
            Price sharksP = hockeyMarket.currentPrice(sharks);
            Price rainP = weather.currentPrice(rain);
            assertTrue(rainP.compareTo(sharksP) > 0);

            tx.commit();
            HibernateTestUtil.currentSession().flush();
            HibernateTestUtil.closeSession();
        }
    }

    private void buyUpToQuantity(MarketMaker tMaker, User sam, Position quick, String probability, int quant) {
        tMaker.buyUpToQuantity(quick, new Probability(probability), new Quantity(quant), sam);
    }
}
