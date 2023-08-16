package net.commerce.zocalo.JspSupport;

// Copyright 2008-2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.service.MarketOwner;
import net.commerce.zocalo.service.Config;
import net.commerce.zocalo.currency.CashBank;

import javax.servlet.http.HttpServletRequestWrapper;

import org.hibernate.Session;
import org.hibernate.Transaction;

public class ListMarketsPageTest extends PersistentTestHelper {
    private final String dbFilePath = "data/ListMarketsTest";

    public void testMarketDisplay() throws Exception {
        Config.initPasswdGen();

        manualSetUpForCreate(dbFilePath);
        Session session1 = HibernateTestUtil.currentSession();
        Transaction transaction = session1.beginTransaction();
        CashBank rootBank = new CashBank("cash");
        session1.save(rootBank);
        transaction.commit();

        manualSetUpForUpdate(dbFilePath);
        String joeName = "joe";
        String passwd = "joefoo";
        HttpServletRequestWrapper wrapper = registerUserAndGetRequestWithCookieInSession(joeName, passwd, dbFilePath);

        manualSetUpForUpdate(dbFilePath);
        MarketDisplay markets = new MarketDisplay();
        markets.setUserName(joeName);
        markets.processRequest(wrapper, null);

        manualSetUpForUpdate(dbFilePath);
        Session session = HibernateTestUtil.currentSession();
        session.beginTransaction();
        MarketOwner.newMarket("foo", joeName);
        assertTrue(MarketOwner.marketsExist());
        HibernateTestUtil.resetSessionFactory();
    }
}
