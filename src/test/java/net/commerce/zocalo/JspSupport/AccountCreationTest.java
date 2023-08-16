package net.commerce.zocalo.JspSupport;
// Copyright 2007, 2008 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.logging.Log4JHelper;
import net.commerce.zocalo.service.MarketOwner;
import net.commerce.zocalo.service.Config;
import net.commerce.zocalo.service.ServerUtil;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.hibernate.HibernateUtil;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.user.UnconfirmedUser;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class AccountCreationTest extends PersistentTestHelper {
    String dbFilePath = "data/AccountCreationTest";

    protected void setUp() throws Exception {
        super.setUp();
        Config.initializeConfiguration(ServerUtil.readConfigFile());
        Log4JHelper.getInstance();
        HibernateTestUtil.resetSessionFactory();
        Config.initPasswdGen();
        manualSetUpForCreate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        CashBank rootBank = new CashBank("passwdTest");
        session.save(rootBank);
        transaction.commit();
    }

    protected void tearDown() throws Exception {
        super.tearDown();
        HibernateTestUtil.currentSession().flush();
        HibernateTestUtil.closeSession();
    }

    public void testCreateAndConfirmAccount() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        session.beginTransaction();
        AccountCreationScreen page = new AccountCreationScreen();
        page.setUserName("zocalo");
        page.setEmailAddress("zocalo@sf.net");
        page.processRequest(wrappedMockRequest((Cookie)null), new MockHttpServletResponse());
    }

    public void testDontCreateAccount() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        CashBank rootBank = new CashBank("accountTest");
        session.save(rootBank);
        // JJDM new AllMarkets(dbFilePath, false);
        String userName = "zocalo";
        SecureUser user = new SecureUser(userName, rootBank.makeFunds(1000), "ffo", userName + "@example.com");
        session.save(user);
        transaction.commit();

        manualSetUpForUpdate(dbFilePath);
        HibernateTestUtil.currentSession().beginTransaction();
        AccountCreationScreen page = new AccountCreationScreen();

        page.setUserName(userName);
        page.setEmailAddress("zocalo@sf.net");
        page.processRequest(wrappedMockRequest((Cookie)null), new MockHttpServletResponse());
        assertEquals(AccountCreationScreen.ACCOUNT_NOT_AVAILABLE + "zocalo<br>", page.getWarning());

        page.setUserName(userName + "2");
        page.setEmailAddress("zocalo@sf.net");
        page.setPassword2("BC37 83a");
        page.processRequest(wrappedMockRequest((Cookie)null), new MockHttpServletResponse());
        assertEquals("passwords don't match." + "<br>", page.getWarning());
        assertNull(HibernateUtil.getUnconfirmedUserByName(userName + "2", HibernateUtil.currentSession()));

        page.setUserName(userName + "2");
        page.setEmailAddress("zocalo@sf.net");
        page.processRequest(wrappedMockRequest((Cookie)null), new MockHttpServletResponse());
        assertEquals(AccountCreationScreen.PASSWORD_WARNING + "<br>", page.getWarning());
        assertNull(HibernateUtil.getUnconfirmedUserByName(userName + "2", HibernateUtil.currentSession()));

        page.setUserName(userName + "2");
        page.setEmailAddress("zocalo@sf.net");
        page.setPassword("BC37 83a");
        page.processRequest(wrappedMockRequest((Cookie)null), new MockHttpServletResponse());
        assertEquals(AccountCreationScreen.PASSWORD_WARNING + "<br>", page.getWarning());
        assertNull(HibernateUtil.getUnconfirmedUserByName(userName + "2", HibernateUtil.currentSession()));

        page.setUserName(userName + "3");
        page.setEmailAddress("zocalo@sf.net");
        page.setPassword("BC37a83a");
        page.processRequest(wrappedMockRequest((Cookie)null, "http://example.com/webpage/Welcome.jsp"), new MockHttpServletResponse());
        assertEquals(AccountCreationScreen.LOOK_FOR_CONFIRMATION + "<br>", page.getWarning());

        page.setUserName(userName + "3");
        page.setEmailAddress("zocalo2@example.com");
        page.setPassword("B!37a83a");
        page.processRequest(wrappedMockRequest((Cookie)null), new MockHttpServletResponse());
        assertEquals(AccountCreationScreen.ACCOUNT_NOT_AVAILABLE + userName + "3" + ".<br>", page.getWarning());
    }

    public void testConfirmRegistration() throws Exception {
        manualSetUpForUpdate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        CashBank rootBank = new CashBank("accountTest");
        session.save(rootBank);
        // JJDM new AllMarkets(dbFilePath, false);
        transaction.commit();
        String requestURL = "http://foo.example.com/createAccount/";
        MarketOwner.createUnconfirmedUserAndNotifyOwner("nick", "foobear", "foo@example.com", requestURL);

        assertNull(HibernateTestUtil.getUserByName("nick", session));
        UnconfirmedUser user = HibernateTestUtil.getUnconfirmedUserByName("nick", session);
        String token = user.getConfirmationToken();
        user.confirm(token);
        assertNull(HibernateTestUtil.getUnconfirmedUserByName("nick", session));
        assertNotNull(HibernateTestUtil.getUserByName("nick", session));
    }

    public void testConfirmRegistrationViaWeb() throws Exception {
        String userName = "nick";
        manualSetUpForUpdate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        CashBank rootBank = new CashBank("accountTest");
        session.save(rootBank);
        // JJDM new AllMarkets(dbFilePath, false);
        transaction.commit();
        String requestURL = "http://foo.example.com/createAccount/";
        MarketOwner.createUnconfirmedUserAndNotifyOwner(userName, "foobear", "foo@example.com", requestURL);

        assertNull(HibernateTestUtil.getUserByName(userName, session));
        UnconfirmedUser user = HibernateTestUtil.getUnconfirmedUserByName(userName, session);
        String token = user.getConfirmationToken();

        AccountCreationScreen page = new AccountCreationScreen();

        page.setUserName(userName);
        page.setConfirmation(token);
        HttpServletRequest request = wrappedMockRequest((Cookie) null);
        page.processRequest(request, new MockHttpServletResponse());

        assertNull(HibernateTestUtil.getUnconfirmedUserByName(userName, session));
        assertNotNull(HibernateTestUtil.getUserByName(userName, session));
        assertEquals(WelcomeScreen.WELCOME_JSP, page.getRequestURL(request));
    }
}
