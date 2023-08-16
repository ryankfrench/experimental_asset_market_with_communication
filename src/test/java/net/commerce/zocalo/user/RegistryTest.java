package net.commerce.zocalo.user;

import net.commerce.zocalo.service.MarketOwner;
import net.commerce.zocalo.service.Config;
import net.commerce.zocalo.service.ServerUtil;
import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Funds;
import net.commerce.zocalo.hibernate.HibernateTestUtil;

import javax.servlet.http.Cookie;

// Copyright 2007, 2008 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class RegistryTest extends PersistentTestHelper {

    protected void tearDown() throws Exception {
        super.tearDown();
        HibernateTestUtil.currentSession().flush();
        HibernateTestUtil.closeSession();
    }

    public void testRegistryCreation() throws Exception {
        manualSetUpForCreate("data/RegistryTest");
        Config.initPasswdGen();
        HibernateTestUtil.currentSession().beginTransaction();
        Registry registry = new Registry();
        assertEquals(0, registry.userCount());
        CashBank rootbank = new CashBank("cash");
        HibernateTestUtil.save(rootbank);
        Funds funds = rootbank.makeFunds(30);
        SecureUser joe = new SecureUser("joe", funds, "foo", "a@b.com");
        HibernateTestUtil.save(joe);
        Cookie cookie = registry.register(joe);

        assertEquals(1, registry.userCount());
        assertEquals(joe,  registry.lookupUser(cookie.getValue()));
    }

    public void testRootToken() throws Exception {
        manualSetUpForCreate("data/RegistryTest");
        Registry registry = new Registry();
        assertEquals(0, registry.userCount());
        PasswordUtil util = PasswordUtil.make(0);
        String token = util.nextToken(8);
        registry.addAdminToken(token);
        assertTrue(registry.isAdminToken(token));
        assertFalse(registry.isAdminToken("foo"));
    }

    public void testRegisterUnconfirmed() throws Exception {
        manualSetUpForCreate("data/RegistryTest");
        Config.initializeConfiguration(ServerUtil.readConfigFile());
        Config.initPasswdGen();
        HibernateTestUtil.currentSession().beginTransaction();
        String requestURL = "http://test.example.com/createAccounts";
        UnconfirmedUser user = MarketOwner.createUnconfirmedUserAndNotifyOwner("nick", "foobear", "foo@example.com", requestURL);
        assertNotNull(user);
    }

    public void testNoPlusInTokens() {
        Config.initPasswdGen();
        for (int i = 0; i < 100; i++) {
            String tok = Registry.newToken();
            assertTrue("found a token with '+': " + tok, tok.indexOf("+") < 0);
        }
    }

}
