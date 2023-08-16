package net.commerce.zocalo.claim;

import net.commerce.zocalo.PersistentTestHelper;
import net.commerce.zocalo.market.BinaryMarket;
import net.commerce.zocalo.currency.CurrencyToken;
import net.commerce.zocalo.currency.Funds;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.user.SecureUser;
import org.hibernate.Criteria;
import org.hibernate.Session;
import org.hibernate.criterion.Expression;

import java.util.List;
import java.io.File;

import junitx.framework.Assert;
// Copyright 2006, 2007 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

public class PersistentClaimTest extends PersistentTestHelper {
    protected void tearDown() throws Exception {
        super.tearDown();
        HibernateTestUtil.resetSessionFactory();
    }

    protected void setUp() throws Exception {
        super.setUp();
        HibernateTestUtil.resetSessionFactory();
    }

    public void testBasicPersistence() throws Exception {
        manualSetUpForCreate("data/PersistentClaimTest");

        CurrencyToken token = new CurrencyToken("ignore");
        storeObject(token);
        SecureUser owner = new SecureUser("owner", Funds.make(token), "secure", "someone@example.com");
        storeObject(owner);
        String name = "empty";
        BinaryClaim claim = BinaryClaim.makeClaim(name, owner, "ignore this description");
        assertEquals(name, claim.getName());

        storeObject(claim);
        assertNotNull(HibernateTestUtil.currentSession());
        manualTearDown();
    }

    public void testPersistentPosition() throws Exception {
        {
            manualSetUpForCreate("data/PersistentClaimTest");
            String name = "actualClaim";
            CurrencyToken token = new CurrencyToken("ignore");
            storeObject(token);
            SecureUser owner = new SecureUser("owner", Funds.make(token), "secure", "someone@example.com");
            storeObject(owner);
            BinaryClaim firstClaim = BinaryClaim.makeClaim(name, owner, "FirstClaim");
            storeObject(firstClaim);
            List positions = allPositions("yes");
            assertEquals(1, positions.size());

            BinaryClaim secondClaim = BinaryClaim.makeClaim(name, owner, "secondClaim");
            storeObject(secondClaim);
            Position imposter = secondClaim.getYesPosition();

            assertFalse(imposter.equals(firstClaim.getYesPosition()));
            storeObject(imposter);
            assertFalse(imposter.equals(firstClaim.getYesPosition()));

            List morePositions = allPositions("yes");
            Assert.assertNotEquals(morePositions.get(0), morePositions.get(1));
            assertEquals(2, morePositions.size());

            HibernateTestUtil.currentSession().flush();
            manualTearDown();
        }

        {
            manualSetUpForUpdate("data/PersistentClaimTest");
            BinaryClaim claimAgain = anyClaim();

            assertEquals("yes", claimAgain.getYesPosition().getName());
            assertEquals("no", claimAgain.getNoPosition().getName());
        }
        manualTearDown();
    }

    private BinaryClaim anyClaim() {
        Session session = HibernateTestUtil.currentSession();
        Criteria claimCriteria = session.createCriteria(BinaryClaim.class);
        return (BinaryClaim) claimCriteria.list().iterator().next();
    }

    List allPositions(String claimName) {
        Session session = HibernateTestUtil.currentSession();
        Criteria c = session.createCriteria(Position.class);
        c.add( Expression.like("name", claimName));
        return c.list();
    }

    static private BinaryClaim anyBinaryClaim() {
        Session session = HibernateTestUtil.currentSession();
        Criteria claimCriteria = session.createCriteria(BinaryClaim.class);
        return (BinaryClaim) claimCriteria.list().iterator().next();
    }

    public void testPersistentPositionIdentities() throws Exception {
        // PersistentClaimSave.* was a copy of PersistentClaimTest with the final transaction, which dropped tables, removed
        //  The main requirement is that it is a db that has a market with a claim.
        String logFileName = "data/PersistentClaimSave.log";
        assertTrue(new File(logFileName).exists());
        copyFile("data/PersistentClaimSave.script", "data/PersistentClaimRead.script");
        copyFile(logFileName,    "data/PersistentClaimRead.log");
        copyFile("data/PersistentClaimSave.data",   "data/PersistentClaimRead.data");
        copyFile("data/PersistentClaimSave.properties", "data/PersistentClaimRead.properties");

        manualSetUpForUpdate("data/PersistentClaimRead");

        BinaryClaim claimAgain = anyBinaryClaim();
        Position no = claimAgain.getNoPosition();
        Position yes = claimAgain.getYesPosition();

        assertEquals("yes", yes.getName());
        assertEquals("no", no.getName());

        assertEquals(yes, no.opposite());
        assertEquals(no, yes.opposite());

        assertTrue(no.isInvertedPosition());
        assertFalse(yes.isInvertedPosition());
        HibernateTestUtil.currentSession().flush();

        manualTearDown();
    }
}
