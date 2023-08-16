package net.commerce.zocalo.service;

// Copyright 2007-2009 Chris Hibbert.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.JunitHelper;
import net.commerce.zocalo.JspSupport.MarketDisplay;
import net.commerce.zocalo.JspSupport.MockHttpServletResponse;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.user.User;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.servlet.http.HttpServletRequest;

public class AllMarketsTest extends PersistentTestHelper {
    private final String mgrName = "joe";
    private final String dbFilePath = "data/AllMarketTest";

    protected void setUp() throws Exception {
        super.setUp();
        Log4JHelper.getInstance();
        HibernateTestUtil.resetSessionFactory();
        Config.initPasswdGen();
        manualSetUpForCreate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        CashBank rootBank = new CashBank("passwdTest");
        session.save(rootBank);
        SecureUser mgr = new SecureUser(mgrName, rootBank.makeFunds(1000), "foo", mgrName + "@example.com");
        session.save(mgr);
        transaction.commit();
    }

    public void testOneTableLine() throws Exception {
        StringBuffer resultBuf = new StringBuffer();
        resultBuf.append("<table>");
        MarketDisplay screen = new MarketDisplay();

        manualSetUpForUpdate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        session.beginTransaction();
        SecureUser joe = HibernateTestUtil.getUserByName(mgrName);
        assertEquals(mgrName, joe.getName());
        MarketOwner.newBinaryMarket("bean", joe);
        MarketOwner.newBinaryMarket("weather", joe);
        MarketOwner.newBinaryMarket("grass", joe);
        screen = new MarketDisplay();
        screen.setUserName("joe");
        HttpServletRequest userWrapper = getUserWrapper(joe.getName(), "foo");
        screen.processRequest(userWrapper, new MockHttpServletResponse());

        String result = screen.getMarketNamesTable();
        String expected = ".*<table.*((grass|weather|bean)'.*){3}.*</table>";
        JunitHelper.assertREMatches(expected, result);
        HibernateTestUtil.resetSessionFactory();
    }

    public void testBestOffersTable() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        assertEquals(mgrName, HibernateTestUtil.getUserByName(mgrName).getName());
        MarketOwner.newMarket("bean", mgrName);
        transaction.commit();

        manualSetUpForUpdate(dbFilePath);
        String userName = "sue";
        createUserInSession(userName, "passwd2");

        manualSetUpForUpdate(dbFilePath);
        HibernateTestUtil.currentSession().beginTransaction();
        User sue = HibernateTestUtil.getUserByName(userName);
        assertEquals(userName, HibernateTestUtil.getUserByName(userName).getName());
        BinaryMarket beanMarket = (BinaryMarket) HibernateTestUtil.getMarketByName("bean");
        assertEquals("bean", HibernateTestUtil.getMarketByName("bean").getName());
        Position yes = beanMarket.getBinaryClaim().getYesPosition();
        limitOrder(beanMarket, yes, "30", (double)10, sue);
        limitOrder(beanMarket, yes, "40", (double)8, sue);
        limitOrder(beanMarket, yes, "45", (double)15, sue);
        limitOrder(beanMarket, yes, "37", (double)20, sue);

        Position no = beanMarket.getBinaryClaim().getNoPosition();
        limitOrder(beanMarket, no, "30", (double)9, sue);
        limitOrder(beanMarket, no, "20", (double)11, sue);
        limitOrder(beanMarket, no, "35", (double)13, sue);
        limitOrder(beanMarket, no, "43", (double)5, sue);

        StringBuffer buf = new StringBuffer();
        MarketDisplay.printOrdersTable(HibernateTestUtil.getMarketByName("bean"), buf, sue, "");
        String result = buf.toString();
        String expected = ".*80.*70.*65.*30.*";
        JunitHelper.assertREMatches(expected, result);
        assertTrue(result.indexOf("80") < result.indexOf("11"));
        assertTrue(result.indexOf("70") < result.indexOf("9"));
        assertTrue(result.indexOf("65") < result.indexOf("13"));
        assertTrue(result.indexOf("57") < result.indexOf("45"));

        HibernateTestUtil.resetSessionFactory();
    }

    public void testNarrowRangeMarket() throws Exception {
        StringBuffer resultBuf = new StringBuffer();
        resultBuf.append("<table>");
        MarketDisplay screen = new MarketDisplay();

        manualSetUpForUpdate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        session.beginTransaction();
        SecureUser joe = HibernateTestUtil.getUserByName(mgrName);
        assertEquals(mgrName, joe.getName());
        MarketOwner.newBinaryMarket("bean", joe, q(50), 0);
        screen = new MarketDisplay();
        screen.setUserName(joe.getName());
        HttpServletRequest userWrapper = getUserWrapper(joe.getName(), "foo");
        screen.processRequest(userWrapper, new MockHttpServletResponse());
        String result = screen.getMarketNamesTable();
        String expected = ".*<table.*bean.*50.*</table>";
        assertREMatches(expected, result);
        HibernateTestUtil.resetSessionFactory();
    }
}
