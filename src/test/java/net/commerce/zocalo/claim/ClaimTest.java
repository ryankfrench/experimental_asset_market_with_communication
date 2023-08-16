package net.commerce.zocalo.claim;

// Copyright 2009 Chris Hibbert.  All rights reserved.
// Copyright 2005 CommerceNet Consortium, LLC.  All rights reserved.

// This software is published under the terms of the MIT license, a copy
// of which has been included with this distribution in the LICENSE file.

import net.commerce.zocalo.currency.CashBank;
import net.commerce.zocalo.hibernate.HibernateTestUtil;
import net.commerce.zocalo.JunitHelper;
import junitx.framework.ArrayAssert;
import net.commerce.zocalo.logging.Log4JHelper;

public class ClaimTest extends JunitHelper {
    private final CashBank rootBank = new CashBank("cash");

    protected void setUp() throws Exception {
        HibernateTestUtil.resetSessionFactory();
        Log4JHelper.getInstance();
    }

    public void testClaimCreation() {
        BinaryClaim claim = BinaryClaim.makeClaim("grass", rootBank.makeEndowedUser("Janet", q(1000)), "The Grass will be Green");
        assertEquals("grass", claim.getName());
        ArrayAssert.assertEquivalenceArrays( new Position[] { claim.getYesPosition(), claim.getNoPosition() }, claim.positions());
    }
}
