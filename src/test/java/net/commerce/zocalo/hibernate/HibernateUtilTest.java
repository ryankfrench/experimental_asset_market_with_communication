package net.commerce.zocalo.hibernate;

import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.claim.BinaryClaim;
import net.commerce.zocalo.market.Market;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.JunitHelper;
// Copyright 2009 Chris Hibbert.  All rights reserved.
// Copyright 2006 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class HibernateUtilTest extends PersistentTestHelper {
        protected void setUp() throws Exception {
            manualSetUpForCreate("data/HibernateUtilTest");
        }

    public void testClaimOwners() throws Exception {
        CashBank bank = new CashBank("money");
        storeObject(bank);
        SecureUser owner = new SecureUser("joe", bank.makeFunds(30000), "secure", "someone@example.com");
        storeObject(owner);
        BinaryClaim weather = BinaryClaim.makeClaim("rain", owner, "will it rain tomorrow?");
        storeObject(weather);
        Market weatherMarket = BinaryMarket.make(owner, weather, bank.noFunds());
        storeObject(weatherMarket);

        assertEquals(0, HibernateUtil.couponOwners(weather).size());

        SecureUser mary = new SecureUser("mary", bank.makeFunds(30000), "secure", "mary@example.com");
        storeObject(mary);
        SecureUser sally = new SecureUser("sally", bank.makeFunds(30000), "secure", "sally@example.com");
        storeObject(sally);
        weatherMarket.limitOrder(weather.getYesPosition(), Price.dollarPrice(60), q(25), mary);
        HibernateUtil.currentSession().flush();
        weatherMarket.limitOrder(weather.getNoPosition(), Price.dollarPrice(60), q(25), sally);
        HibernateUtil.currentSession().flush();

        BinaryClaim sports = BinaryClaim.makeClaim("contest", owner, "will we win tomorrow?");
        storeObject(sports);
        Market sportsMarket = BinaryMarket.make(owner, sports, bank.noFunds());
        storeObject(sportsMarket);

        SecureUser barry = new SecureUser("barry", bank.makeFunds(30000), "secure", "barry@example.com");
        storeObject(barry);
        SecureUser sarah = new SecureUser("sarah", bank.makeFunds(30000), "secure", "sarah@example.com");
        storeObject(sarah);
        SecureUser jesse = new SecureUser("jesse", bank.makeFunds(30000), "jesse", "jesse@example.com");
        storeObject(jesse);
        sportsMarket.limitOrder(sports.getYesPosition(), Price.dollarPrice(60), q(40), barry);
        sportsMarket.limitOrder(sports.getNoPosition(), Price.dollarPrice(60), q(25), sarah);
        sportsMarket.limitOrder(sports.getNoPosition(), Price.dollarPrice(60), q(15), jesse);

        assertEquals(2, HibernateUtil.couponOwners(weather).size());
        assertEquals(3, HibernateUtil.couponOwners(sports).size());
    }

    public void testREMatches() {
        JunitHelper.assertREMatches("fo+", "foo");
        JunitHelper.assertRENoMatch("fo+", "37foo");
        JunitHelper.assertREMatches(".*fo+", "37foo");
        JunitHelper.assertRENoMatch(".*fo+", "37foobar");
    }
}
