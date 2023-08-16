package net.commerce.zocalo.history;

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.JspSupport.TradeHistory;
import net.commerce.zocalo.service.Config;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import org.hibernate.Transaction;
import org.hibernate.Session;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

// Copyright 2006-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class PersistentHistoryTest extends PersistentTestHelper {
    private final String password = "secure";

    protected void setUp() throws Exception {
        super.setUp();
        manualSetUpForCreate("data/PersistentHistoryTest");
        Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
        CashBank bank = new CashBank("money");
        storeObject(bank);
        SecureUser owner = new SecureUser("owner", bank.makeFunds(30000), password, "someone@example.com");
        storeObject(owner);
        BinaryClaim weather = BinaryClaim.makeClaim("rain", owner, "will it rain tomorrow?");
        storeObject(weather);
        BinaryMarket market = BinaryMarket.make(owner, weather, bank.noFunds());
        market.makeMarketMaker(owner, q(20000));
        storeObject(market);
        SecureUser joe = new SecureUser("joe", bank.makeFunds(30000), password, "joe@example.com");
        storeObject(joe);
        SecureUser alice = new SecureUser("alice", bank.makeFunds(30000), password, "alice@example.com");
        storeObject(alice);
        Config.initPasswdGen();

        marketOrder(market, weather.getYesPosition(), "55", 10, joe);
        marketOrder(market, weather.getNoPosition(), "55", 20, alice);
        marketOrder(market, weather.getYesPosition(), "55", 10, joe);
        tx.commit();
        HibernateTestUtil.currentSession().flush();
        HibernateTestUtil.closeSession();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testListClaimsWithHistory() throws Exception {
        manualSetUpForUpdate("data/PersistentHistoryTest");
        TradeHistory history = new TradeHistory();

        SecureUser alice = getUser("alice");
        HttpServletRequest aliceRequest = getRequestWithCookie(alice.getName(), password);
        history.processRequest(aliceRequest, null);
        List aliceTrades = HibernateUtil.getTrades(alice);
        assertEquals(1, aliceTrades.size());

        SecureUser joe = getUser("joe");
        HttpServletRequest joeRequest = getRequestWithCookie(joe.getName(), password);
        history.processRequest(joeRequest, null);
        List joeTrades = HibernateUtil.getTrades(joe);
        assertEquals(2, joeTrades.size());
        manualTearDown();
    }

    public void testPageDisplay() throws Exception {
        manualSetUpForUpdate("data/PersistentHistoryTest");
        TradeHistory history = new TradeHistory();
        SecureUser joe = getUser("joe");
        HttpServletRequest joeRequest = getRequestWithCookie(joe.getName(), password);

        Session session = HibernateUtil.currentSession();
        history.processRequest(joeRequest, null);
        String[] strings = history.tradeTable().split("</tr>");
        assertEquals(4, strings.length);  // Header + two rows + 1

        assertTrue(session.isOpen());
        manualTearDown();
    }

    public void testRetrieveTradeHistory() throws Exception {
        manualSetUpForUpdate("data/PersistentHistoryTest");
        TradeHistory history = new TradeHistory();

        Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
        CashBank bank = HibernateUtil.getOrMakePersistentRootBank("money");
        SecureUser owner = HibernateUtil.getUserByName("owner");
        BinaryClaim hockey = BinaryClaim.makeClaim("hockey", owner, "Will the home team win?");
        storeObject(hockey);
        BinaryMarket market = BinaryMarket.make(owner, hockey, bank.noFunds());
        market.makeMarketMaker(owner, q(50));
        storeObject(market);

        SecureUser alice = getUser("alice");
        SecureUser joe = getUser("joe");

        Position yes = hockey.getYesPosition();
        Position no = hockey.getNoPosition();
        marketOrder(market, yes, "55", 10, joe);
        marketOrder(market, no, "55", 10, alice);
        marketOrder(market, no, "65", 5, alice);
        marketOrder(market, yes, "65", 5, joe);
        tx.commit();
        HibernateTestUtil.currentSession().flush();
        HibernateTestUtil.closeSession();

        manualSetUpForUpdate("data/PersistentHistoryTest");
        HttpServletRequest aliceRequest = getRequestWithCookie(alice.getName(), password);
        history.processRequest(aliceRequest, null);
        List allAliceTrades = HibernateUtil.getTrades(alice);
        assertEquals(3, allAliceTrades.size());
        List aliceHockeyTrades = HibernateUtil.getTrades(alice, market);
        assertEquals(2, aliceHockeyTrades.size());

        HttpServletRequest joeRequest = getRequestWithCookie(joe.getName(), password);
        history.processRequest(joeRequest, null);
        List allJoeTrades = HibernateUtil.getTrades(joe);
        assertEquals(4, allJoeTrades.size());
        List joeTrades = HibernateUtil.getTrades(joe, market);
        assertEquals(2, joeTrades.size());
        CostAccounter aliceRainCount = new CostAccounter(alice, HibernateTestUtil.getMarketByName("rain"));
        Quantity aliceRainCost = aliceRainCount.getBuyCost("no");
        assertBinaryTradesBalance(market, alice, q(30000).minus(aliceRainCost));
        CostAccounter joeRainCount = new CostAccounter(joe, HibernateTestUtil.getMarketByName("rain"));
        Quantity joeRainCost = joeRainCount.getBuyCost("yes");
        assertBinaryTradesBalance(market, joe, q(30000).minus(joeRainCost));

        manualTearDown();
    }

    public void testCostAccounter() throws Exception {
        manualSetUpForUpdate("data/PersistentHistoryTest");

        HibernateTestUtil.currentSession().beginTransaction();
        CashBank bank = HibernateUtil.getOrMakePersistentRootBank("money");
        SecureUser owner = HibernateUtil.getUserByName("owner");
        BinaryClaim hockey = BinaryClaim.makeClaim("hockey", owner, "Will the home team win?");
        storeObject(hockey);
        BinaryMarket market = BinaryMarket.make(owner, hockey, bank.noFunds());
        market.makeMarketMaker(owner, q(50));
        storeObject(market);

        SecureUser alice = getUser("alice");
        SecureUser joe = getUser("joe");
        final Quantity aliceStart = alice.cashOnHand();
        final Quantity joeStart = joe.cashOnHand();
        final Position yes = hockey.getYesPosition();
        final Position no = yes.opposite();

        assertBinaryTradesBalance(market, alice, aliceStart);
        assertBinaryTradesBalance(market, joe, joeStart);
        marketOrder(market, yes, "55", 10, joe);
        assertBinaryTradesBalance(market, joe, joeStart);
        marketOrder(market, no, "55", 10,  alice);
        assertBinaryTradesBalance(market, alice, aliceStart);
        marketOrder(market, no, "65", 5, alice);
        marketOrder(market, yes, "65", 5, joe);
        assertBinaryTradesBalance(market, alice, aliceStart);
        assertBinaryTradesBalance(market, joe, joeStart);
    }
}
