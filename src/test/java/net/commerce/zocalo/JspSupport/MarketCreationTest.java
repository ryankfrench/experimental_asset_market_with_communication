package net.commerce.zocalo.JspSupport;

import net.commerce.zocalo.PersistentTestHelper;
//JJDM import net.commerce.zocalo.service.AllMarkets;
import net.commerce.zocalo.service.Config;
import net.commerce.zocalo.claim.MultiClaim;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.market.*;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.hibernate.HibernateUtil;
import org.hibernate.Transaction;
import org.hibernate.Session;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
// Copyright 2006-2009 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class MarketCreationTest extends PersistentTestHelper {
    private final String dbFilePath = "data/ClaimPurchaseTest";
    private final String userName = "joe";
    private final String userPassword = "joeSecret";
    private final String ownerName = "owner";
    private final String ownerPassword = "masterSecret";
    protected CashBank bank;

    protected void setUp() throws Exception {
        super.setUp();
        manualSetUpForCreate(dbFilePath);
        // JJDM new AllMarkets(dbFilePath, false);
        Config.initPasswdGen();

        Transaction tx = HibernateTestUtil.currentSession().beginTransaction();
        bank = new CashBank("money");
        storeObject(bank);
        SecureUser user = new SecureUser(userName, bank.makeFunds(30000), userPassword, "user@example.com");
        storeObject(user);
        SecureUser owner = new SecureUser(ownerName, bank.makeFunds(300), ownerPassword, "someone@example.com");
        storeObject(owner);
        tx.commit();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testAddBinaryMarket() throws Exception {
        String unfundedMarketName = "rain";
        String fundedMarketName = "reelected";
        assertEquals(null, HibernateTestUtil.getMarketByName(unfundedMarketName));
        assertEquals(userName, HibernateTestUtil.getUserByName(userName).getName());

        Session session = HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();

        HttpServletRequest wrappedRequest = getWrapperWithBoth(userName, userPassword);
        assertAdminAndUserTokens(new HttpServletRequestWrapper(wrappedRequest));
        MarketCreation markets = new MarketCreation();
        markets.processRequest(wrappedRequest, null);  // check for null marketName
        markets.setMarketName(unfundedMarketName);
        markets.processRequest(wrappedRequest, null);
        transaction.commit();

        markets.setMarketName(fundedMarketName);
        markets.setMarketMakerEndowment("100");
        markets.processRequest(wrappedRequest, null);
        transaction.commit();

        manualSetUpForUpdate(dbFilePath);
        Market unfunded = HibernateTestUtil.getMarketByName(unfundedMarketName);
        assertFalse(unfunded.hasMaker());
        assertQEquals(100, unfunded.maxPrice().times(Quantity.ONE));
        Market funded = HibernateTestUtil.getMarketByName(fundedMarketName);
        assertTrue(funded.hasMaker());
        assertQEquals(100, funded.maxPrice().times(Quantity.ONE));
    }

    public void testHtmlInNames() throws Exception {
        String unfundedMarketName = "rain&mdash;";
        String fundedMarketName = "reelected";
        assertEquals(null, HibernateTestUtil.getMarketByName(unfundedMarketName));
        assertEquals(userName, HibernateTestUtil.getUserByName(userName).getName());

        Session session = HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();

        HttpServletRequest wrappedRequest = getWrapperWithBoth(userName, userPassword);
        assertAdminAndUserTokens(new HttpServletRequestWrapper(wrappedRequest));
        MarketCreation markets = new MarketCreation();
        markets.setMarketName(unfundedMarketName);
        markets.processRequest(wrappedRequest, null);
        transaction.commit();

        markets.setMarketName(fundedMarketName);
        markets.setMarketMakerEndowment("100");
        markets.processRequest(wrappedRequest, null);

        assertREMatches("HTML Special.*", HibernateTestUtil.getUserByName(userName).getWarningsHTML());
        transaction.commit();

        manualSetUpForUpdate(dbFilePath);
        Market unfunded = HibernateTestUtil.getMarketByName(unfundedMarketName);
        assertNull(unfunded);
    }

    public void testAddMultiMarket() throws Exception {
        String multiMarketName = "weather";
        String fundedMarketName = "stanley";
        assertEquals(null, HibernateTestUtil.getMarketByName(multiMarketName));

        Session session = HibernateUtil.currentSession();
        Transaction transaction = session.beginTransaction();

        HttpServletRequest wrappedRequest = getWrapperWithBoth(userName, userPassword);
        assertAdminAndUserTokens(new HttpServletRequestWrapper(wrappedRequest));
        MarketCreation markets = new MarketCreation();
        markets.setMarketName(multiMarketName);
        markets.setOutcomes("multi");
        markets.setPositions("rain,sun,fog");
        markets.setMarketMakerEndowment("100");
        markets.processRequest(wrappedRequest, null);

        markets.setMarketName(fundedMarketName);
        markets.setPositions("sharks,avalanche,sabres,thrashers");
        markets.setOutcomes("multi");
        markets.processRequest(wrappedRequest, null);
        transaction.commit();

        manualSetUpForUpdate(dbFilePath);
        MultiMarket weather = HibernateTestUtil.getMultiMarketByName(multiMarketName);
        assertNotNull(weather);
        assertTrue(weather.hasMaker());
        assertEquals(3, weather.getClaim().positions().length);

        MultiMarket stanley = HibernateTestUtil.getMultiMarketByName(fundedMarketName);
        assertNotNull(stanley);
        assertTrue(stanley.hasMaker());
        assertEquals(4, stanley.getClaim().positions().length);
        assertQEquals(Quantity.Q100, stanley.maxPrice());
    }


    public void testCallback() {
        CashBank rootBank = new CashBank("money");
        storeObject(rootBank);
        SecureUser owner = new SecureUser("joe", rootBank.makeFunds(1000), "secure", "someone@example.com");
        storeObject(owner);
        BinaryClaim claim = BinaryClaim.makeClaim("soccer", owner, "who'll win?");
        storeObject(claim);
        Market binaryMkt = BinaryMarket.make(owner, claim, rootBank.noFunds());
        storeObject(binaryMkt);
        Market unaryMkt = UnaryMarket.make(owner, claim, rootBank.noFunds());
        storeObject(unaryMkt);
        String[] positions = {"rain", "snow", "sun"};
        MultiClaim mClaim = MultiClaim.makeClaim("weather", owner, "tomorrow's weather", positions);
        storeObject(mClaim);
        MultiMarket multiMkt = MultiMarket.make(owner, mClaim, rootBank.noFunds());
        storeObject(multiMkt);

        final boolean[] binaryCalled = new boolean[]{false};
        MarketCallback binary = new MarketCallback() {
            public void binaryMarket() { binaryCalled[0] = true; }
            public void unaryMarket() { fail(); }
            public void multiMarket() { fail(); }
        };
        binaryMkt.marketCallBack(binary);
        assert(binaryCalled[0]);

        final boolean[] multiCalled = new boolean[]{false};
        MarketCallback multi = new MarketCallback() {
            public void binaryMarket() { fail(); }
            public void unaryMarket() { fail(); }
            public void multiMarket() { multiCalled[0] = true; }
        };
        multiMkt.marketCallBack(multi);
        assert(multiCalled[0]);

        final boolean[] unaryCalled = new boolean[]{false};
        MarketCallback unary = new MarketCallback() {
            public void binaryMarket() { fail(); }
            public void unaryMarket() { unaryCalled[0] = true; }
            public void multiMarket() { fail(); }
        };
        unaryMkt.marketCallBack(unary);
        assert(unaryCalled[0]);
    }
}
