package net.commerce.zocalo.claim;

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.user.SecureUser;
import net.commerce.zocalo.currency.CurrencyToken;
import net.commerce.zocalo.currency.Funds;
import net.commerce.zocalo.currency.Price;
import net.commerce.zocalo.currency.Quantity;
import net.commerce.zocalo.hibernate.HibernateTestUtil;

import java.util.Set;
import java.util.HashSet;
import java.math.MathContext;
import java.math.RoundingMode;

import junitx.framework.Assert;
import org.hibernate.Session;
import org.hibernate.Criteria;

// Copyright 2006 Chris Hibbert.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class MultiClaimTest extends PersistentTestHelper {
    protected void tearDown() throws Exception {
        super.tearDown();
        HibernateTestUtil.resetSessionFactory();
    }

    protected void setUp() throws Exception {
        super.setUp();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testBasicClaim() throws Exception {
        String[] positionNames = new String[]{"red", "yellow", "green"};
        CurrencyToken token = new CurrencyToken("ignore");
        SecureUser owner = new SecureUser("owner", Funds.make(token), "secure", "someone@example.com");
        String name = "empty";
        Set names = makeNames(positionNames);
        MultiClaim claim = MultiClaim.makeClaim(name, owner, "ignore this description", positionNames );
        assertEquals(name, claim.getName());
        Position[] positions = claim.positions();
        assertEquals(3, positions.length);
        assertTrue(claim.positionsInclude(positions[1]));
        assertTrue(names.contains(positions[0].getName()));
        assertQEquals(70, claim.naturalPrice(positions[0], Price.dollarPrice("70")));
    }

    private Set makeNames(String[] names) {
        Set nameSet = new HashSet();
        for (int i = 0; i < names.length; i++) {
            nameSet.add(names[i]);
        }
        return nameSet;
    }

    public void testPersistentClaim() throws Exception {
        String name = "weather";
        String[] positionNames = new String[]{"rain", "snow", "sun"};
        Set names = makeNames(positionNames);
        {
            manualSetUpForCreate("data/MultiClaimTest");
            CurrencyToken token = new CurrencyToken("ignore");
            storeObject(token);
            SecureUser owner = new SecureUser("owner", Funds.make(token), "secure", "someone@example.com");
            storeObject(owner);
            MultiClaim firstClaim = MultiClaim.makeClaim(name, owner, "FirstClaim", positionNames);
            storeObject(firstClaim);
            assertEquals(3, firstClaim.positions().length);

            MultiClaim secondClaim = MultiClaim.makeClaim(name, owner, "secondClaim", positionNames);
            storeObject(secondClaim);


            Assert.assertNotEquals(firstClaim.positions()[0], secondClaim.positions()[0]);
            assertTrue(names.contains(firstClaim.positions()[0].getName()));
            assertTrue(names.contains(secondClaim.positions()[0].getName()));

            manualTearDown();
        }

        {
            manualSetUpForUpdate("data/MultiClaimTest");
            MultiClaim multiClaim = anyClaim();

            Position[] positions = multiClaim.positions();
            assertTrue(positions[0] + " should be one of {rain, sun, snow}", "rainsunsnow".indexOf(positions[0].getName()) >= 0);
            assertEquals(3, positions.length);
            assertTrue(names.contains(positions[2].getName()));
            assertEquals(name, multiClaim.getName());
        }
        manualTearDown();
    }

    private MultiClaim anyClaim() {
        Session session = HibernateTestUtil.currentSession();
        Criteria claimCriteria = session.createCriteria(MultiClaim.class);
        return (MultiClaim) claimCriteria.list().iterator().next();
    }
}
