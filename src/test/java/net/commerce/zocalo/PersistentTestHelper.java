package net.commerce.zocalo;

import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.user.User;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.user.Registry;
import net.commerce.zocalo.market.*;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.claim.Position;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.JspSupport.MockHttpServletRequest;
import net.commerce.zocalo.service.MarketOwner;
import net.commerce.zocalo.service.Config;
import net.commerce.zocalo.service.BayeuxSingleton;
import net.commerce.zocalo.history.CostAccounter;
import net.commerce.zocalo.ajax.dispatch.*;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.Criteria;
import org.hibernate.criterion.Expression;

import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.Cookie;
import java.io.*;

import junitx.framework.ArrayAssert;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public abstract class PersistentTestHelper extends JunitHelper {
    protected MockBayeuxChannel historyChannel;
    protected MockBayeuxChannel livingChannel;
    protected MockBayeuxChannel changeChannel;
    protected MockBayeux mockBayeux;

    static protected void manualSetUpForUpdate(String dbFilePath) throws Exception {
        Log4JHelper.getInstance();
        HibernateTestUtil.initializeSessionFactory(dbFilePath, false);
    }

    protected void manualSetUpForCreate(String dbFilePath) throws Exception {
        Log4JHelper.getInstance();
        HibernateTestUtil.initializeSessionFactory(dbFilePath, true);
    }

    protected void manualTearDown() throws Exception {
        HibernateTestUtil.closeSession();
        HibernateTestUtil.resetSessionFactory();
    }

    protected void storeObject(Object o) {
        Session session = HibernateTestUtil.currentSession();
        Transaction tx = session.beginTransaction();

        session.save(o);
        tx.commit();
    }

    protected void setUp() throws Exception {
        super.setUp();
        setupBayeux();
        Log4JHelper.getInstance();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        mockBayeux = null;     // nulling these improves GC performance during testing.
        historyChannel = null;
        livingChannel = null;
        changeChannel = null;
    }

    /** modelled on example from http://javaalmanac.com/, which says
      "All the code examples from the book are made available here for you to copy and paste into your programs." */
    static public void copyFile(String src, String dest) throws IOException {
        if (! new File(src).exists()) {
            return;
        }
        InputStream in = new FileInputStream(src);
        OutputStream out = new FileOutputStream(dest);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
        in.close();
        out.close();
    }

    public SecureUser getUser(String name) {
        Session session = HibernateTestUtil.currentSession();
        Criteria userCriteria = session.createCriteria(User.class);
        userCriteria.add(Expression.eq("name", name));
        return (SecureUser) userCriteria.uniqueResult();
    }

    public Market getMarket(long theMarketId) {
        Session session = HibernateTestUtil.currentSession();
        Criteria marketCriteria = session.createCriteria(Market.class);
        marketCriteria.add(Expression.eq("id", new Long(theMarketId)));
        return (Market) marketCriteria.uniqueResult();
    }

    static public Market getMarket() {
        Session session = HibernateTestUtil.currentSession();
        Criteria marketCriteria = session.createCriteria(Market.class);
        return (Market) marketCriteria.uniqueResult();
    }

    public CashBank getBank() {
        Session session = HibernateTestUtil.currentSession();
        Criteria bankCriteria = session.createCriteria(CashBank.class);
        return (CashBank) bankCriteria.uniqueResult();
    }

    public BinaryClaim getClaim() {
        Session session = HibernateTestUtil.currentSession();
        Criteria claimCriteria = session.createCriteria(BinaryClaim.class);
        return (BinaryClaim) claimCriteria.uniqueResult();
    }

    public HttpServletRequestWrapper registerUserAndGetRequestWithCookieInSession(String userName, String password, String dbFilePath) throws Exception {
        createUserInSession(userName, password);
        manualSetUpForUpdate(dbFilePath);
        HttpServletRequest request = getRequestWithCookie(userName, password);
        assertEquals(1, request.getCookies().length);

        MockHttpServletRequest mock = new MockHttpServletRequest();
        HttpServletRequestWrapper wrapper = new HttpServletRequestWrapper(mock);
        mock.setCookie(request.getCookies()[0]);
        return wrapper;
    }

    public HttpServletRequest getRequestWithCookie(String userName, String password) {
        Cookie cookie = MarketOwner.login(userName, password);
        return wrappedMockRequest(cookie);
    }

    protected void createUserInSession(String userName, String password) {
        Session session = HibernateTestUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        MarketOwner.createUser(userName, 1000, password, "default@example.com", session);
        transaction.commit();
        session.close();
    }

    static protected HttpServletRequest wrappedMockRequest(Cookie cookie) {
        MockHttpServletRequest mock = new MockHttpServletRequest();
        mock.setCookie(cookie);
        return new HttpServletRequestWrapper(mock);
    }

    static protected HttpServletRequest wrappedMockRequest(Cookie cookie, String nextURL) {
        MockHttpServletRequest mock = new MockHttpServletRequest();
        mock.setCookie(cookie);
        mock.setNextUrl(nextURL);
        return new HttpServletRequestWrapper(mock);
    }

    static protected HttpServletRequest wrappedMockRequest(Cookie[] cookies) {
        MockHttpServletRequest mock = new MockHttpServletRequest();
        for (int i = 0; i < cookies.length; i++) {
            Cookie cooky = cookies[i];
            mock.setCookie(cooky);
        }
        return new HttpServletRequestWrapper(mock);
    }

    static public HttpServletRequest getWrapperWithBoth(String userName, String userPassword) {
        return wrappedMockRequest(
                new Cookie[] { getAdminCookie(), getUserCookie(userName, userPassword)});
    }

    static public HttpServletRequest getUserWrapper(String userName, String userPassword) {
        return wrappedMockRequest(getUserCookie(userName, userPassword));
    }

    static public Cookie getUserCookie(String userName, String userPassword) {
        return MarketOwner.login(userName, userPassword);
    }

    static public Cookie getAdminCookie() {
        String adminToken = Config.matchAdminPassword("unsafe");
        return new Cookie(Registry.ADMIN_TOKEN, adminToken);
    }

    protected void setupBayeux() {
        mockBayeux = new MockBayeux();
        BayeuxSingleton.getInstance().setBayeux(mockBayeux);
        historyChannel = (MockBayeuxChannel)mockBayeux.getChannel(TradeEventDispatcher.TRADE_EVENT_TOPIC_SUFFIX, true);
        livingChannel = (MockBayeuxChannel)mockBayeux.getChannel(BidUpdateDispatcher.BID_UPTDATE_TOPIC_URI, true);
        changeChannel = (MockBayeuxChannel)mockBayeux.getChannel(TransitionDispatcher.TRANSITION_TOPIC_URI, true);
    }

    static public void assertAdminTokenOnly(HttpServletRequestWrapper request) {
        if (1 == request.getCookies().length) {
            assertEquals(Registry.ADMIN_TOKEN, request.getCookies()[0].getName());
        } else {
            boolean adminTokenFound = false;
            Cookie[] cookies = request.getCookies();
            for (int i = 0; i < cookies.length; i++) {
                if (request.getCookies()[i].getName().equals(Registry.ADMIN_TOKEN)) {
                    adminTokenFound = true;
                } else {
                    assertNull("null registration token expected", cookies[i].getValue());
                }
            }
            assertTrue(adminTokenFound);
        }
    }

    static public void assertAdminAndUserTokens(HttpServletRequestWrapper request) {
        assertEquals(2, request.getCookies().length);
        ArrayAssert.assertEquivalenceArrays(
                new String[] { Registry.ADMIN_TOKEN, Registry.REGISTRATION },
                new String[] { request.getCookies()[0].getName(), request.getCookies()[1].getName() });
        assertNotNull(request.getCookies()[0].getValue());
        assertNotNull(request.getCookies()[1].getValue());
    }

    static public void assertUserTokenOnly(HttpServletRequestWrapper request) {
        if (1 == request.getCookies().length) {
            assertEquals(Registry.REGISTRATION, request.getCookies()[0].getName());
        } else {
            assertEquals(2, request.getCookies().length);
            if (request.getCookies()[0].getName().equals(Registry.ADMIN_TOKEN)) {
                assertNull(request.getCookies()[0].getValue());
                assertEquals(Registry.REGISTRATION, request.getCookies()[1].getName());
                assertNotNull(request.getCookies()[1].getValue());
            } else if (request.getCookies()[1].getName().equals(Registry.ADMIN_TOKEN)) {
                assertEquals(Registry.REGISTRATION, request.getCookies()[0].getName());
                assertNotNull(request.getCookies()[0].getValue());
                assertNull(request.getCookies()[1].getValue());
            } else {
                assertFalse("expected one Registration Token and one Null Admin Token", true);
            }
        }
    }

    static public void assertBinaryTradesBalance(BinaryMarket market, SecureUser user, Quantity start) {
        Position yes = market.getBinaryClaim().getYesPosition();
        String YES = "yes";
        String NO = "no";

        final CostAccounter accounter = new CostAccounter(user, market);
        final Quantity yesQ = accounter.getQuantity(YES);
        final Quantity noQ = accounter.getQuantity(NO);
        assertQEquals(yesQ.minus(noQ), user.couponCount(yes.getClaim()));
        final Quantity redeemValue = accounter.getRedemptionValue(YES).plus(accounter.getRedemptionValue(NO));
        assertCostsBalance(user, market, start.plus(redeemValue), accounter);
    }

    static public void assertBalancedTrades(SecureUser user, Market market, Quantity startCash) {
        CostAccounter accts = new CostAccounter(user, market);
        final Position[] positions = market.getClaim().positions();
        Quantity balance = startCash;
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            Quantity quantity = accts.getQuantity(position.getName());
            Quantity cost = (accts.getCost(position.getName())).minus(accts.getRedemptionValue(position.getName()));
            Quantity coupons = user.couponCount(position);
            String comment = position.getName();
            assertQEquals(comment, quantity, coupons);
            balance = balance.minus(cost);
        }
        assertQEquals(balance, user.cashOnHand());
    }

    static public void assertCostsBalance(SecureUser user, Market market, Quantity startCash, CostAccounter accts) {
        final Position[] positions = market.getClaim().positions();
        Quantity balance = startCash;
        for (int i = 0; i < positions.length; i++) {
            Position position = positions[i];
            Quantity cost = accts.getCost(position.getName());
            balance = balance.minus(cost);
        }
        assertQEquals(balance, user.cashOnHand());
    }
}
