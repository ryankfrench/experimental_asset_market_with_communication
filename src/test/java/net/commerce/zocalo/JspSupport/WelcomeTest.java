package net.commerce.zocalo.JspSupport;

import net.commerce.zocalo.PersistentTestHelper;
//JJDM import net.commerce.zocalo.service.AllMarkets;
import net.commerce.zocalo.service.Config;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.user.Registry;
import net.commerce.zocalo.user.SecureUser;
import org.hibernate.Session;
import org.hibernate.Transaction;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

// Copyright 2007-2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class WelcomeTest extends PersistentTestHelper {
    private CashBank rootBank;
    private final String dbFilePath = "data/PersistentWelcomeTest";

    protected void setUp() throws Exception {
        HibernateTestUtil.resetSessionFactory();
        rootBank = new CashBank("passwdTest");
        // JJDM new AllMarkets(dbFilePath, false);
    }

    protected void tearDown() throws Exception {
        manualTearDown();
    }

    public void testNoAccount() {
        WelcomeScreen screen = new WelcomeScreen();
        screen.setUserName("joe");
        screen.setPassword("");
        screen.processRequest(wrappedMockRequest((Cookie) null), new MockHttpServletResponse());
        assertEquals(null, screen.getUser());
        assertEquals(WelcomeScreen.USERNAME_AND_PASSWORD_WARNING + "<br>", screen.getWarning());
    }

    public void testBlankUserName() {
        WelcomeScreen screen = new WelcomeScreen();
        screen.setUserName("");
        screen.setPassword("");
        screen.processRequest(wrappedMockRequest((Cookie) null), new MockHttpServletResponse());
        assertEquals(null, screen.getUser());
        assertEquals(WelcomeScreen.USERNAME_AND_PASSWORD_WARNING + "<br>", screen.getWarning());
    }

    public void testPasswordPassFail() throws Exception {
        String userName = "joe";
        String password = "glub";

        manualSetUpForCreate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        session.save(rootBank);
        SecureUser user = new SecureUser(userName, rootBank.makeFunds(1000), password, userName + "@example.com");
        session.save(user);
        transaction.commit();
        manualTearDown();

        manualSetUpForUpdate(dbFilePath);
        assertEquals(userName, HibernateTestUtil.getUserByName(userName).getName());
        WelcomeScreen screen = new WelcomeScreen();
        screen.setUserName(userName);
        HttpServletRequest badRequest = wrappedMockRequest((Cookie)null);
        assertNoLoginTokens(badRequest);
        screen.setPassword("foo");
        screen.processRequest(badRequest, new MockHttpServletResponse());
        assertEquals(null, screen.getUser());
        assertFalse(screen.loginSucceeded());
        assertEquals(WelcomeScreen.UNKNOWN_ACCOUNT_OR_PASSWORD_WARNING + "<br>", screen.getWarning());
        assertNoLoginTokens(badRequest);

        screen.setUserName("unknown");
        screen.setPassword("unrecognized");
        screen.processRequest(badRequest, new MockHttpServletResponse());
        assertFalse(screen.loginSucceeded());
        assertEquals(WelcomeScreen.UNKNOWN_ACCOUNT_OR_PASSWORD_WARNING + "<br>", screen.getWarning());
        assertNoLoginTokens(badRequest);
        manualTearDown();

        manualSetUpForUpdate(dbFilePath);
        screen.setUserName(userName);
        assertEquals(userName, HibernateTestUtil.getUserByName(userName).getName());
        screen.setPassword(password);
        HttpServletRequest goodRequest = wrappedMockRequest((Cookie)null);
        screen.processRequest(goodRequest, new MockHttpServletResponse());
        assertEquals(userName, screen.getUserName());
        assertTrue(screen.loginSucceeded());
        assertEquals("", screen.getWarning());
        assertEquals(1, goodRequest.getCookies().length);

        screen.setUserName("unknown");
        screen.setPassword("unrecognized");
        screen.processRequest(goodRequest, new MockHttpServletResponse());
        assertFalse(screen.loginSucceeded());
        assertEquals(WelcomeScreen.UNKNOWN_ACCOUNT_OR_PASSWORD_WARNING + "<br>", screen.getWarning());
        assertNoLoginTokens(goodRequest);

        HibernateTestUtil.resetSessionFactory();
    }

    public void testGoodToken() throws Exception {
        manualSetUpForCreate(dbFilePath);
        String userName = "joe";
        String password = "glub";
        Session session = HibernateTestUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        session.save(rootBank);
        SecureUser user = new SecureUser(userName, rootBank.makeFunds(300), password, "user@example.com");
        storeObject(user);
        transaction.commit();
        manualTearDown();

        manualSetUpForUpdate(dbFilePath);
        WelcomeScreen screen = new WelcomeScreen();
        screen.setUserName("");
        screen.setPassword("");
        screen.processRequest(getUserWrapper(userName, password), new MockHttpServletResponse());

        assertEquals(WelcomeScreen.USERNAME_AND_PASSWORD_WARNING + "<br>", screen.getWarning());
        assertFalse(screen.loginSucceeded());

        manualTearDown();
    }

    public void testBadToken() throws Exception {
        String userName = "joe";
        String password = "glub";
        manualSetUpForCreate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        session.save(rootBank);
        SecureUser user = new SecureUser(userName, rootBank.makeFunds(300), password, "user@example.com");
        storeObject(user);
        transaction.commit();
        manualTearDown();

        manualSetUpForUpdate(dbFilePath);

        WelcomeScreen firstScreen = new WelcomeScreen();
        firstScreen.setUserName("");
        firstScreen.setPassword("blue");
        firstScreen.processRequest(getUserWrapper(userName, password), new MockHttpServletResponse());

        assertEquals(WelcomeScreen.UNKNOWN_ACCOUNT_OR_PASSWORD_WARNING + "<br>", firstScreen.getWarning());
        assertFalse(firstScreen.loginSucceeded());

        WelcomeScreen secondScreen = new WelcomeScreen();
        secondScreen.setUserName("blue");
        secondScreen.setPassword("");
        secondScreen.processRequest(getUserWrapper(userName, password), new MockHttpServletResponse());

        assertEquals(WelcomeScreen.USERNAME_AND_PASSWORD_WARNING + "<br>", secondScreen.getWarning());
        assertFalse(secondScreen.loginSucceeded());

        manualTearDown();
    }

    public void testMockRequest() {
        MockHttpServletRequest mock = new MockHttpServletRequest();
        HttpServletRequestWrapper outerRequest = new HttpServletRequestWrapper(mock);
        assertEquals(0, mock.getCookies().length);
        assertEquals(0, outerRequest.getCookies().length);

        mock.setCookie(new Cookie("foo", "bar"));
        assertNotNull(outerRequest.getCookies());

        assertEquals("bar", outerRequest.getCookies()[0].getValue());
        assertEquals("foo", outerRequest.getCookies()[0].getName());
    }

    public void testWelcomeAdmin() throws Exception {
        manualSetUpForCreate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        session.save(rootBank);
        transaction.commit();
        manualTearDown();

        manualSetUpForUpdate(dbFilePath);

        String userName = "joe";
        String goodPassword = "glub";

        // Create User
        createUserInSession(userName, goodPassword);

        // login user as admin
        manualSetUpForUpdate(dbFilePath);
        WelcomeScreen screen = new WelcomeScreen();
        screen.setUserName(userName);
        screen.setPassword("unsafe");
        MockHttpServletRequest mock = new MockHttpServletRequest();
        HttpServletRequestWrapper request = new HttpServletRequestWrapper(mock);
        screen.processRequest(request, new MockHttpServletResponse());
        manualTearDown();

        manualSetUpForUpdate(dbFilePath);
        assertEquals(null, screen.getUser());
        assertFalse(screen.loginSucceeded());
        assertAdminTokenOnly(request);

        // login user retaining admin access
        screen.setUserName(userName);
        screen.setPassword(goodPassword);
        screen.processRequest(request, new MockHttpServletResponse());
        manualTearDown();

        manualSetUpForUpdate(dbFilePath);
        assertEquals(userName, screen.getUserName());
        assertTrue(screen.loginSucceeded());

        // create another user account
        String newbieName = "newbie";
        screen.setUserName(newbieName);
        screen.setPassword("nextpassword");
        screen.processRequest(request, new MockHttpServletResponse());
        manualTearDown();

        manualSetUpForUpdate(dbFilePath);

        assertEquals(newbieName, screen.getUserName());
        assertFalse(screen.loginSucceeded());
        assertAdminTokenOnly(request);

        manualTearDown();
    }

    public void testDropAdminAuth() throws Exception {
        String userName = "joe";
        String goodPassword = "glub";
        String otherUserName = "jill";
        String adminPasswd = "unsafe";

        manualSetUpForCreate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        Transaction transaction = session.beginTransaction();
        session.save(rootBank);
        SecureUser user = new SecureUser(userName, rootBank.makeFunds(1000), goodPassword, userName + "@example.com");
        session.save(user);
        transaction.commit();
        manualTearDown();

        // login existing user
        manualSetUpForUpdate(dbFilePath);
        MockHttpServletRequest mock = new MockHttpServletRequest();
        HttpServletRequestWrapper request = new HttpServletRequestWrapper(mock);
        WelcomeScreen screen = new WelcomeScreen();
        screen.setUserName(userName);
        screen.setPassword(goodPassword);
        screen.processRequest(request, new MockHttpServletResponse());
        assertEquals(userName, screen.getUserName());
        assertTrue(screen.loginSucceeded());
        assertUserTokenOnly(request);
        manualTearDown();

        // login user as admin
        manualSetUpForUpdate(dbFilePath);
        screen.setPassword(adminPasswd);
        screen.processRequest(request, new MockHttpServletResponse());
        assertEquals(userName, screen.getUserName());
        assertFalse(screen.loginSucceeded());
        assertAdminAndUserTokens(request);
        manualTearDown();

        // Drop Admin Authority
        manualSetUpForUpdate(dbFilePath);
        screen.setPassword("");
        screen.processRequest(request, new MockHttpServletResponse());
        assertEquals(userName, screen.getUserName());
        assertFalse(screen.loginSucceeded());
        assertUserTokenOnly(request);
        assertNull(screen.getRequestURL(request));
        assertTrue(screen.getUserAsWarnable().getWarningsHTML().indexOf("removed Admin Authorization") >= 0);
        manualTearDown();

        // login as admin only (with other name)
        manualSetUpForUpdate(dbFilePath);
        screen.setUserName(otherUserName);
        screen.setPassword(adminPasswd);
        screen.processRequest(request, new MockHttpServletResponse());
        assertEquals(otherUserName, screen.getUserName());
        assertFalse(screen.loginSucceeded());
        assertNull(screen.getRequestURL(request));
        assertAdminTokenOnly(request);
        manualTearDown();

        // drop admin auth again
        manualSetUpForUpdate(dbFilePath);
        screen.setUserName(otherUserName);
        screen.setPassword("");
        screen.processRequest(request, new MockHttpServletResponse());
        assertEquals(null, screen.getUser());
        assertFalse(screen.loginSucceeded());
        assertNull(screen.getRequestURL(request));
        assertNoLoginTokens(request);
        manualTearDown();

        // login as only admin (with first name)
        manualSetUpForUpdate(dbFilePath);
        screen.setUserName(userName);
        screen.setPassword(adminPasswd);
        screen.processRequest(request, new MockHttpServletResponse());
        assertEquals(userName, screen.getUserName());
        assertFalse(screen.loginSucceeded());
        assertNull(screen.getRequestURL(request));
        assertAdminTokenOnly(request);
        manualTearDown();

        // login as first user retaining admin
        manualSetUpForUpdate(dbFilePath);
        screen.setUserName(userName);
        screen.setPassword(goodPassword);
        screen.processRequest(request, new MockHttpServletResponse());
        assertEquals(userName, screen.getUserName());
        assertTrue(screen.loginSucceeded());
        assertEquals("account.jsp", screen.getRequestURL(request));
        assertAdminAndUserTokens(request);

        manualTearDown();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testOddUserNames() throws Exception {
        manualSetUpForCreate(dbFilePath);
        String adminToken = Config.matchAdminPassword("unsafe");
        Cookie cookie = new Cookie(Registry.ADMIN_TOKEN, adminToken);
        HttpServletRequest request = wrappedMockRequest(cookie);

        WelcomeScreen screen = new WelcomeScreen();
        screen.setUserName("jk*fb");
        screen.setPassword("34");
        screen.processRequest(request, new MockHttpServletResponse());
        assertEquals(null, screen.getUser());
        assertEquals(WelcomeScreen.PLEASE_USE_PRINTING_CHARS + "<br>", screen.getWarning());

        screen.setUserName("jk.fb");
        screen.processRequest(request, new MockHttpServletResponse());
        assertEquals(null, screen.getUser());
        assertNull(screen.getRequestURL(request));
        assertEquals(WelcomeScreen.UNKNOWN_ACCOUNT_OR_PASSWORD_WARNING + "<br>", screen.getWarning());
    }
}
